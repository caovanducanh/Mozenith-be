package com.example.demologin.service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.example.demologin.entity.CalendarCredential;

/**
 * Service interface for Google Calendar operations. Implementation lives in
 * {@code GoogleCalendarServiceImpl}.
 */
public interface GoogleCalendarService {

    Map createEvent(Long userId, String calendarId, com.example.demologin.dto.request.CalendarEventRequest req);
    Map createEventWithToken(String accessToken, String calendarId, com.example.demologin.dto.request.CalendarEventRequest req);

    Map updateEvent(Long userId, String calendarId, String eventId, com.example.demologin.dto.request.CalendarEventRequest req);
    Map updateEventWithToken(String accessToken, String calendarId, String eventId, com.example.demologin.dto.request.CalendarEventRequest req);

    void deleteEvent(Long userId, String calendarId, String eventId);
    void deleteEventWithToken(String accessToken, String calendarId, String eventId);

    void saveCredentialFromAuthorizedClient(Long userId, String accessToken, String refreshToken, Set<String> scopes, Instant expiresAt);

    Optional<CalendarCredential> findCredential(Long userId);

    CalendarCredential refreshIfNeeded(CalendarCredential cred);

    Map getEvents(Long userId, String calendarId, Instant timeMin, Instant timeMax);
    Map getEventsWithToken(String accessToken, String calendarId, Instant timeMin, Instant timeMax);

    /**
     * Validate that the provided OAuth2 access token includes the required
     * Google Calendar scope. Throws {@link com.example.demologin.exception.exceptions.ForbiddenException}
     * if the scope is missing, or {@link com.example.demologin.exception.exceptions.InvalidTokenException}
     * if the token is invalid.
     */
    void validateTokenHasCalendarScope(String accessToken);

    Map<String, Object> getAccessToken(Long userId);

    /**
     * Delete stored credential for a user.
     * Used when user wants to unlink their Google Calendar.
     * 
     * @param userId The user ID whose credential should be deleted
     */
    void deleteCredential(Long userId);
}
