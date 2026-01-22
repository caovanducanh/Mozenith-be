package com.example.demologin.serviceImpl;

import com.example.demologin.dto.request.VerifyTokenRequest;
import com.example.demologin.dto.response.VerifyTokenResponse;
import com.example.demologin.service.HumanVerificationService;
import com.example.demologin.utils.HumanVerifyTokenUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Map;

// TEMPORARILY DISABLED - Human verification feature
/*
@Service
public class HumanVerificationServiceImpl implements HumanVerificationService {

    @Value("${cloudflare.turnstile.secret-key}")
    private String turnstileSecretKey;

    @Value("${verify.token.secret}")
    private String verifyTokenSecret;

    @Value("${verify.token.expiry-ms:86400000}")
    private int verifyTokenExpiryMs;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    @SuppressWarnings("unchecked")
    public VerifyTokenResponse verifyHuman(VerifyTokenRequest request) {
        String url = "https://challenges.cloudflare.com/turnstile/v0/siteverify";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("secret", turnstileSecretKey);
        body.add("response", request.getToken());

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

        Map<String, Object> response = restTemplate.postForObject(url, entity, Map.class);

        if (response != null && Boolean.TRUE.equals(response.get("success"))) {
            HumanVerifyTokenUtil util = new HumanVerifyTokenUtil(verifyTokenSecret, verifyTokenExpiryMs);
            String jwt = util.generateToken();
            return new VerifyTokenResponse(jwt);
        } else {
            return new VerifyTokenResponse(null);
        }
    }
}
*/
