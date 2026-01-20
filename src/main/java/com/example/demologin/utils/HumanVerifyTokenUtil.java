package com.example.demologin.utils;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.util.Date;

public class HumanVerifyTokenUtil {
    private final String secret;
    private final int expiryMs;

    public HumanVerifyTokenUtil(String secret, int expiryMs) {
        this.secret = secret;
        this.expiryMs = expiryMs;
    }

    public String generateToken() {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setSubject("human-verified")
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + expiryMs))
                .signWith(SignatureAlgorithm.HS256, secret)
                .compact();
    }
}
