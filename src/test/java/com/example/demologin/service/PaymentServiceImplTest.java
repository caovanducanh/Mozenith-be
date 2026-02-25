package com.example.demologin.service;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;

import com.example.demologin.serviceImpl.PaymentServiceImpl;

public class PaymentServiceImplTest {
    private PaymentServiceImpl service;
    private com.example.demologin.service.TransactionService transactionService;

    @BeforeEach
    void setUp() {
        transactionService = mock(com.example.demologin.service.TransactionService.class);
        service = new PaymentServiceImpl(transactionService);
    }

    @Test
    public void testCreateAndVerify() throws Exception {
        // use default injected values by field declarations
        String url = service.createPremiumUrl(123L, 50000L);
        // transactionService should have been asked to create a pending record
        verify(transactionService).createPendingTransaction(eq(123L), anyString(), eq(50000L * 100));
        // nothing to print in normal operation; previous versions of this
        // test included debug diagnostics but they are no longer needed.
        assertTrue(url.contains("vnp_TmnCode="));
        assertTrue(url.contains("vnp_Amount="));
        assertTrue(url.contains("vnp_SecureHash="));
        assertTrue(url.contains("vnp_SecureHashType="));

        // parse query parameters back
        String query = url.substring(url.indexOf('?') + 1);
        Map<String, String> params = new HashMap<>();
        for (String pair : query.split("&")) {
            int idx = pair.indexOf('=');
            String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
            String value = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
            params.put(key, value);
        }
        // check verifyIpn still succeeds (and implicitly that the
        // signature included the SecureHashType parameter).
        assertTrue(service.verifyIpn(params));
        assertTrue(service.verifyIpn(params));
    }

    /**
     * Regression test reproducing the failure seen in production where the
     * gateway returns a URL that was later reported as "INVALID_SIGNATURE" by
     * our endpoint.  The sample parameters are taken from an actual sandbox
     * callback (spaces decoded by Spring, plus signs preserved in the URL).
     */
    @Test
    public void verifyIpn_exampleFromScreenshot() {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_Amount", "5000000");
        params.put("vnp_BankCode", "NCB");
        params.put("vnp_BankTranNo", "VNP15431310");
        params.put("vnp_CardType", "ATM");
        // Spring would already have decoded "+" to space in the request map
        params.put("vnp_OrderInfo", "Premium upgrade");
        params.put("vnp_PayDate", "20260225182238");
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_TmnCode", "350YP26B");
        params.put("vnp_TransactionNo", "15431310");
        params.put("vnp_TransactionStatus", "00");
        params.put("vnp_TxnRef", "2_1772018520220");
        params.put("vnp_SecureHash",
                "1350ad695d302436c4f97dd67a3f5ef61d8e2297312ce120762d45f4646cacb3056c6db55ca3acdb9fd484bb6cd85e4a99342437b1d8ebd8f971ff1edb903504");
        // gateway would also include this parameter but it's ignored during
        // verification
        params.put("vnp_SecureHashType", "SHA512");
        assertTrue(service.verifyIpn(params), "sample IPN from screenshot should be accepted");
    }
}
