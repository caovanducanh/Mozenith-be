package com.example.demologin.service;

import java.util.Map;

public interface PaymentService {
    /**
     * Create a VNPAY payment URL for the given user and amount.
     * Amount is expressed in the major unit (e.g. 50000 for 50,000 VND).
     */
    String createPremiumUrl(Long userId, long amount);

    /**
     * Verify that an IPN callback from VNPAY has a correct checksum.
     */
    boolean verifyIpn(Map<String, String> parameters);
}
