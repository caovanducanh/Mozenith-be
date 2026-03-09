package com.example.demologin.serviceImpl;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.demologin.service.PaymentService;

import lombok.extern.slf4j.Slf4j;
import vn.payos.PayOS;
import vn.payos.type.CheckoutResponseData;
import vn.payos.type.ItemData;
import vn.payos.type.PaymentData;
import vn.payos.type.Webhook;
import vn.payos.type.WebhookData;

@Service
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final com.example.demologin.service.TransactionService transactionService;
    private final PayOS payOS;

    @Value("${payos.returnUrl:https://be.ducanhvipro.dpdns.org/api/payment/success}")
    private String returnUrl;

    @Value("${payos.cancelUrl:https://be.ducanhvipro.dpdns.org/api/payment/cancel}")
    private String cancelUrl;

    public PaymentServiceImpl(
            com.example.demologin.service.TransactionService transactionService,
            @Value("${payos.clientId}") String clientId,
            @Value("${payos.apiKey}") String apiKey,
            @Value("${payos.checksumKey}") String checksumKey) {
        this.transactionService = transactionService;
        this.payOS = new PayOS(clientId, apiKey, checksumKey);
    }

    @Override
    public String createPremiumUrl(Long userId, long amount) {
        try {
            // PayOS requires a unique numeric order code
            long orderCode = Long.parseLong(userId + "" + System.currentTimeMillis() % 1_000_000_000);
            String txnRef = userId + "_" + orderCode;

            ItemData item = ItemData.builder()
                    .name("Premium Upgrade - Mozenith")
                    .quantity(1)
                    .price((int) amount)
                    .build();

            PaymentData paymentData = PaymentData.builder()
                    .orderCode(orderCode)
                    .amount((int) amount)
                    .description("Premium upgrade")
                    .returnUrl(returnUrl)
                    .cancelUrl(cancelUrl)
                    .item(item)
                    .build();

            CheckoutResponseData response = payOS.createPaymentLink(paymentData);

            // record pending transaction (amount stored in VND, no multiplication)
            transactionService.createPendingTransaction(userId, txnRef, amount);

            log.info("PayOS payment link created for user {}: {}", userId, response.getCheckoutUrl());
            return response.getCheckoutUrl();
        } catch (Exception e) {
            log.error("Failed to create PayOS payment link", e);
            throw new RuntimeException("Failed to create payment URL", e);
        }
    }

    @Override
    public boolean verifyWebhook(Map<String, Object> webhookBody) {
        try {
            Webhook webhook = new Webhook(webhookBody);
            WebhookData data = payOS.verifyPaymentWebhookData(webhook);
            // If verification succeeds and we get data, it's valid
            return data != null;
        } catch (Exception e) {
            log.error("PayOS webhook verification failed", e);
            return false;
        }
    }
}