package com.example.demologin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Response DTO for diary entry
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiaryEntryResponse {

    private Long id;
    private String clientId;
    private String content;
    private String mood;
    private MoodInfo moodInfo;
    private Instant timestamp;
    private String formattedDate;
    private boolean createdByVoice;
    private List<String> tags;
    private Instant createdAt;
    private Instant updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MoodInfo {
        private String emoji;
        private String label;
    }
}
