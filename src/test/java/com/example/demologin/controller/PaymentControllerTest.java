package com.example.demologin.controller;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demologin.annotation.PublicEndpoint;
import com.example.demologin.enums.PackageType;
import com.example.demologin.service.PaymentService;
import com.example.demologin.service.QuotaService;
import com.example.demologin.utils.AccountUtils;

class PaymentControllerTest {

    AccountUtils accountUtils;
    PaymentService paymentService;
    QuotaService quotaService;
    PaymentController controller;

    @BeforeEach
    void setUp() {
        accountUtils = mock(AccountUtils.class);
        paymentService = mock(PaymentService.class);
        quotaService = mock(QuotaService.class);
        controller = new PaymentController(paymentService, quotaService, accountUtils);
    }

    @Test
    void ipn_endpoint_is_public_and_accepts_both_methods() throws NoSuchMethodException {
        // verify the annotation is present and mapping allows POST/GET
        var method = PaymentController.class.getMethod("handleIpn", Map.class);
        assertTrue(method.isAnnotationPresent(PublicEndpoint.class), "IPN handler should be marked public");
        // ensure the request mapping covers both POST and GET
        var mapping = method.getAnnotation(org.springframework.web.bind.annotation.RequestMapping.class);
        assertTrue(mapping != null, "RequestMapping should be present");
        var methods = mapping.method();
        assertTrue(methods.length >= 2, "Should support more than one HTTP method");
        // simple sanity check that POST and GET are included
        boolean hasPost = false, hasGet = false;
        for (var m : methods) {
            if (m == org.springframework.web.bind.annotation.RequestMethod.POST) hasPost = true;
            if (m == org.springframework.web.bind.annotation.RequestMethod.GET) hasGet = true;
        }
        assertTrue(hasPost && hasGet, "mapping should allow POST and GET");
    }

    @Test
    void invalid_signature_returns_invalid() {
        Map<String, String> params = Map.of("foo", "bar");
        when(paymentService.verifyIpn(params)).thenReturn(false);

        String resp = controller.handleIpn(params);
        assertEquals("INVALID_SIGNATURE", resp);
        // quota service should never be touched
        verify(quotaService, org.mockito.Mockito.never()).setPackage(eq(0L), eq(PackageType.PREMIUM));
    }

    @Test
    void successful_ipn_upgrades_package() {
        Map<String, String> params = Map.of(
                "vnp_ResponseCode", "00",
                "vnp_TxnRef", "42_abcdef"
        );
        when(paymentService.verifyIpn(params)).thenReturn(true);

        String resp = controller.handleIpn(params);
        assertEquals("OK", resp);
        // userId 42 should be passed to quotaService
        verify(quotaService).setPackage(eq(42L), eq(PackageType.PREMIUM));
    }
}
