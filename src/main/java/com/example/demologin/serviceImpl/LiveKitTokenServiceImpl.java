package com.example.demologin.serviceImpl;

import com.example.demologin.config.LiveKitConfig;
import com.example.demologin.dto.response.LiveKitTokenResponse;
import com.example.demologin.service.LiveKitTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class LiveKitTokenServiceImpl implements LiveKitTokenService {

    private final LiveKitConfig liveKitConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public LiveKitTokenResponse generateToken(Long userId, String fullName, String email, String authToken) {
        String roomName = "room-" + userId + "-" + UUID.randomUUID().toString().substring(0, 8);
        String identity = String.valueOf(userId);

        // Build participant metadata JSON
        Map<String, String> metadata = new HashMap<>();
        metadata.put("userId", String.valueOf(userId));
        metadata.put("fullName", fullName);
        metadata.put("email", email);
        metadata.put("authToken", authToken);

        String metadataJson;
        try {
            metadataJson = objectMapper.writeValueAsString(metadata);
        } catch (Exception e) {
            log.error("Failed to serialize metadata", e);
            throw new RuntimeException("Failed to build token metadata");
        }

        // Build LiveKit video grant
        Map<String, Object> videoGrant = new HashMap<>();
        videoGrant.put("room", roomName);
        videoGrant.put("roomJoin", true);
        videoGrant.put("canPublish", true);
        videoGrant.put("canSubscribe", true);
        videoGrant.put("canPublishData", true);

        // Build the JWT (LiveKit access token format)
        SecretKey key = Keys.hmacShaKeyFor(liveKitConfig.getApiSecret().getBytes(StandardCharsets.UTF_8));
        Date now = new Date();
        Date expiry = new Date(now.getTime() + 3600_000); // 1 hour

        String token = Jwts.builder()
                .issuer(liveKitConfig.getApiKey())
                .subject(identity)
                .issuedAt(now)
                .notBefore(now)
                .expiration(expiry)
                .claim("name", fullName)
                .claim("metadata", metadataJson)
                .claim("video", videoGrant)
                .signWith(key)
                .compact();

        log.info("Generated LiveKit token for user {} in room {}", userId, roomName);
        return new LiveKitTokenResponse(liveKitConfig.getUrl(), token);
    }
}
