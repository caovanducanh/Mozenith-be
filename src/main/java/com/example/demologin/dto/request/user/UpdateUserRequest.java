package com.example.demologin.dto.request.user;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserRequest {
    private Long userId;
    private String fullName;
    private String email;
    private String reason; // Lý do cập nhật
} 
