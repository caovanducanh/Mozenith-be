package com.example.demologin.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for adding chat memories
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMemoryRequest {

    /**
     * List of conversation messages to extract memories from
     */
    private List<ChatMessage> messages;

    /**
     * Direct memory to save (alternative to messages)
     */
    @Size(max = 2048, message = "Memory content cannot exceed 2048 characters")
    private String memory;

    /**
     * Optional category for the memory
     */
    @Size(max = 100, message = "Category cannot exceed 100 characters")
    private String category;

    /**
     * Source of the memory
     */
    @Size(max = 50, message = "Source cannot exceed 50 characters")
    private String source;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatMessage {
        @NotBlank(message = "Role is required")
        private String role;

        @NotBlank(message = "Content is required")
        private String content;
    }
}
