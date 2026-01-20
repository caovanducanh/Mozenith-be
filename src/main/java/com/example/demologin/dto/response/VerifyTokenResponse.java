package com.example.demologin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class VerifyTokenResponse {
    private String verifyToken; // JWT verify thành công, null nếu fail
}
