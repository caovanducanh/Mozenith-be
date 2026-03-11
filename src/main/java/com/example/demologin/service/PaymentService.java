package com.example.demologin.service;

import java.util.Map;

public interface PaymentService {
    /**
     * Create a PayOS payment link for the given user and amount.
     * Amount is expressed in VND (e.g. 50000 for 50,000 VND).
     */
    String createPremiumUrl(Long userId, long amount);

    /**
     * Verify a PayOS webhook callback and return true if the signature is valid
     * and the payment was successful.
     */
    boolean verifyWebhook(Map<String, Object> webhookBody);

    /**
     * Call PayOS API to verify a payment by its orderCode.
     * Returns true if PayOS confirms the payment status is PAID.
     */
    boolean verifyPaymentWithPayOS(String orderCode);
}
