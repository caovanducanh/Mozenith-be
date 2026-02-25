package com.example.demologin.controller;

import java.util.Map;

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
     */
    // VNPAY will POST the notification, but when you're clicking the
    // return link in the sandbox or exercising the API from Swagger the
    // gateway will redirect with a GET.  Either way the endpoint must be
    // reachable without authentication so we mark it @PublicEndpoint and
    // accept both methods.
    @PublicEndpoint
    @RequestMapping(path = "/ipn", method = { org.springframework.web.bind.annotation.RequestMethod.POST,
            org.springframework.web.bind.annotation.RequestMethod.GET })
    public String handleIpn(@RequestParam Map<String, String> params) {
        log.info("Received IPN callback: {}", params);
        // verify checksum
        if (!paymentService.verifyIpn(params)) {
            log.warn("Invalid secure hash for IPN");
            // still persist record so we have history of the callback
            transactionService.recordIpn(params);
            return "INVALID_SIGNATURE";
        }
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
                // successful
                return "OK";
            } catch (Exception e) {
                log.error("Failed to parse txnRef or upgrade package", e);
                transactionService.recordIpn(params);
                return "ERROR";
            }
        }
        // for non-success statuses we still save the callback so admin can
        // investigate (e.g. user cancelled or failed)
        transactionService.recordIpn(params);
        return "IGNORED";
    }
}
