package com.example.demologin.service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Service to manage OAuth2 state for Google Calendar authorization.
 * 
 * State is used to:
 * 1. Prevent CSRF attacks
 * 2. Track which user initiated the OAuth flow
 * 3. Remember if it's a mobile or web flow
 * 
 * States are stored in memory with TTL of 5 minutes.
 * For production with multiple instances, consider using Redis.
 */
@Service
public class GoogleCalendarOAuthStateService {

    private static final Logger log = LoggerFactory.getLogger(GoogleCalendarOAuthStateService.class);
    
    // State TTL in seconds (5 minutes)
    private static final long STATE_TTL_SECONDS = 300;
    
    // In-memory state storage (for single-instance deployment)
    // For multi-instance, replace with Redis
    private final ConcurrentHashMap<String, OAuthStateEntry> stateStore = new ConcurrentHashMap<>();
    
    // Cleanup scheduler
    private final ScheduledExecutorService cleanupScheduler;
    
    public GoogleCalendarOAuthStateService() {
        // Schedule cleanup of expired states every minute
        cleanupScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "oauth-state-cleanup");
            t.setDaemon(true);
            return t;
        });
        cleanupScheduler.scheduleAtFixedRate(this::cleanupExpiredStates, 1, 1, TimeUnit.MINUTES);
    }
    
    /**
     * Save OAuth state with user info.
     * 
     * @param state The unique state string
     * @param userId The user ID initiating the OAuth flow
     * @param mobile Whether this is a mobile flow
     */
    public void saveState(String state, Long userId, boolean mobile) {
        OAuthStateEntry entry = new OAuthStateEntry(
            new OAuthState(userId, mobile),
            Instant.now().plusSeconds(STATE_TTL_SECONDS)
        );
        stateStore.put(state, entry);
        log.debug("Saved OAuth state: {} for user={} (mobile={})", state, userId, mobile);
    }
    
    /**
     * Get and remove state (one-time use).
     * Returns null if state not found or expired.
     * 
     * @param state The state string to validate
     * @return OAuthState if valid, null otherwise
     */
    public OAuthState getAndRemoveState(String state) {
        OAuthStateEntry entry = stateStore.remove(state);
        
        if (entry == null) {
            log.warn("State not found: {}", state);
            return null;
        }
        
        if (entry.getExpiresAt().isBefore(Instant.now())) {
            log.warn("State expired: {} (expired at {})", state, entry.getExpiresAt());
            return null;
        }
        
        log.debug("Retrieved and removed OAuth state: {} for user={}", state, entry.getState().getUserId());
        return entry.getState();
    }
    
    /**
     * Check if state exists (without removing).
     */
    public boolean hasState(String state) {
        OAuthStateEntry entry = stateStore.get(state);
        return entry != null && entry.getExpiresAt().isAfter(Instant.now());
    }
    
    /**
     * Clean up expired states.
     */
    private void cleanupExpiredStates() {
        Instant now = Instant.now();
        int removed = 0;
        for (String key : stateStore.keySet()) {
            OAuthStateEntry entry = stateStore.get(key);
            if (entry != null && entry.getExpiresAt().isBefore(now)) {
                stateStore.remove(key);
                removed++;
            }
        }
        if (removed > 0) {
            log.debug("Cleaned up {} expired OAuth states", removed);
        }
    }
    
    /**
     * OAuth state data object.
     */
    @Data
    @AllArgsConstructor
    public static class OAuthState {
        private Long userId;
        private boolean mobile;
    }
    
    /**
     * Internal entry with expiration time.
     */
    @Data
    @AllArgsConstructor
    private static class OAuthStateEntry {
        private OAuthState state;
        private Instant expiresAt;
    }
}
