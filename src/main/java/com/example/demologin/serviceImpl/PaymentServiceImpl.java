package com.example.demologin.serviceImpl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.demologin.service.PaymentService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Service
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private static final String PAYOS_API_URL = "https://api-merchant.payos.vn/v2/payment-requests";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final com.example.demologin.service.TransactionService transactionService;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${payos.clientId}")
    private String clientId;

    @Value("${payos.apiKey}")
    private String apiKey;

    @Value("${payos.checksumKey}")
    private String checksumKey;

    @Value("${payos.returnUrl:https://be.ducanhvipro.dpdns.org/api/payment/success}")
    private String returnUrl;

    @Value("${payos.cancelUrl:https://be.ducanhvipro.dpdns.org/api/payment/cancel}")
    private String cancelUrl;

    public PaymentServiceImpl(com.example.demologin.service.TransactionService transactionService) {
        this.transactionService = transactionService;
        this.httpClient = new OkHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String createPremiumUrl(Long userId, long amount) {
        try {
            // PayOS requires a unique numeric order code
            long orderCode = Long.parseLong(userId + "" + System.currentTimeMillis() % 1_000_000_000);
            String txnRef = userId + "_" + orderCode;
            String description = "Premium upgrade";

            // Create checksum
            String checksumData = String.format("amount=%d&cancelUrl=%s&description=%s&orderCode=%d&returnUrl=%s",
                    (int) amount, cancelUrl, description, orderCode, returnUrl);
            String signature = generateHmacSHA256(checksumData, checksumKey);

            // Build request body
            String requestBody = objectMapper.writeValueAsString(Map.of(
                    "orderCode", orderCode,
                    "amount", (int) amount,
                    "description", description,
                    "cancelUrl", cancelUrl,
                    "returnUrl", returnUrl,
                    "signature", signature,
                    "items", new Object[]{
                            Map.of(
                                    "name", "Premium Upgrade - Mozenith",
                                    "quantity", 1,
                                    "price", (int) amount
                            )
                    }
            ));

            Request request = new Request.Builder()
                    .url(PAYOS_API_URL)
                    .addHeader("x-client-id", clientId)
                    .addHeader("x-api-key", apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(requestBody, JSON))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                
                if (!response.isSuccessful()) {
                    log.error("PayOS API error: {} - {}", response.code(), responseBody);
                    throw new RuntimeException("Failed to create payment URL: " + response.code());
                }

                JsonNode jsonResponse = objectMapper.readTree(responseBody);
                String checkoutUrl = jsonResponse.path("data").path("checkoutUrl").asText();

                // Record pending transaction
                transactionService.createPendingTransaction(userId, txnRef, amount);

                log.info("PayOS payment link created for user {}: {}", userId, checkoutUrl);
                return checkoutUrl;
            }
        } catch (Exception e) {
            log.error("Failed to create PayOS payment link", e);
            throw new RuntimeException("Failed to create payment URL", e);
        }
    }

    @Override
    public boolean verifyWebhook(Map<String, Object> webhookBody) {
        try {
            // Extract data and signature from webhook
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) webhookBody.get("data");
            String receivedSignature = (String) webhookBody.get("signature");

            if (data == null || receivedSignature == null) {
                log.warn("Webhook missing data or signature");
                return false;
            }

            // Create sorted data string for signature verification
            TreeMap<String, Object> sortedData = new TreeMap<>(data);
            StringBuilder dataStr = new StringBuilder();
            for (Map.Entry<String, Object> entry : sortedData.entrySet()) {
                if (dataStr.length() > 0) {
                    dataStr.append("&");
                }
                dataStr.append(entry.getKey()).append("=").append(entry.getValue());
            }

            String calculatedSignature = generateHmacSHA256(dataStr.toString(), checksumKey);
            boolean isValid = calculatedSignature.equals(receivedSignature);

            if (!isValid) {
                log.warn("PayOS webhook signature mismatch");
            }

            return isValid;
        } catch (Exception e) {
            log.error("PayOS webhook verification failed", e);
            return false;
        }
    }

    private String generateHmacSHA256(String data, String key) throws Exception {
        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
        javax.crypto.spec.SecretKeySpec secretKey = new javax.crypto.spec.SecretKeySpec(
                key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKey);
        byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        
        StringBuilder hexString = new StringBuilder();
        for (byte b : hmacBytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}