package com.example.demologin.controller;

import java.time.Instant;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demologin.annotation.SecuredEndpoint;
import com.example.demologin.service.GoogleCalendarService;
import com.example.demologin.utils.AccountUtils;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/calendar")
@RequiredArgsConstructor
public class CalendarController {

    private final AccountUtils accountUtils;
    private final GoogleCalendarService googleCalendarService;

    @GetMapping("/events")
    @SecuredEndpoint("CALENDAR_READ")
    public ResponseEntity<Map> getEvents(@RequestParam(required = false) String calendarId,
                                         @RequestParam(required = false) String timeMin,
                                         @RequestParam(required = false) String timeMax,
                                         @RequestParam(required = false, name = "googleAccessToken") String googleAccessToken) {
        Long userId = accountUtils.getCurrentUser().getUserId();
        Instant tMin = timeMin == null ? null : Instant.parse(timeMin);
        Instant tMax = timeMax == null ? null : Instant.parse(timeMax);
        Map events;
        if (googleAccessToken != null && !googleAccessToken.isEmpty()) {
            // validate provided token has required calendar scope before making API calls
            googleCalendarService.validateTokenHasCalendarScope(googleAccessToken);
            events = googleCalendarService.getEventsWithToken(googleAccessToken, calendarId, tMin, tMax);
        } else {
            events = googleCalendarService.getEvents(userId, calendarId, tMin, tMax);
        }
        return ResponseEntity.ok(events);
    }

    @PostMapping("/events")
    @SecuredEndpoint("CALENDAR_CREATE")
    public ResponseEntity<Map> createEvent(@RequestBody com.example.demologin.dto.request.CalendarEventRequest req,
                                           @RequestParam(required = false) String calendarId,
                                           @RequestParam(required = false, name = "googleAccessToken") String googleAccessToken) {
        Long userId = accountUtils.getCurrentUser().getUserId();
        Map created;
        if (googleAccessToken != null && !googleAccessToken.isEmpty()) {
            googleCalendarService.validateTokenHasCalendarScope(googleAccessToken);
            created = googleCalendarService.createEventWithToken(googleAccessToken, calendarId, req);
        } else {
            created = googleCalendarService.createEvent(userId, calendarId, req);
        }
        return ResponseEntity.status(201).body(created);
    }

    @PutMapping("/events/{eventId}")
    @SecuredEndpoint("CALENDAR_UPDATE")
    public ResponseEntity<Map> updateEvent(@PathVariable String eventId,
                                           @RequestBody com.example.demologin.dto.request.CalendarEventRequest req,
                                           @RequestParam(required = false) String calendarId,
                                           @RequestParam(required = false, name = "googleAccessToken") String googleAccessToken) {
        Long userId = accountUtils.getCurrentUser().getUserId();
        Map updated;
        if (googleAccessToken != null && !googleAccessToken.isEmpty()) {
            googleCalendarService.validateTokenHasCalendarScope(googleAccessToken);
            updated = googleCalendarService.updateEventWithToken(googleAccessToken, calendarId, eventId, req);
        } else {
            updated = googleCalendarService.updateEvent(userId, calendarId, eventId, req);
        }
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/events/{eventId}")
    @SecuredEndpoint("CALENDAR_DELETE")
    public ResponseEntity<?> deleteEvent(@PathVariable String eventId,
                                         @RequestParam(required = false) String calendarId,
                                         @RequestParam(required = false, name = "googleAccessToken") String googleAccessToken) {
        Long userId = accountUtils.getCurrentUser().getUserId();
        if (googleAccessToken != null && !googleAccessToken.isEmpty()) {
            googleCalendarService.validateTokenHasCalendarScope(googleAccessToken);
            googleCalendarService.deleteEventWithToken(googleAccessToken, calendarId, eventId);
        } else {
            googleCalendarService.deleteEvent(userId, calendarId, eventId);
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/token/validate")
    @SecuredEndpoint("CALENDAR_READ")
    public ResponseEntity<Map<String, Object>> validateToken(@RequestParam(name = "googleAccessToken") String googleAccessToken) {
        googleCalendarService.validateTokenHasCalendarScope(googleAccessToken);
        return ResponseEntity.ok(Map.of("valid", true));
    }

    @GetMapping("/credential")
    @SecuredEndpoint("CALENDAR_READ")
    public ResponseEntity<Map<String, Object>> getCredential() {
        Long userId = accountUtils.getCurrentUser().getUserId();
        String userEmail = accountUtils.getCurrentUser().getEmail();
        java.util.Optional<com.example.demologin.entity.CalendarCredential> opt = googleCalendarService.findCredential(userId);
        if (opt.isPresent()) {
            com.example.demologin.entity.CalendarCredential cred = opt.get();
            Map<String, Object> resp = new java.util.HashMap<>();
            resp.put("linked", true);
            resp.put("email", userEmail);
            resp.put("scopes", cred.getScopes());
            resp.put("expiresAt", cred.getExpiresAt());
            return ResponseEntity.ok(resp);
        } else {
            Map<String, Object> resp = Map.of("linked", false);
            return ResponseEntity.ok(resp);
        }
    }

    @GetMapping("/credential/token")
    @SecuredEndpoint("CALENDAR_READ")
    public ResponseEntity<Map<String, Object>> getCredentialToken() {
        Long userId = accountUtils.getCurrentUser().getUserId();
        // This will throw BadRequestException (mapped to 400) if missing
        Map<String, Object> token = googleCalendarService.getAccessToken(userId);
        return ResponseEntity.ok(token);
    }
}
