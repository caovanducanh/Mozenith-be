package com.example.demologin.controller;

import com.example.demologin.annotation.SecuredEndpoint;
import com.example.demologin.dto.request.DiaryBulkSyncRequest;
import com.example.demologin.dto.request.DiaryEntryRequest;
import com.example.demologin.dto.response.DiaryEntryResponse;
import com.example.demologin.dto.response.DiarySummaryResponse;
import com.example.demologin.service.DiaryEntryService;
import com.example.demologin.utils.AccountUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller for managing user diary/journal entries.
 * Provides CRUD operations with complete user isolation and cloud sync.
 */
@Slf4j
@RestController
@RequestMapping("/api/diary")
@RequiredArgsConstructor
@Tag(name = "Diary", description = "Endpoints for managing diary/journal entries")
public class DiaryController {

    private final DiaryEntryService diaryEntryService;
    private final AccountUtils accountUtils;

    @PostMapping
    @SecuredEndpoint("DIARY_CREATE")
    @Operation(summary = "Create diary entry", description = "Create a new diary entry")
    public ResponseEntity<DiaryEntryResponse> createEntry(@Valid @RequestBody DiaryEntryRequest request) {
        Long userId = accountUtils.getCurrentUser().getUserId();
        log.info("📔 Creating diary entry for user {}", userId);
        DiaryEntryResponse entry = diaryEntryService.createEntry(userId, request);
        return ResponseEntity.status(201).body(entry);
    }

    @PutMapping("/{entryId}")
    @SecuredEndpoint("DIARY_UPDATE")
    @Operation(summary = "Update diary entry", description = "Update an existing diary entry")
    public ResponseEntity<DiaryEntryResponse> updateEntry(
            @PathVariable Long entryId,
            @Valid @RequestBody DiaryEntryRequest request) {
        Long userId = accountUtils.getCurrentUser().getUserId();
        log.info("📔 Updating diary entry {} for user {}", entryId, userId);
        DiaryEntryResponse entry = diaryEntryService.updateEntry(userId, entryId, request);
        return ResponseEntity.ok(entry);
    }

    @GetMapping("/{entryId}")
    @SecuredEndpoint("DIARY_READ")
    @Operation(summary = "Get diary entry", description = "Get a specific diary entry by ID")
    public ResponseEntity<DiaryEntryResponse> getEntry(@PathVariable Long entryId) {
        Long userId = accountUtils.getCurrentUser().getUserId();
        DiaryEntryResponse entry = diaryEntryService.getEntry(userId, entryId);
        return ResponseEntity.ok(entry);
    }

    @GetMapping
    @SecuredEndpoint("DIARY_READ")
    @Operation(summary = "Get all diary entries", description = "Get all diary entries for the authenticated user")
    public ResponseEntity<List<DiaryEntryResponse>> getAllEntries() {
        Long userId = accountUtils.getCurrentUser().getUserId();
        log.info("📖 Fetching all diary entries for user {}", userId);
        List<DiaryEntryResponse> entries = diaryEntryService.getAllEntries(userId);
        return ResponseEntity.ok(entries);
    }

    @GetMapping("/recent")
    @SecuredEndpoint("DIARY_READ")
    @Operation(summary = "Get recent diary entries", description = "Get the 50 most recent diary entries")
    public ResponseEntity<List<DiaryEntryResponse>> getRecentEntries() {
        Long userId = accountUtils.getCurrentUser().getUserId();
        List<DiaryEntryResponse> entries = diaryEntryService.getRecentEntries(userId);
        return ResponseEntity.ok(entries);
    }

    @GetMapping("/mood/{mood}")
    @SecuredEndpoint("DIARY_READ")
    @Operation(summary = "Get entries by mood", description = "Get diary entries filtered by mood")
    public ResponseEntity<List<DiaryEntryResponse>> getEntriesByMood(@PathVariable String mood) {
        Long userId = accountUtils.getCurrentUser().getUserId();
        List<DiaryEntryResponse> entries = diaryEntryService.getEntriesByMood(userId, mood);
        return ResponseEntity.ok(entries);
    }

    @GetMapping("/search")
    @SecuredEndpoint("DIARY_READ")
    @Operation(summary = "Search diary entries", description = "Search diary entries by keyword")
    public ResponseEntity<List<DiaryEntryResponse>> searchEntries(@RequestParam String keyword) {
        Long userId = accountUtils.getCurrentUser().getUserId();
        log.info("🔍 Searching diary entries for user {} with keyword: {}", userId, keyword);
        List<DiaryEntryResponse> entries = diaryEntryService.searchEntries(userId, keyword);
        return ResponseEntity.ok(entries);
    }

    @DeleteMapping("/{entryId}")
    @SecuredEndpoint("DIARY_DELETE")
    @Operation(summary = "Delete diary entry", description = "Delete a specific diary entry")
    public ResponseEntity<Void> deleteEntry(@PathVariable Long entryId) {
        Long userId = accountUtils.getCurrentUser().getUserId();
        log.info("🗑️ Deleting diary entry {} for user {}", entryId, userId);
        diaryEntryService.deleteEntry(userId, entryId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    @SecuredEndpoint("DIARY_DELETE")
    @Operation(summary = "Delete all diary entries", description = "Delete all diary entries for the authenticated user")
    public ResponseEntity<Void> deleteAllEntries() {
        Long userId = accountUtils.getCurrentUser().getUserId();
        log.info("🗑️ Deleting all diary entries for user {}", userId);
        diaryEntryService.deleteAllEntries(userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/summary")
    @SecuredEndpoint("DIARY_READ")
    @Operation(summary = "Get diary summary", description = "Get summary statistics of diary entries")
    public ResponseEntity<DiarySummaryResponse> getSummary() {
        Long userId = accountUtils.getCurrentUser().getUserId();
        DiarySummaryResponse summary = diaryEntryService.getSummary(userId);
        return ResponseEntity.ok(summary);
    }

    @PostMapping("/sync")
    @SecuredEndpoint("DIARY_CREATE")
    @Operation(summary = "Bulk sync diary entries", description = "Sync multiple diary entries (upsert based on clientId)")
    public ResponseEntity<List<DiaryEntryResponse>> bulkSync(@Valid @RequestBody DiaryBulkSyncRequest request) {
        Long userId = accountUtils.getCurrentUser().getUserId();
        log.info("🔄 Syncing {} diary entries for user {}", request.getEntries().size(), userId);
        List<DiaryEntryResponse> entries = diaryEntryService.bulkSync(userId, request);
        return ResponseEntity.ok(entries);
    }

    @GetMapping("/count")
    @SecuredEndpoint("DIARY_READ")
    @Operation(summary = "Get diary entry count", description = "Get the number of diary entries for the authenticated user")
    public ResponseEntity<Map<String, Long>> getEntryCount() {
        Long userId = accountUtils.getCurrentUser().getUserId();
        long count = diaryEntryService.getEntryCount(userId);
        return ResponseEntity.ok(Map.of("count", count));
    }
}
