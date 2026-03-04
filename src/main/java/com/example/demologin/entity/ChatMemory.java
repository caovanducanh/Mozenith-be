package com.example.demologin.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Entity to store AI chat memories/context per user.
 * Each user has their own isolated chat memory that persists across sessions.
 */
@Entity
@Table(name = "chat_memory", indexes = {
    @Index(name = "idx_chat_memory_user_id", columnList = "userId"),
    @Index(name = "idx_chat_memory_user_created", columnList = "userId, createdAt DESC")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMemory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    /**
     * The memory content (e.g., "User likes the color red", "User's car needs fixing")
     */
    @Column(nullable = false, length = 2048)
    private String memory;

    /**
     * Optional category for organizing memories (e.g., "preference", "task", "personal")
     */
    @Column(length = 100)
    private String category;

    /**
     * Source of the memory (e.g., "conversation", "explicit_save")
     */
    @Column(length = 50)
    private String source;

    @Column(nullable = false)
    private Instant createdAt;

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
