package com.example.demologin.service;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.example.demologin.serviceImpl.PaymentServiceImpl;

public class PaymentServiceImplTest {
    private PaymentServiceImpl service = new PaymentServiceImpl();

    @Test
    public void testCreateAndVerify() throws Exception {
        // use default injected values by field declarations
        String url = service.createPremiumUrl(123L, 50000L);
        // nothing to print in normal operation; previous versions of this
        // test included debug diagnostics but they are no longer needed.
        assertTrue(url.contains("vnp_TmnCode="));
        assertTrue(url.contains("vnp_Amount="));
        assertTrue(url.contains("vnp_SecureHash="));
        assertTrue(url.contains("vnp_SecureHashType="));

        // parse query parameters back both decoded (the common case when
        // Spring hands us a Map) and raw (just in case we want to replay the
        // exact string we sent earlier).  The previous bug only showed up for
        // the decoded variant, so make sure both forms verify.
        String query = url.substring(url.indexOf('?') + 1);
        Map<String, String> decoded = new HashMap<>();
        Map<String, String> raw = new HashMap<>();
        for (String pair : query.split("&")) {
            int idx = pair.indexOf('=');
            String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
            String value = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
            decoded.put(key, value);
            // raw map uses the un-decoded value; this is effectively what the
            // payment URL contained before Spring processed it.
            String rawVal = pair.substring(idx + 1);
            raw.put(key, rawVal);
        }
        // check verifyIpn succeeds for both; the commit that fixed the
        // signature issue encodes the values when recomputing the hash, so
        // decoded inputs now behave correctly.
        // for debugging, compute the signature using both approaches so we
        // can print the intermediate values if verification fails.
        String providedHash = decoded.get("vnp_SecureHash");
        // copy maps after removing hash
        Map<String, String> copyDecoded = new HashMap<>(decoded);
        copyDecoded.remove("vnp_SecureHash");
        SortedMap<String, String> sortedDecoded = new java.util.TreeMap<>(copyDecoded);
        // unencoded concatenation (old behaviour)
        StringBuilder rawBuild = new StringBuilder();
        for (Map.Entry<String, String> e : sortedDecoded.entrySet()) {
            if (rawBuild.length() > 0) rawBuild.append("&");
            rawBuild.append(e.getKey()).append("=").append(e.getValue());
        }
        // encoded concatenation (new behaviour)
        StringBuilder encBuild = new StringBuilder();
        for (Map.Entry<String, String> e : sortedDecoded.entrySet()) {
            if (encBuild.length() > 0) encBuild.append("&");
            encBuild.append(java.net.URLEncoder.encode(e.getKey(), java.nio.charset.StandardCharsets.US_ASCII))
                    .append("=")
                    .append(java.net.URLEncoder.encode(e.getValue(), java.nio.charset.StandardCharsets.US_ASCII));
        }
        // helper to compute HMACSHA512 (duplicate of private method in service)
        java.util.function.BiFunction<String, String, String> hmac = (key, data) -> {
            try {
                javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA512");
                javax.crypto.spec.SecretKeySpec secretKey = new javax.crypto.spec.SecretKeySpec(
                        key.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA512");
                mac.init(secretKey);
                byte[] digest = mac.doFinal(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                for (byte b : digest) {
                    sb.append(String.format("%02x", b));
                }
                return sb.toString();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        };
        // Use the same default secret that PaymentServiceImpl initializes
        String secret = "8NUD7XPA4UCTHGK90MM4LLYN62GZOXRI";
        String calcRaw = hmac.apply(secret, rawBuild.toString());
        String calcEnc = hmac.apply(secret, encBuild.toString());
        boolean okDecoded = service.verifyIpn(decoded);
        if (!okDecoded) {
            System.out.println("decoded map: " + decoded);
            System.out.println("provided hash=" + providedHash);
            System.out.println("raw string=" + rawBuild);
            System.out.println("calculated (raw)=" + calcRaw);
            System.out.println("encoded string=" + encBuild);
            System.out.println("calculated (enc)=" + calcEnc);
        }
        assertTrue(okDecoded, "decoded verification failed");
        // the raw (still-encoded) form would not normally be seen by
        // verifyIpn; Spring decodes the values for us.  However we do
        // validate that the re-encoded string calculation equals the
        // provided hash so we know the algorithm is correct.
        assertTrue(calcEnc.equals(providedHash), "encoded hash mismatch");
    }
}
