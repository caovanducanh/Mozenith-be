package com.example.demologin.serviceImpl;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.demologin.service.PaymentService;

@Service
public class PaymentServiceImpl implements PaymentService {

    // updated to newly registered sandbox merchant
    @Value("${vnpay.tmnCode:350YP26B}")
    private String tmnCode = "350YP26B";

    @Value("${vnpay.hashSecret:8NUD7XPA4UCTHGK90MM4LLYN62GZOXRI}")
    private String hashSecret = "8NUD7XPA4UCTHGK90MM4LLYN62GZOXRI";

    // sandbox payment URL remains the same
    @Value("${vnpay.url:https://sandbox.vnpayment.vn/paymentv2/vpcpay.html}")
    private String vnpUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";

    // returnUrl must match one of the URLs registered with your VNPAY
    // sandbox merchant account.  When testing from a remote device through a
    // tunnel you should point this at your public hostname rather than
    // localhost.  We use the tunnel address as the default below so you can
    // start the backend without passing a property.
    @Value("${vnpay.returnUrl:https://be.ducanhvipro.dpdns.org/api/payment/ipn}")
    private String returnUrl = "https://be.ducanhvipro.dpdns.org/api/payment/ipn";

    @Override
    public String createPremiumUrl(Long userId, long amount) {
        try {
            Map<String, String> params = new HashMap<>();
            params.put("vnp_Version", "2.1.0");
            params.put("vnp_Command", "pay");
            params.put("vnp_TmnCode", tmnCode);
            // amount should be in smallest currency unit (multiply by 100)
            params.put("vnp_Amount", String.valueOf(amount * 100));
            // currency code is REQUIRED by VNPAY
            params.put("vnp_CurrCode", "VND");
            String txnRef = userId + "_" + System.currentTimeMillis();
            params.put("vnp_TxnRef", txnRef);
            params.put("vnp_OrderInfo", "Premium upgrade");
            params.put("vnp_OrderType", "billpayment");
            params.put("vnp_Locale", "vn");
            // client IP is REQUIRED by VNPAY
            params.put("vnp_IpAddr", "127.0.0.1");
            params.put("vnp_ReturnUrl", returnUrl);
            // supply current date/time in format yyyyMMddHHmmss
            java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            params.put("vnp_CreateDate", now.format(fmt));
            // expire date is required — 15 minutes from now
            params.put("vnp_ExpireDate", now.plusMinutes(15).format(fmt));

            // build sorted params and a SINGLE query string used for both
            // the hash calculation AND the URL — this is critical so the
            // signature matches what VNPAY recalculates on their side.
            SortedMap<String, String> sorted = new TreeMap<>(params);
            StringBuilder hashData = new StringBuilder();
            StringBuilder query = new StringBuilder();
            for (Map.Entry<String, String> entry : sorted.entrySet()) {
                if (hashData.length() > 0) {
                    hashData.append("&");
                    query.append("&");
                }
                String encodedKey = URLEncoder.encode(entry.getKey(), StandardCharsets.US_ASCII);
                String encodedVal = URLEncoder.encode(entry.getValue(), StandardCharsets.US_ASCII);
                hashData.append(encodedKey).append("=").append(encodedVal);
                query.append(encodedKey).append("=").append(encodedVal);
            }
            String secureHash = hmacSHA512(hashSecret, hashData.toString());
            query.append("&vnp_SecureHash=").append(secureHash);

            // log url for troubleshooting (avoid in production!)
            System.out.println("VNPAY request: " + vnpUrl + "?" + query.toString());
            return vnpUrl + "?" + query.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create payment URL", e);
        }
    }

    @Override
    public boolean verifyIpn(Map<String, String> parameters) {
        String providedHash = parameters.get("vnp_SecureHash");
        Map<String, String> copy = new HashMap<>(parameters);
        copy.remove("vnp_SecureHash");
        // do NOT remove SecureHashType; it is part of the signed data
        SortedMap<String, String> sorted = new TreeMap<>(copy);
        // build raw string (unencoded) same as during creation
        StringBuilder raw = new StringBuilder();
        for (Map.Entry<String, String> entry : sorted.entrySet()) {
            if (raw.length() > 0) raw.append("&");
            raw.append(entry.getKey()).append("=").append(entry.getValue());
        }
        String calculated = hmacSHA512(hashSecret, raw.toString());
        return calculated.equals(providedHash);
    }

    private String hmacSHA512(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            mac.init(secretKey);
            byte[] digest = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate HMACSHA512", e);
        }
    }
}