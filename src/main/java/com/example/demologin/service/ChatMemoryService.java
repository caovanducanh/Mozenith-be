package com.example.demologin.service;

import com.example.demologin.dto.request.ChatMemoryRequest;
import com.example.demologin.dto.response.ChatMemoryResponse;

import java.util.List;

/**
 * Service interface for managing user chat memories.
 * Provides complete isolation of memories per user.
 */
public interface ChatMemoryService {

    /**
     * Add memories from conversation messages
     * 
     * @param userId The user ID
     * @param request The request containing messages or direct memory
     * @return List of created memories
     */
    List<ChatMemoryResponse> addMemories(Long userId, ChatMemoryRequest request);

    /**
     * Get all memories for a user
     * 
     * @param userId The user ID
     * @return List of user's memories
     */
    List<ChatMemoryResponse> getAllMemories(Long userId);

    /**
     * Get recent memories for a user (limited to 50)
     * 
     * @param userId The user ID
     * @return List of recent memories
     */
    List<ChatMemoryResponse> getRecentMemories(Long userId);

    /**
     * Search memories by keyword
     * 
     * @param userId The user ID
     * @param keyword Search keyword
     * @return List of matching memories
     */
    List<ChatMemoryResponse> searchMemories(Long userId, String keyword);

    /**
     * Delete a specific memory
     * 
     * @param userId The user ID
     * @param memoryId The memory ID
     */
    void deleteMemory(Long userId, Long memoryId);

    /**
     * Delete all memories for a user
     * 
     * @param userId The user ID
     */
    void deleteAllMemories(Long userId);

    /**
     * Get memory count for a user
     * 
     * @param userId The user ID
     * @return Number of memories
     */
    long getMemoryCount(Long userId);
}
