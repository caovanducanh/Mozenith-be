package com.example.demologin.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Entity to store user diary/journal entries.
 * Each user has their own isolated diary that syncs across devices.
 */
@Entity
@Table(name = "diary_entry", indexes = {
    @Index(name = "idx_diary_user_id", columnList = "userId"),
    @Index(name = "idx_diary_user_timestamp", columnList = "userId, timestamp DESC")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiaryEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Client-side UUID for sync purposes
     */
    @Column(nullable = false, length = 36)
    private String clientId;

    @Column(nullable = false)
    private Long userId;

    /**
     * The diary entry content
     */
    @Column(nullable = false, length = 4096)
    private String content;

    /**
     * Mood enum value: VERY_HAPPY, HAPPY, NEUTRAL, SAD, VERY_SAD, ANGRY, ANXIOUS, EXCITED, TIRED, GRATEFUL, LOVED, CONFUSED
     */
    @Column(nullable = false, length = 20)
    private String mood;

    /**
     * Original timestamp from the client (when the entry was created)
     */
    @Column(nullable = false)
    private Instant timestamp;

    /**
     * Whether the entry was created via AI voice command
     */
    @Column(nullable = false)
    private boolean createdByVoice;

    /**
     * Comma-separated tags
     */
    @Column(length = 500)
    private String tags;

    /**
     * Server-side created timestamp
     */
    @Column(nullable = false)
    private Instant createdAt;

    /**
     * Server-side updated timestamp
     */
    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
