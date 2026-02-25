package com.example.demologin.service;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.example.demologin.serviceImpl.PaymentServiceImpl;

public class PaymentServiceImplTest {
    private PaymentServiceImpl service = new PaymentServiceImpl();

    @Test
    public void testCreateAndVerify() throws Exception {
        // use default injected values by field declarations
        String url = service.createPremiumUrl(123L, 50000L);
        assertTrue(url.contains("vnp_TmnCode="));
        assertTrue(url.contains("vnp_Amount="));
        assertTrue(url.contains("vnp_SecureHash="));

        // parse query parameters back
        String query = url.substring(url.indexOf('?') + 1);
        Map<String, String> params = new HashMap<>();
        for (String pair : query.split("&")) {
            int idx = pair.indexOf('=');
            String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
            String value = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
            params.put(key, value);
        }
        assertTrue(service.verifyIpn(params));
    }
}
