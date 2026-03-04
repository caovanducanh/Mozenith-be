package com.example.demologin.repository;

import com.example.demologin.entity.DiaryEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface DiaryEntryRepository extends JpaRepository<DiaryEntry, Long> {

    /**
     * Find all entries for a user, ordered by timestamp (newest first)
     */
    List<DiaryEntry> findByUserIdOrderByTimestampDesc(Long userId);

    /**
     * Find entries for a user with pagination
     */
    Page<DiaryEntry> findByUserId(Long userId, Pageable pageable);

    /**
     * Find entry by user and client ID (for sync/upsert)
     */
    Optional<DiaryEntry> findByUserIdAndClientId(Long userId, String clientId);

    /**
     * Find entries by user and mood
     */
    List<DiaryEntry> findByUserIdAndMoodOrderByTimestampDesc(Long userId, String mood);

    /**
     * Find entries after a certain timestamp (for incremental sync)
     */
    List<DiaryEntry> findByUserIdAndUpdatedAtAfterOrderByTimestampDesc(Long userId, Instant since);

    /**
     * Search entries by content (case-insensitive) for a specific user
     */
    @Query("SELECT d FROM DiaryEntry d WHERE d.userId = :userId AND LOWER(d.content) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY d.timestamp DESC")
    List<DiaryEntry> searchByContent(@Param("userId") Long userId, @Param("keyword") String keyword);

    /**
     * Get the most recent N entries for a user
     */
    List<DiaryEntry> findTop50ByUserIdOrderByTimestampDesc(Long userId);

    /**
     * Find entries within a date range
     */
    List<DiaryEntry> findByUserIdAndTimestampBetweenOrderByTimestampDesc(Long userId, Instant start, Instant end);

    /**
     * Delete all entries for a user
     */
    void deleteByUserId(Long userId);

    /**
     * Count entries for a user
     */
    long countByUserId(Long userId);

    /**
     * Count entries by mood for a user
     */
    @Query("SELECT d.mood, COUNT(d) FROM DiaryEntry d WHERE d.userId = :userId GROUP BY d.mood")
    List<Object[]> countByMoodForUser(@Param("userId") Long userId);
}
