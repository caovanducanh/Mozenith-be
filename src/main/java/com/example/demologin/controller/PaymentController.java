package com.example.demologin.controller;

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
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
     * IPN callback from VNPAY sandbox. VNPAY will POST parameters to this
     * endpoint; we verify the checksum and if successful, upgrade the user's
     * package for one month.
     * 
     * Returns an HTML page that redirects the user's browser to the mobile app
     * using a deep link (bestie://payment?status=success or failed).
     */
    // VNPAY will POST the notification, but when you're clicking the
    // return link in the sandbox or exercising the API from Swagger the
    // gateway will redirect with a GET.  Either way the endpoint must be
    // reachable without authentication so we mark it @PublicEndpoint and
    // accept both methods.
    @PublicEndpoint
    @RequestMapping(path = "/ipn", method = { org.springframework.web.bind.annotation.RequestMethod.POST,
            org.springframework.web.bind.annotation.RequestMethod.GET },
            produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> handleIpn(@RequestParam Map<String, String> params) {
        log.info("Received IPN callback: {}", params);
        
        String status = "failed";
        String message = "Payment processing failed";
        
        // verify checksum
        if (!paymentService.verifyIpn(params)) {
            log.warn("Invalid secure hash for IPN");
            // still persist record so we have history of the callback
            transactionService.recordIpn(params);
            message = "Invalid payment signature";
        } else {
            String responseCode = params.get("vnp_ResponseCode");
            String txnStatus = params.get("vnp_TransactionStatus");
            String txnRef = params.get("vnp_TxnRef");
            // VNPAY indicates success by both response code 00 *and* transaction
            // status 00.  we ignore other statuses so we don't accidentally grant
            // premium for a pending or failed payment.
            if (txnRef != null && responseCode != null && responseCode.equals("00")
                    && txnStatus != null && txnStatus.equals("00")) {
                try {
                    Long userId = Long.parseLong(txnRef.split("_")[0]);
                    quotaService.setPackage(userId, PackageType.PREMIUM);
                    // update transaction history with success details
                    transactionService.recordIpn(params);
                    status = "success";
                    message = "Payment successful! Redirecting to app...";
                } catch (Exception e) {
                    log.error("Failed to parse txnRef or upgrade package", e);
                    transactionService.recordIpn(params);
                    message = "Error processing payment";
                }
            } else {
                // for non-success statuses we still save the callback so admin can
                // investigate (e.g. user cancelled or failed)
                transactionService.recordIpn(params);
                message = "Payment was not completed";
            }
        }
        
        // Build deep link URL for the mobile app
        String deepLink = DEEP_LINK_BASE + "?status=" + status;
        
        // Return HTML page that redirects to the app's deep link
        String html = buildRedirectHtml(deepLink, status, message);
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(html);
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
