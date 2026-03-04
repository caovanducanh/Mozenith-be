package com.example.demologin.serviceImpl;

import com.example.demologin.dto.request.DiaryBulkSyncRequest;
import com.example.demologin.dto.request.DiaryEntryRequest;
import com.example.demologin.dto.response.DiaryEntryResponse;
import com.example.demologin.dto.response.DiarySummaryResponse;
import com.example.demologin.entity.DiaryEntry;
import com.example.demologin.exception.exceptions.NotFoundException;
import com.example.demologin.repository.DiaryEntryRepository;
import com.example.demologin.service.DiaryEntryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of DiaryEntryService.
 * Manages diary entries with full user isolation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DiaryEntryServiceImpl implements DiaryEntryService {

    private final DiaryEntryRepository diaryEntryRepository;

    private static final Map<String, DiaryEntryResponse.MoodInfo> MOOD_INFO = Map.ofEntries(
        Map.entry("VERY_HAPPY", new DiaryEntryResponse.MoodInfo("😄", "Very Happy")),
        Map.entry("HAPPY", new DiaryEntryResponse.MoodInfo("🙂", "Happy")),
        Map.entry("NEUTRAL", new DiaryEntryResponse.MoodInfo("😐", "Neutral")),
        Map.entry("SAD", new DiaryEntryResponse.MoodInfo("😢", "Sad")),
        Map.entry("VERY_SAD", new DiaryEntryResponse.MoodInfo("😭", "Very Sad")),
        Map.entry("ANGRY", new DiaryEntryResponse.MoodInfo("😠", "Angry")),
        Map.entry("ANXIOUS", new DiaryEntryResponse.MoodInfo("😰", "Anxious")),
        Map.entry("EXCITED", new DiaryEntryResponse.MoodInfo("🤩", "Excited")),
        Map.entry("TIRED", new DiaryEntryResponse.MoodInfo("😴", "Tired")),
        Map.entry("GRATEFUL", new DiaryEntryResponse.MoodInfo("🙏", "Grateful")),
        Map.entry("LOVED", new DiaryEntryResponse.MoodInfo("🥰", "Loved")),
        Map.entry("CONFUSED", new DiaryEntryResponse.MoodInfo("😕", "Confused"))
    );

    @Override
    @Transactional
    public DiaryEntryResponse createEntry(Long userId, DiaryEntryRequest request) {
        DiaryEntry entry = buildEntryFromRequest(userId, request);
        DiaryEntry saved = diaryEntryRepository.save(entry);
        log.info("📔 Created diary entry {} for user {}", saved.getId(), userId);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public DiaryEntryResponse updateEntry(Long userId, Long entryId, DiaryEntryRequest request) {
        DiaryEntry entry = diaryEntryRepository.findById(entryId)
                .orElseThrow(() -> new NotFoundException("Diary entry not found with id: " + entryId));

        if (!entry.getUserId().equals(userId)) {
            throw new SecurityException("User does not own this diary entry");
        }

        entry.setContent(request.getContent());
        if (request.getMood() != null) {
            entry.setMood(request.getMood().toUpperCase());
        }
        if (request.getTags() != null) {
            entry.setTags(String.join(",", request.getTags()));
        }
        entry.setCreatedByVoice(request.isCreatedByVoice());

        DiaryEntry saved = diaryEntryRepository.save(entry);
        log.info("📔 Updated diary entry {} for user {}", entryId, userId);
        return toResponse(saved);
    }

    @Override
    public DiaryEntryResponse getEntry(Long userId, Long entryId) {
        DiaryEntry entry = diaryEntryRepository.findById(entryId)
                .orElseThrow(() -> new NotFoundException("Diary entry not found with id: " + entryId));

        if (!entry.getUserId().equals(userId)) {
            throw new SecurityException("User does not own this diary entry");
        }

        return toResponse(entry);
    }

    @Override
    public List<DiaryEntryResponse> getAllEntries(Long userId) {
        return diaryEntryRepository.findByUserIdOrderByTimestampDesc(userId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<DiaryEntryResponse> getRecentEntries(Long userId) {
        return diaryEntryRepository.findTop50ByUserIdOrderByTimestampDesc(userId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<DiaryEntryResponse> getEntriesByMood(Long userId, String mood) {
        return diaryEntryRepository.findByUserIdAndMoodOrderByTimestampDesc(userId, mood.toUpperCase())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<DiaryEntryResponse> searchEntries(Long userId, String keyword) {
        return diaryEntryRepository.searchByContent(userId, keyword)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteEntry(Long userId, Long entryId) {
        DiaryEntry entry = diaryEntryRepository.findById(entryId)
                .orElseThrow(() -> new NotFoundException("Diary entry not found with id: " + entryId));

        if (!entry.getUserId().equals(userId)) {
            throw new SecurityException("User does not own this diary entry");
        }

        diaryEntryRepository.delete(entry);
        log.info("🗑️ Deleted diary entry {} for user {}", entryId, userId);
    }

    @Override
    @Transactional
    public void deleteAllEntries(Long userId) {
        diaryEntryRepository.deleteByUserId(userId);
        log.info("🗑️ Deleted all diary entries for user {}", userId);
    }

    @Override
    public DiarySummaryResponse getSummary(Long userId) {
        List<DiaryEntry> entries = diaryEntryRepository.findByUserIdOrderByTimestampDesc(userId);

        Map<String, Long> moodCounts = entries.stream()
                .collect(Collectors.groupingBy(DiaryEntry::getMood, Collectors.counting()));

        String dominantMood = moodCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("NEUTRAL");

        DiaryEntry lastEntry = entries.isEmpty() ? null : entries.get(0);

        return DiarySummaryResponse.builder()
                .totalEntries(entries.size())
                .moodCounts(moodCounts)
                .dominantMood(dominantMood)
                .lastEntryDate(lastEntry != null ? formatDate(lastEntry.getTimestamp()) : null)
                .lastEntryContent(lastEntry != null ? truncate(lastEntry.getContent(), 100) : null)
                .build();
    }

    @Override
    @Transactional
    public List<DiaryEntryResponse> bulkSync(Long userId, DiaryBulkSyncRequest request) {
        // Upsert incoming entries
        for (DiaryEntryRequest entryReq : request.getEntries()) {
            if (entryReq.getClientId() != null && !entryReq.getClientId().isBlank()) {
                // Try to find existing entry by clientId
                Optional<DiaryEntry> existing = diaryEntryRepository.findByUserIdAndClientId(userId, entryReq.getClientId());
                if (existing.isPresent()) {
                    // Update existing
                    DiaryEntry entry = existing.get();
                    entry.setContent(entryReq.getContent());
                    if (entryReq.getMood() != null) {
                        entry.setMood(entryReq.getMood().toUpperCase());
                    }
                    if (entryReq.getTags() != null) {
                        entry.setTags(String.join(",", entryReq.getTags()));
                    }
                    entry.setCreatedByVoice(entryReq.isCreatedByVoice());
                    diaryEntryRepository.save(entry);
                } else {
                    // Create new
                    DiaryEntry entry = buildEntryFromRequest(userId, entryReq);
                    diaryEntryRepository.save(entry);
                }
            } else {
                // No clientId, just create new
                DiaryEntry entry = buildEntryFromRequest(userId, entryReq);
                diaryEntryRepository.save(entry);
            }
        }

        log.info("📔 Synced {} diary entries for user {}", request.getEntries().size(), userId);

        // Return entries updated since lastSyncTimestamp (or all if not provided)
        if (request.getLastSyncTimestamp() != null && !request.getLastSyncTimestamp().isBlank()) {
            Instant since = Instant.parse(request.getLastSyncTimestamp());
            return diaryEntryRepository.findByUserIdAndUpdatedAtAfterOrderByTimestampDesc(userId, since)
                    .stream()
                    .map(this::toResponse)
                    .collect(Collectors.toList());
        } else {
            return getAllEntries(userId);
        }
    }

    @Override
    public long getEntryCount(Long userId) {
        return diaryEntryRepository.countByUserId(userId);
    }

    private DiaryEntry buildEntryFromRequest(Long userId, DiaryEntryRequest request) {
        String clientId = request.getClientId();
        if (clientId == null || clientId.isBlank()) {
            clientId = UUID.randomUUID().toString();
        }

        Instant timestamp = Instant.now();
        if (request.getTimestamp() != null && !request.getTimestamp().isBlank()) {
            try {
                timestamp = Instant.parse(request.getTimestamp());
            } catch (Exception e) {
                log.warn("Could not parse timestamp: {}, using current time", request.getTimestamp());
            }
        }

        String mood = request.getMood() != null ? request.getMood().toUpperCase() : "NEUTRAL";
        String tags = request.getTags() != null ? String.join(",", request.getTags()) : "";

        return DiaryEntry.builder()
                .clientId(clientId)
                .userId(userId)
                .content(request.getContent())
                .mood(mood)
                .timestamp(timestamp)
                .createdByVoice(request.isCreatedByVoice())
                .tags(tags)
                .build();
    }

    private DiaryEntryResponse toResponse(DiaryEntry entry) {
        List<String> tags = entry.getTags() != null && !entry.getTags().isBlank()
                ? Arrays.asList(entry.getTags().split(","))
                : Collections.emptyList();

        DiaryEntryResponse.MoodInfo moodInfo = MOOD_INFO.getOrDefault(
                entry.getMood(),
                new DiaryEntryResponse.MoodInfo("😐", "Neutral")
        );

        return DiaryEntryResponse.builder()
                .id(entry.getId())
                .clientId(entry.getClientId())
                .content(entry.getContent())
                .mood(entry.getMood())
                .moodInfo(moodInfo)
                .timestamp(entry.getTimestamp())
                .formattedDate(formatDate(entry.getTimestamp()))
                .createdByVoice(entry.isCreatedByVoice())
                .tags(tags)
                .createdAt(entry.getCreatedAt())
                .updatedAt(entry.getUpdatedAt())
                .build();
    }

    private String formatDate(Instant instant) {
        if (instant == null) return "";
        return DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a")
                .withZone(ZoneId.systemDefault())
                .format(instant);
    }

    private String truncate(String s, int maxLength) {
        if (s == null) return "";
        return s.length() > maxLength ? s.substring(0, maxLength) + "..." : s;
    }
}
