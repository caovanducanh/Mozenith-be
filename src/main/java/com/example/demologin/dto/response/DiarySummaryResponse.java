package com.example.demologin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Response DTO for diary summary/statistics
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiarySummaryResponse {

    private long totalEntries;
    private Map<String, Long> moodCounts;
    private String dominantMood;
    private String lastEntryDate;
    private String lastEntryContent;
}
