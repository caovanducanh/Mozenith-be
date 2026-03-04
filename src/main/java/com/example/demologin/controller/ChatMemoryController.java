package com.example.demologin.controller;

import com.example.demologin.annotation.SecuredEndpoint;
import com.example.demologin.dto.request.ChatMemoryRequest;
import com.example.demologin.dto.response.ChatMemoryResponse;
import com.example.demologin.service.ChatMemoryService;
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
 * Controller for managing user chat memories.
 * Provides CRUD operations for AI assistant memories with complete user isolation.
 */
@Slf4j
@RestController
@RequestMapping("/api/chat-memory")
@RequiredArgsConstructor
@Tag(name = "Chat Memory", description = "Endpoints for managing AI assistant memories")
public class ChatMemoryController {

    private final ChatMemoryService chatMemoryService;
    private final AccountUtils accountUtils;

    @PostMapping
    @SecuredEndpoint("CHAT_MEMORY_CREATE")
    @Operation(summary = "Add memories", description = "Add memories from conversation or direct input")
    public ResponseEntity<List<ChatMemoryResponse>> addMemories(@Valid @RequestBody ChatMemoryRequest request) {
        Long userId = accountUtils.getCurrentUser().getUserId();
        log.info("💾 Adding memories for user {}", userId);
        List<ChatMemoryResponse> memories = chatMemoryService.addMemories(userId, request);
        return ResponseEntity.status(201).body(memories);
    }

    @GetMapping
    @SecuredEndpoint("CHAT_MEMORY_READ")
    @Operation(summary = "Get all memories", description = "Get all memories for the authenticated user")
    public ResponseEntity<List<ChatMemoryResponse>> getAllMemories() {
        Long userId = accountUtils.getCurrentUser().getUserId();
        log.info("📖 Fetching all memories for user {}", userId);
        List<ChatMemoryResponse> memories = chatMemoryService.getAllMemories(userId);
        return ResponseEntity.ok(memories);
    }

    @GetMapping("/recent")
    @SecuredEndpoint("CHAT_MEMORY_READ")
    @Operation(summary = "Get recent memories", description = "Get the 50 most recent memories")
    public ResponseEntity<List<ChatMemoryResponse>> getRecentMemories() {
        Long userId = accountUtils.getCurrentUser().getUserId();
        List<ChatMemoryResponse> memories = chatMemoryService.getRecentMemories(userId);
        return ResponseEntity.ok(memories);
    }

    @GetMapping("/search")
    @SecuredEndpoint("CHAT_MEMORY_READ")
    @Operation(summary = "Search memories", description = "Search memories by keyword")
    public ResponseEntity<List<ChatMemoryResponse>> searchMemories(@RequestParam String keyword) {
        Long userId = accountUtils.getCurrentUser().getUserId();
        log.info("🔍 Searching memories for user {} with keyword: {}", userId, keyword);
        List<ChatMemoryResponse> memories = chatMemoryService.searchMemories(userId, keyword);
        return ResponseEntity.ok(memories);
    }

    @DeleteMapping("/{memoryId}")
    @SecuredEndpoint("CHAT_MEMORY_DELETE")
    @Operation(summary = "Delete a memory", description = "Delete a specific memory by ID")
    public ResponseEntity<Void> deleteMemory(@PathVariable Long memoryId) {
        Long userId = accountUtils.getCurrentUser().getUserId();
        log.info("🗑️ Deleting memory {} for user {}", memoryId, userId);
        chatMemoryService.deleteMemory(userId, memoryId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    @SecuredEndpoint("CHAT_MEMORY_DELETE")
    @Operation(summary = "Delete all memories", description = "Delete all memories for the authenticated user")
    public ResponseEntity<Void> deleteAllMemories() {
        Long userId = accountUtils.getCurrentUser().getUserId();
        log.info("🗑️ Deleting all memories for user {}", userId);
        chatMemoryService.deleteAllMemories(userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/count")
    @SecuredEndpoint("CHAT_MEMORY_READ")
    @Operation(summary = "Get memory count", description = "Get the number of memories for the authenticated user")
    public ResponseEntity<Map<String, Long>> getMemoryCount() {
        Long userId = accountUtils.getCurrentUser().getUserId();
        long count = chatMemoryService.getMemoryCount(userId);
        return ResponseEntity.ok(Map.of("count", count));
    }
}
