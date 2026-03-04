package com.example.demologin.service;

import com.example.demologin.dto.request.DiaryBulkSyncRequest;
import com.example.demologin.dto.request.DiaryEntryRequest;
import com.example.demologin.dto.response.DiaryEntryResponse;
import com.example.demologin.dto.response.DiarySummaryResponse;

import java.util.List;

/**
 * Service interface for managing user diary entries.
 * Provides complete isolation of diary data per user.
 */
public interface DiaryEntryService {

    /**
     * Create a new diary entry
     */
    DiaryEntryResponse createEntry(Long userId, DiaryEntryRequest request);

    /**
     * Update an existing diary entry
     */
    DiaryEntryResponse updateEntry(Long userId, Long entryId, DiaryEntryRequest request);

    /**
     * Get a single entry by ID
     */
    DiaryEntryResponse getEntry(Long userId, Long entryId);

    /**
     * Get all entries for a user
     */
    List<DiaryEntryResponse> getAllEntries(Long userId);

    /**
     * Get recent entries (last 50)
     */
    List<DiaryEntryResponse> getRecentEntries(Long userId);

    /**
     * Get entries by mood
     */
    List<DiaryEntryResponse> getEntriesByMood(Long userId, String mood);

    /**
     * Search entries by content keyword
     */
    List<DiaryEntryResponse> searchEntries(Long userId, String keyword);

    /**
     * Delete an entry
     */
    void deleteEntry(Long userId, Long entryId);

    /**
     * Delete all entries for a user
     */
    void deleteAllEntries(Long userId);

    /**
     * Get diary summary/statistics
     */
    DiarySummaryResponse getSummary(Long userId);

    /**
     * Bulk sync entries (upsert based on clientId)
     * Returns entries updated since lastSyncTimestamp
     */
    List<DiaryEntryResponse> bulkSync(Long userId, DiaryBulkSyncRequest request);

    /**
     * Get entry count for a user
     */
    long getEntryCount(Long userId);
}
