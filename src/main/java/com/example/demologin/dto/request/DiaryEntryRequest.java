package com.example.demologin.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for creating/updating diary entries
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiaryEntryRequest {

    /**
     * Client-side UUID for the entry (for sync purposes)
     */
    @Size(max = 36, message = "Client ID cannot exceed 36 characters")
    private String clientId;

    /**
     * The diary entry content
     */
    @NotBlank(message = "Content is required")
    @Size(max = 4096, message = "Content cannot exceed 4096 characters")
    private String content;

    /**
     * Mood enum value: VERY_HAPPY, HAPPY, NEUTRAL, SAD, VERY_SAD, ANGRY, ANXIOUS, EXCITED, TIRED, GRATEFUL, LOVED, CONFUSED
     */
    @Size(max = 20, message = "Mood cannot exceed 20 characters")
    private String mood;

    /**
     * Original timestamp from the client (ISO-8601 format)
     */
    private String timestamp;

    /**
     * Whether the entry was created via AI voice command
     */
    private boolean createdByVoice;

    /**
     * Tags for the entry
     */
    private List<String> tags;
}
