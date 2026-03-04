package com.example.demologin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Response DTO for chat memory
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMemoryResponse {

    private Long id;
    private String memory;
    private String category;
    private String source;
    private Instant createdAt;
    private Instant updatedAt;
}
