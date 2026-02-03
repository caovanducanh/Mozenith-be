package com.example.demologin.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "calendar_credential")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalendarCredential {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId; // link to User.id (store id to avoid fetch complexity)

    @Column(nullable = false)
    private String provider; // e.g., "google"

    @Column(length = 2048)
    private String accessToken;

    @Column(length = 2048)
    private String refreshToken;

    private Instant expiresAt;

    @Column(length = 1024)
    private String scopes;

    private Instant createdAt;

    private Instant updatedAt;
}
