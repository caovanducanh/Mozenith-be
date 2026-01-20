package com.example.demologin.entity;

import com.example.demologin.enums.ActivityType;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_activity_logs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActivityType activityType;

    @Column(nullable = true)
    private Long userId;

    @Column(length = 100)
    private String fullName;

    @Column(nullable = false)
    private LocalDateTime timestamp;


    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "SUCCESS";

    @Column(length = 500)
    private String details;

    @Column(length = 45)
    private String ipAddress;

    @Column(length = 1000)
    private String userAgent;

    // Device and browser information
    @Column(length = 100)
    private String browser;

    @Column(length = 50)
    private String browserVersion;

    @Column(length = 100)
    private String operatingSystem;

    @Column(length = 100)
    private String device;

    @Column(length = 20)
    private String deviceType;

    // Location information
    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String region;

    @Column(length = 100)
    private String country;

    @Column(length = 10)
    private String countryCode;

    // Add pre-persist method to ensure required fields are set
    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
        if (status == null) {
            status = "SUCCESS";
        }
    }
}
