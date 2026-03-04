package com.example.demologin.serviceImpl;

import com.example.demologin.dto.request.ChatMemoryRequest;
import com.example.demologin.dto.response.ChatMemoryResponse;
import com.example.demologin.entity.ChatMemory;
import com.example.demologin.exception.exceptions.ResourceNotFoundException;
import com.example.demologin.repository.ChatMemoryRepository;
import com.example.demologin.service.ChatMemoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of ChatMemoryService.
 * Extracts and stores meaningful memories from conversations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMemoryServiceImpl implements ChatMemoryService {

    private final ChatMemoryRepository chatMemoryRepository;

    @Override
    @Transactional
    public List<ChatMemoryResponse> addMemories(Long userId, ChatMemoryRequest request) {
        List<ChatMemory> savedMemories = new ArrayList<>();

        // If direct memory is provided, save it
        if (request.getMemory() != null && !request.getMemory().isBlank()) {
            ChatMemory memory = ChatMemory.builder()
                    .userId(userId)
                    .memory(request.getMemory().trim())
                    .category(request.getCategory())
                    .source(request.getSource() != null ? request.getSource() : "explicit_save")
                    .build();
            savedMemories.add(chatMemoryRepository.save(memory));
            log.info("💾 Saved direct memory for user {}: {}", userId, truncate(request.getMemory(), 50));
        }

        // If messages are provided, extract and save memories from them
        if (request.getMessages() != null && !request.getMessages().isEmpty()) {
            List<ChatMemory> extractedMemories = extractMemoriesFromMessages(userId, request.getMessages(), request.getSource());
            savedMemories.addAll(chatMemoryRepository.saveAll(extractedMemories));
            log.info("💾 Extracted and saved {} memories from conversation for user {}", extractedMemories.size(), userId);
        }

        return savedMemories.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ChatMemoryResponse> getAllMemories(Long userId) {
        return chatMemoryRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ChatMemoryResponse> getRecentMemories(Long userId) {
        return chatMemoryRepository.findTop50ByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ChatMemoryResponse> searchMemories(Long userId, String keyword) {
        return chatMemoryRepository.searchByKeyword(userId, keyword)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteMemory(Long userId, Long memoryId) {
        ChatMemory memory = chatMemoryRepository.findById(memoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Memory not found with id: " + memoryId));
        
        if (!memory.getUserId().equals(userId)) {
            throw new SecurityException("User does not own this memory");
        }
        
        chatMemoryRepository.delete(memory);
        log.info("🗑️ Deleted memory {} for user {}", memoryId, userId);
    }

    @Override
    @Transactional
    public void deleteAllMemories(Long userId) {
        chatMemoryRepository.deleteByUserId(userId);
        log.info("🗑️ Deleted all memories for user {}", userId);
    }

    @Override
    public long getMemoryCount(Long userId) {
        return chatMemoryRepository.countByUserId(userId);
    }

    /**
     * Extract meaningful memories from conversation messages.
     * This is a simple extraction - for production, you might want to use AI to extract memories.
     */
    private List<ChatMemory> extractMemoriesFromMessages(Long userId, List<ChatMemoryRequest.ChatMessage> messages, String source) {
        List<ChatMemory> memories = new ArrayList<>();
        StringBuilder conversationContext = new StringBuilder();

        for (ChatMemoryRequest.ChatMessage msg : messages) {
            if (msg.getContent() != null && !msg.getContent().isBlank()) {
                conversationContext.append(msg.getRole()).append(": ").append(msg.getContent()).append("\n");
            }
        }

        // Extract user messages as potential memories (simple approach)
        for (ChatMemoryRequest.ChatMessage msg : messages) {
            if ("user".equalsIgnoreCase(msg.getRole()) && msg.getContent() != null) {
                String content = msg.getContent().trim();
                
                // Skip very short or greeting messages
                if (content.length() > 20 && !isGreeting(content)) {
                    // Check if this might be a preference or important info
                    if (mightBeImportant(content)) {
                        ChatMemory memory = ChatMemory.builder()
                                .userId(userId)
                                .memory(content)
                                .category(categorize(content))
                                .source(source != null ? source : "conversation")
                                .build();
                        memories.add(memory);
                    }
                }
            }
        }

        return memories;
    }

    /**
     * Check if a message is a simple greeting
     */
    private boolean isGreeting(String content) {
        String lower = content.toLowerCase();
        String[] greetings = {"hello", "hi", "hey", "good morning", "good afternoon", "good evening", 
                              "how are you", "what's up", "bye", "goodbye", "thanks", "thank you"};
        for (String greeting : greetings) {
            if (lower.startsWith(greeting) || lower.equals(greeting)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if a message might contain important information worth remembering
     */
    private boolean mightBeImportant(String content) {
        String lower = content.toLowerCase();
        String[] importantKeywords = {
            "i like", "i love", "i prefer", "i hate", "i don't like",
            "my favorite", "my name", "i am", "i'm",
            "remind me", "remember", "don't forget",
            "schedule", "appointment", "meeting",
            "i need", "i want", "i have",
            "birthday", "anniversary", "car", "work", "job",
            "allergy", "allergic", "medical", "health"
        };
        
        for (String keyword : importantKeywords) {
            if (lower.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Categorize the memory based on content
     */
    private String categorize(String content) {
        String lower = content.toLowerCase();
        
        if (lower.contains("like") || lower.contains("love") || lower.contains("prefer") || lower.contains("favorite")) {
            return "preference";
        }
        if (lower.contains("schedule") || lower.contains("appointment") || lower.contains("meeting") || lower.contains("remind")) {
            return "task";
        }
        if (lower.contains("birthday") || lower.contains("name") || lower.contains("anniversary")) {
            return "personal";
        }
        if (lower.contains("work") || lower.contains("job") || lower.contains("project")) {
            return "work";
        }
        if (lower.contains("health") || lower.contains("allergy") || lower.contains("medical")) {
            return "health";
        }
        
        return "general";
    }

    private ChatMemoryResponse toResponse(ChatMemory memory) {
        return ChatMemoryResponse.builder()
                .id(memory.getId())
                .memory(memory.getMemory())
                .category(memory.getCategory())
                .source(memory.getSource())
                .createdAt(memory.getCreatedAt())
                .updatedAt(memory.getUpdatedAt())
                .build();
    }

    private String truncate(String s, int maxLength) {
        if (s == null) return "";
        return s.length() > maxLength ? s.substring(0, maxLength) + "..." : s;
    }
}
