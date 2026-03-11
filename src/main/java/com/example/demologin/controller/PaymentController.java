package com.example.demologin.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demologin.annotation.ApiResponse;
import com.example.demologin.annotation.AuthenticatedEndpoint;
import com.example.demologin.annotation.PublicEndpoint;
import com.example.demologin.enums.PackageType;
import com.example.demologin.service.PaymentService;
import com.example.demologin.service.QuotaService;
import com.example.demologin.service.TransactionService;
import com.example.demologin.utils.AccountUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payment")
@Slf4j
public class PaymentController {
    private final PaymentService paymentService;
    private final QuotaService quotaService;
    private final AccountUtils accountUtils;
    private final TransactionService transactionService;

    // Deep link scheme for redirecting back to the mobile app
    private static final String DEEP_LINK_BASE = "bestie://payment";

    @PostMapping("/premium-url")
    @AuthenticatedEndpoint
    @ApiResponse(message = "Payment url generated")
    public Object createPremiumUrl() {
        Long userId = accountUtils.getCurrentUser().getUserId();
        // fixed cost 50k VND
        String url = paymentService.createPremiumUrl(userId, 50000L);
        return Map.of("url", url);
    }

    /**
     * Check the status of the current user's latest payment transaction.
     * Used by mobile app to poll for payment completion after returning from PayOS.
     */
    @GetMapping("/status")
    @AuthenticatedEndpoint
    @ApiResponse(message = "Payment status retrieved")
    public Object getPaymentStatus() {
        Long userId = accountUtils.getCurrentUser().getUserId();
        return transactionService.getLatestTransactionForUser(userId);
    }

    /**
     * Webhook callback from PayOS. PayOS sends a POST with a JSON body
     * containing payment result and signature. We verify and process it.
     */
    @PublicEndpoint
    @PostMapping("/webhook")
    public ResponseEntity<Map<String, Object>> handleWebhook(@RequestBody Map<String, Object> webhookBody) {
        log.info("Received PayOS webhook: {}", webhookBody);

        Map<String, Object> response = new HashMap<>();

        if (!paymentService.verifyWebhook(webhookBody)) {
            log.warn("Invalid PayOS webhook signature");
            transactionService.recordPayOSWebhook(webhookBody);
            response.put("error", 1);
            response.put("message", "Invalid signature");
            return ResponseEntity.ok(response);
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) webhookBody.get("data");
        if (data == null) {
            response.put("error", 1);
            response.put("message", "Missing data");
            return ResponseEntity.ok(response);
        }

        String code = String.valueOf(data.getOrDefault("code", ""));
        String orderCode = String.valueOf(data.getOrDefault("orderCode", ""));

        // PayOS success code is "00"
        if ("00".equals(code) && !orderCode.isEmpty()) {
            try {
                transactionService.recordPayOSWebhook(webhookBody);
                // Extract userId from the txnRef stored when creating payment
                Long userId = transactionService.getUserIdByOrderCode(orderCode);
                if (userId != null) {
                    quotaService.setPackage(userId, PackageType.PREMIUM);
                    log.info("User {} upgraded to PREMIUM via PayOS", userId);
                }
                response.put("error", 0);
                response.put("message", "OK");
            } catch (Exception e) {
                log.error("Failed to process PayOS webhook", e);
                response.put("error", 1);
                response.put("message", "Processing error");
            }
        } else {
            transactionService.recordPayOSWebhook(webhookBody);
            response.put("error", 0);
            response.put("message", "OK");
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Success redirect page — after PayOS payment, user's browser is redirected here.
     * Verifies the payment with PayOS and upgrades the user before redirecting to the app.
     */
    @PublicEndpoint
    @GetMapping(path = "/success", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> paymentSuccess(
            @RequestParam(required = false) String orderCode,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String status) {
        log.info("Payment success redirect, orderCode: {}, code: {}, status: {}", orderCode, code, status);

        boolean upgraded = false;

        if (orderCode != null && !orderCode.isEmpty()) {
            try {
                boolean redirectIndicatesSuccess = "00".equals(code) && "PAID".equalsIgnoreCase(status);

                // Try to verify with PayOS API
                boolean apiConfirmed = paymentService.verifyPaymentWithPayOS(orderCode);

                // If API didn't confirm but PayOS redirect says PAID, retry after delay
                // (PayOS may take a moment to finalize the payment in their API)
                if (!apiConfirmed && redirectIndicatesSuccess) {
                    log.info("PayOS API didn't confirm yet for orderCode={}, retrying after 3s delay...", orderCode);
                    try { Thread.sleep(3000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                    apiConfirmed = paymentService.verifyPaymentWithPayOS(orderCode);
                }

                if (apiConfirmed || redirectIndicatesSuccess) {
                    Long userId = transactionService.getUserIdByOrderCode(orderCode);
                    if (userId != null) {
                        quotaService.setPackage(userId, PackageType.PREMIUM);
                        transactionService.markSuccessByOrderCode(orderCode);
                        upgraded = true;
                        log.info("User {} upgraded to PREMIUM via success redirect (orderCode={}, apiConfirmed={}, redirectIndicatesSuccess={})",
                                userId, orderCode, apiConfirmed, redirectIndicatesSuccess);
                    } else {
                        log.warn("Could not find userId for orderCode: {}", orderCode);
                    }
                } else {
                    log.warn("Payment not confirmed for orderCode: {} (apiConfirmed=false, redirectIndicatesSuccess=false)", orderCode);
                }
            } catch (Exception e) {
                log.error("Error processing payment success for orderCode: {}", orderCode, e);
            }
        }

        String deepLink = DEEP_LINK_BASE + "?status=" + (upgraded ? "success" : "failed");
        String msg = upgraded ? "Payment successful! Redirecting to app..." : "Payment processing issue. Please check the app.";
        String htmlStatus = upgraded ? "success" : "failed";
        String html = buildRedirectHtml(deepLink, htmlStatus, msg);
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
    }

    /**
     * Endpoint for the mobile app to verify a payment and trigger the upgrade.
     * Calls PayOS API to confirm payment, then upgrades the user if confirmed.
     */
    @PostMapping("/verify")
    @AuthenticatedEndpoint
    @ApiResponse(message = "Payment verified")
    public Object verifyAndUpgrade() {
        Long userId = accountUtils.getCurrentUser().getUserId();
        var latestTx = transactionService.getLatestTransactionForUser(userId);

        if ("SUCCESS".equals(latestTx.getStatus())) {
            // Already processed — user should already be premium
            return Map.of("verified", true, "status", "SUCCESS");
        }

        String oc = latestTx.getOrderCode();
        if (oc == null || oc.isEmpty()) {
            return Map.of("verified", false, "status", "NO_ORDER");
        }

        boolean paid = paymentService.verifyPaymentWithPayOS(oc);
        if (paid) {
            quotaService.setPackage(userId, PackageType.PREMIUM);
            transactionService.markSuccessByOrderCode(oc);
            log.info("User {} upgraded to PREMIUM via verify endpoint (orderCode={})", userId, oc);
            return Map.of("verified", true, "status", "SUCCESS");
        }

        return Map.of("verified", false, "status", latestTx.getStatus());
    }

    /**
     * Cancel redirect page — user cancelled the payment on PayOS.
     */
    @PublicEndpoint
    @GetMapping(path = "/cancel", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> paymentCancel(@RequestParam(required = false) String orderCode) {
        log.info("Payment cancelled, orderCode: {}", orderCode);
        if (orderCode != null) {
            transactionService.markCancelledByOrderCode(orderCode);
        }
        String deepLink = DEEP_LINK_BASE + "?status=failed";
        String html = buildRedirectHtml(deepLink, "failed", "Payment was cancelled.");
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
    }
    
    /**
     * Build an HTML page that redirects to the mobile app via deep link.
     * Uses both JavaScript redirect and meta refresh as fallbacks.
     */
    private String buildRedirectHtml(String deepLink, String status, String message) {
        String statusColor = "success".equals(status) ? "#22c55e" : "#ef4444";
        String statusEmoji = "success".equals(status) ? "✅" : "❌";
        
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <meta http-equiv="refresh" content="2;url=%s">
                <title>Payment %s</title>
                <style>
                    * { margin: 0; padding: 0; box-sizing: border-box; }
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                        background: linear-gradient(135deg, #1a1a2e 0%%, #16213e 100%%);
                        min-height: 100vh;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        padding: 20px;
                    }
                    .container {
                        background: white;
                        border-radius: 20px;
                        padding: 40px;
                        text-align: center;
                        max-width: 400px;
                        width: 100%%;
                        box-shadow: 0 20px 60px rgba(0,0,0,0.3);
                    }
                    .emoji { font-size: 64px; margin-bottom: 20px; }
                    .title {
                        font-size: 24px;
                        font-weight: bold;
                        color: #1a1a2e;
                        margin-bottom: 10px;
                    }
                    .message {
                        color: #666;
                        margin-bottom: 30px;
                        line-height: 1.5;
                    }
                    .status {
                        display: inline-block;
                        padding: 8px 20px;
                        background: %s;
                        color: white;
                        border-radius: 20px;
                        font-weight: bold;
                        margin-bottom: 20px;
                    }
                    .btn {
                        display: inline-block;
                        padding: 15px 40px;
                        background: #7c3aed;
                        color: white;
                        text-decoration: none;
                        border-radius: 12px;
                        font-weight: bold;
                        font-size: 16px;
                        transition: transform 0.2s, box-shadow 0.2s;
                    }
                    .btn:hover {
                        transform: translateY(-2px);
                        box-shadow: 0 10px 30px rgba(124,58,237,0.4);
                    }
                    .loader {
                        width: 40px;
                        height: 40px;
                        border: 4px solid #e5e7eb;
                        border-top-color: #7c3aed;
                        border-radius: 50%%;
                        animation: spin 1s linear infinite;
                        margin: 20px auto;
                    }
                    @keyframes spin { to { transform: rotate(360deg); } }
                    .hint {
                        margin-top: 20px;
                        font-size: 14px;
                        color: #999;
                    }
                </style>
                <script>
                    // Redirect to app after a short delay
                    setTimeout(function() {
                        window.location.href = '%s';
                    }, 1500);
                </script>
            </head>
            <body>
                <div class="container">
                    <div class="emoji">%s</div>
                    <div class="status">%s</div>
                    <h1 class="title">%s</h1>
                    <div class="message">%s</div>
                    <div class="loader"></div>
                    <a href="%s" class="btn">Open Mozenith App</a>
                    <p class="hint">If you're not redirected automatically, tap the button above.</p>
                </div>
            </body>
            </html>
            """.formatted(
                deepLink,
                status.substring(0, 1).toUpperCase() + status.substring(1),
                statusColor,
                deepLink,
                statusEmoji,
                status.toUpperCase(),
                "success".equals(status) ? "Payment Successful!" : "Payment Failed",
                message,
                deepLink
            );
    }
}
