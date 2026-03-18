package com.example.demologin.dto.response;

import lombok.*;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackResponse {
    private Long id;
    private Long userId;
    private String username;
    private String fullName;
    private double rating;
    private String comment;
    private Instant createdAt;
}
