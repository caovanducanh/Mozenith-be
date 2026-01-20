package com.example.demologin.dto.request;

import lombok.Data;

@Data
public class VerifyTokenRequest {
    private String token; // token FE gửi lên (cf-turnstile-response)
}
