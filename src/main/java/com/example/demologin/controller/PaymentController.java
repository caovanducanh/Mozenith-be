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
     * Shows a success page and redirects to the mobile app via deep link.
     */
    @PublicEndpoint
    @GetMapping(path = "/success", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> paymentSuccess(@RequestParam(required = false) String orderCode) {
        log.info("Payment success redirect, orderCode: {}", orderCode);
        String deepLink = DEEP_LINK_BASE + "?status=success";
        String html = buildRedirectHtml(deepLink, "success", "Payment successful! Redirecting to app...");
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
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

    /**
     * Verify payment status endpoint - called by mobile app to check if payment was processed.
     * This serves as a fallback when webhook might be delayed or missed.
     * It checks the latest transaction and applies premium if payment was successful.
     */
    @GetMapping("/verify-status")
    @AuthenticatedEndpoint
    public ResponseEntity<Map<String, Object>> verifyPaymentStatus() {
        Long userId = accountUtils.getCurrentUser().getUserId();
        Map<String, Object> response = new java.util.HashMap<>();
        
        // Build response envelope matching other API responses
        Map<String, Object> envelope = new java.util.HashMap<>();
        envelope.put("statusCode", 200);
        envelope.put("message", "Payment status verified");
        
        try {
            // Get the user's latest transaction
            var latestTx = transactionService.getLatestTransactionByUserId(userId);
            
            if (latestTx == null) {
                response.put("status", "NO_TRANSACTION");
                response.put("message", "No payment transaction found");
                envelope.put("data", response);
                return ResponseEntity.ok(envelope);
            }
            
            String txStatus = latestTx.getStatus();
            response.put("transactionStatus", txStatus);
            response.put("orderCode", latestTx.getOrderCode());
            
            if ("SUCCESS".equals(txStatus)) {
                // Ensure premium is applied (idempotent operation)
                var currentQuota = quotaService.getQuota(userId);
                if (!"PREMIUM".equals(currentQuota.getPackageType().name())) {
                    // Payment was successful but premium wasn't applied - fix it now
                    quotaService.setPackage(userId, PackageType.PREMIUM);
                    log.info("Applied PREMIUM to user {} via verify-status fallback", userId);
                    response.put("premiumApplied", true);
                } else {
                    response.put("premiumApplied", false);
                    response.put("alreadyPremium", true);
                }
                response.put("status", "SUCCESS");
                response.put("message", "Payment verified successfully");
            } else if ("PENDING".equals(txStatus)) {
                response.put("status", "PENDING");
                response.put("message", "Payment is still being processed");
            } else {
                response.put("status", "FAILED");
                response.put("message", "Payment was not successful");
            }
            
            envelope.put("data", response);
            return ResponseEntity.ok(envelope);
        } catch (Exception e) {
            log.error("Error verifying payment status for user {}", userId, e);
            response.put("status", "ERROR");
            response.put("message", "Failed to verify payment status: " + e.getMessage());
            envelope.put("data", response);
            return ResponseEntity.ok(envelope);
        }
    }
}
