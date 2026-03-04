package com.example.demologin.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for bulk sync of diary entries
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiaryBulkSyncRequest {

    /**
     * List of entries to sync (upsert based on clientId)
     */
    @NotEmpty(message = "Entries list cannot be empty")
    @Valid
    private List<DiaryEntryRequest> entries;

    /**
     * Timestamp of last sync (ISO-8601 format) - server returns entries updated after this
     */
    private String lastSyncTimestamp;
}
