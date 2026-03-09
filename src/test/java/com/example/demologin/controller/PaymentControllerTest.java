package com.example.demologin.controller;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.springframework.http.ResponseEntity;

import com.example.demologin.annotation.PublicEndpoint;
import com.example.demologin.enums.PackageType;
import com.example.demologin.service.PaymentService;
import com.example.demologin.service.QuotaService;
import com.example.demologin.utils.AccountUtils;

class PaymentControllerTest {

    AccountUtils accountUtils;
    PaymentService paymentService;
    QuotaService quotaService;
    com.example.demologin.service.TransactionService transactionService;
    PaymentController controller;

    @BeforeEach
    void setUp() {
        accountUtils = mock(AccountUtils.class);
        paymentService = mock(PaymentService.class);
        quotaService = mock(QuotaService.class);
        transactionService = mock(com.example.demologin.service.TransactionService.class);
        controller = new PaymentController(paymentService, quotaService, accountUtils, transactionService);
    }

    @Test
    void webhook_endpoint_is_public() throws NoSuchMethodException {
        var method = PaymentController.class.getMethod("handleWebhook", Map.class);
        assertTrue(method.isAnnotationPresent(PublicEndpoint.class), "Webhook handler should be marked public");
    }

    @Test
    void success_endpoint_is_public() throws NoSuchMethodException {
        var method = PaymentController.class.getMethod("paymentSuccess", String.class);
        assertTrue(method.isAnnotationPresent(PublicEndpoint.class), "Success handler should be marked public");
    }

    @Test
    void cancel_endpoint_is_public() throws NoSuchMethodException {
        var method = PaymentController.class.getMethod("paymentCancel", String.class);
        assertTrue(method.isAnnotationPresent(PublicEndpoint.class), "Cancel handler should be marked public");
    }

    @Test
    void invalid_webhook_signature_returns_error() {
        Map<String, Object> body = new HashMap<>();
        body.put("data", Map.of("code", "00"));
        when(paymentService.verifyWebhook(body)).thenReturn(false);

        ResponseEntity<Map<String, Object>> resp = controller.handleWebhook(body);
        assertTrue(resp.getBody().get("error").equals(1));
        verify(transactionService).recordPayOSWebhook(body);
    }

    @Test
    void successful_webhook_upgrades_package() {
        Map<String, Object> data = new HashMap<>();
        data.put("code", "00");
        data.put("orderCode", "12345");
        data.put("amount", 50000);
        Map<String, Object> body = new HashMap<>();
        body.put("data", data);
        when(paymentService.verifyWebhook(body)).thenReturn(true);
        when(transactionService.getUserIdByOrderCode("12345")).thenReturn(42L);

        ResponseEntity<Map<String, Object>> resp = controller.handleWebhook(body);
        assertTrue(resp.getBody().get("error").equals(0));
        verify(quotaService).setPackage(eq(42L), eq(PackageType.PREMIUM));
        verify(transactionService).recordPayOSWebhook(body);
    }

    @Test
    void success_page_returns_deep_link_html() {
        ResponseEntity<String> resp = controller.paymentSuccess("123");
        String body = resp.getBody();
        assertTrue(body.contains("bestie://payment?status=success"));
        assertTrue(body.contains("Payment Successful") || body.contains("SUCCESS"));
    }

    @Test
    void cancel_page_returns_deep_link_html() {
        ResponseEntity<String> resp = controller.paymentCancel("123");
        String body = resp.getBody();
        assertTrue(body.contains("bestie://payment?status=failed"));
        verify(transactionService).markCancelledByOrderCode("123");
    }
}
