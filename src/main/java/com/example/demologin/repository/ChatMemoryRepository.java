package com.example.demologin.repository;

import com.example.demologin.entity.ChatMemory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMemoryRepository extends JpaRepository<ChatMemory, Long> {

    /**
     * Find all memories for a specific user, ordered by most recent first
     */
    List<ChatMemory> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Find all memories for a user with pagination
     */
    Page<ChatMemory> findByUserId(Long userId, Pageable pageable);

    /**
     * Find memories by user and category
     */
    List<ChatMemory> findByUserIdAndCategoryOrderByCreatedAtDesc(Long userId, String category);

    /**
     * Search memories by keyword (case-insensitive) for a specific user
     */
    @Query("SELECT m FROM ChatMemory m WHERE m.userId = :userId AND LOWER(m.memory) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY m.createdAt DESC")
    List<ChatMemory> searchByKeyword(@Param("userId") Long userId, @Param("keyword") String keyword);

    /**
     * Get the most recent N memories for a user
     */
    List<ChatMemory> findTop50ByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Delete all memories for a user
     */
    void deleteByUserId(Long userId);

    /**
     * Count memories for a user
     */
    long countByUserId(Long userId);
}
