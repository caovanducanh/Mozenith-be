package com.example.demologin.service;

import com.example.demologin.entity.CalendarCredential;
import com.example.demologin.repository.CalendarCredentialRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GoogleCalendarServiceTest {

    CalendarCredentialRepository repo;
    RestTemplate restTemplate;
    GoogleCalendarService service;

    @BeforeEach
    void setUp() {
        repo = mock(CalendarCredentialRepository.class);
        restTemplate = mock(RestTemplate.class);
        service = new GoogleCalendarServiceImpl(repo, restTemplate);
        // inject client id/secret via reflection if needed (skipped for unit test simplicity)
    }

    @Test
    void refreshIfNeeded_refreshes_when_expired() {
        CalendarCredential cred = CalendarCredential.builder()
                .id(1L)
                .userId(10L)
                .provider("google")
                .refreshToken("r1")
                .accessToken("old")
                .expiresAt(Instant.now().minusSeconds(3600))
                .build();

        when(repo.findByUserIdAndProvider(10L, "google")).thenReturn(Optional.of(cred));

        Map<String, Object> respBody = new HashMap<>();
        respBody.put("access_token", "newAccess");
        respBody.put("expires_in", 3600);
        ResponseEntity<Map> resp = new ResponseEntity<>(respBody, HttpStatus.OK);
        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class))).thenReturn(resp);

        GoogleCalendarService spySrv = Mockito.spy(service);
        // use repository mock behavior
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        // call refreshIfNeeded indirectly via getEvents
        // first, stub getEvents token fetch: findByUserIdAndProvider returns cred
        CalendarCredential updated = spySrv.refreshIfNeeded(cred);

        assertNotNull(updated);
        assertEquals("newAccess", updated.getAccessToken());
        assertTrue(updated.getExpiresAt().isAfter(Instant.now()));
    }

    @Test
    void create_update_delete_event_flow() {
        CalendarCredential cred = CalendarCredential.builder()
                .id(1L)
                .userId(10L)
                .provider("google")
                .refreshToken("r1")
                .accessToken("old")
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        when(repo.findByUserIdAndProvider(10L, "google")).thenReturn(Optional.of(cred));

        Map<String, Object> createdBody = new HashMap<>();
        createdBody.put("id", "evt1");
        ResponseEntity<Map> createdResp = new ResponseEntity<>(createdBody, HttpStatus.CREATED);
        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class))).thenReturn(createdResp);

        com.example.demologin.dto.request.CalendarEventRequest req = new com.example.demologin.dto.request.CalendarEventRequest();
        req.setSummary("Test");
        Map created = service.createEvent(10L, null, req);
        assertEquals("evt1", created.get("id"));

        Map<String, Object> updatedBody = new HashMap<>();
        updatedBody.put("id", "evt1");
        ResponseEntity<Map> updatedResp = new ResponseEntity<>(updatedBody, HttpStatus.OK);
        when(restTemplate.exchange(contains("/events/evt1"), any(), any(), eq(Map.class))).thenReturn(updatedResp);

        Map updated = service.updateEvent(10L, null, "evt1", req);
        assertEquals("evt1", updated.get("id"));

        // delete: return no content
        when(restTemplate.exchange(contains("/events/evt1"), eq(org.springframework.http.HttpMethod.DELETE), any(), eq(Void.class))).thenReturn(new ResponseEntity<Void>(HttpStatus.NO_CONTENT));
        service.deleteEvent(10L, null, "evt1");
        // no exception
    }

    @Test
    void operations_throw_bad_request_when_missing_credential() {
        when(repo.findByUserIdAndProvider(99L, "google")).thenReturn(Optional.empty());

        com.example.demologin.dto.request.CalendarEventRequest req = new com.example.demologin.dto.request.CalendarEventRequest();
        req.setSummary("X");

        assertThrows(com.example.demologin.exception.exceptions.BadRequestException.class, () -> service.createEvent(99L, null, req));
        assertThrows(com.example.demologin.exception.exceptions.BadRequestException.class, () -> service.getEvents(99L, null, null, null));
        assertThrows(com.example.demologin.exception.exceptions.BadRequestException.class, () -> service.updateEvent(99L, null, "id", req));
        assertThrows(com.example.demologin.exception.exceptions.BadRequestException.class, () -> service.deleteEvent(99L, null, "id"));
    }

    @Test
    void getEventsWithToken_throws_bad_request_on_insufficient_scope() {
        String token = "tkn";
        String urlContains = "/calendars/primary/events";
        org.springframework.web.client.HttpClientErrorException forbidden = org.springframework.web.client.HttpClientErrorException.create(
                org.springframework.http.HttpStatus.FORBIDDEN,
                "Forbidden",
                org.springframework.http.HttpHeaders.EMPTY,
                ("{\n  \"error\": {\n    \"code\": 403,\n    \"message\": \"Request had insufficient authentication scopes.\",\n    \"errors\": [ { \"message\": \"Insufficient Permission\", \"domain\": \"global\", \"reason\": \"insufficientPermissions\" } ],\n    \"status\": \"PERMISSION_DENIED\",\n    \"details\": [{\"@type\": \"type.googleapis.com/google.rpc.ErrorInfo\",\"reason\": \"ACCESS_TOKEN_SCOPE_INSUFFICIENT\"}]\n  }\n}" ).getBytes(), null);

        when(restTemplate.exchange(contains(urlContains), eq(org.springframework.http.HttpMethod.GET), any(), eq(Map.class))).thenThrow(forbidden);
        com.example.demologin.exception.exceptions.ForbiddenException ex = assertThrows(com.example.demologin.exception.exceptions.ForbiddenException.class, () -> service.getEventsWithToken(token, null, null, null));
        assertTrue(ex.getMessage().contains("missing required calendar scope"));
    }

    @Test
    void validateTokenHasCalendarScope_passes_when_scope_present() {
        String token = "tkn";
        Map<String, Object> body = Map.of("scope", "openid email https://www.googleapis.com/auth/calendar");
        when(restTemplate.getForEntity(contains("tokeninfo"), eq(Map.class))).thenReturn(new ResponseEntity<>(body, HttpStatus.OK));

        service.validateTokenHasCalendarScope(token); // should not throw
    }

    @Test
    void validateTokenHasCalendarScope_throws_forbidden_when_scope_missing() {
        String token = "tkn";
        Map<String, Object> body = Map.of("scope", "openid email");
        when(restTemplate.getForEntity(contains("tokeninfo"), eq(Map.class))).thenReturn(new ResponseEntity<>(body, HttpStatus.OK));

        com.example.demologin.exception.exceptions.ForbiddenException ex = assertThrows(com.example.demologin.exception.exceptions.ForbiddenException.class, () -> service.validateTokenHasCalendarScope(token));
        assertTrue(ex.getMessage().contains("missing required calendar scope"));
    }

    @Test
    void validateTokenHasCalendarScope_throws_invalid_token_on_client_error() {
        String token = "tkn";
        org.springframework.web.client.HttpClientErrorException bad = org.springframework.web.client.HttpClientErrorException.create(
                org.springframework.http.HttpStatus.BAD_REQUEST,
                "Bad Request",
                org.springframework.http.HttpHeaders.EMPTY,
                "{".getBytes(), null);
        when(restTemplate.getForEntity(contains("tokeninfo"), eq(Map.class))).thenThrow(bad);

        assertThrows(com.example.demologin.exception.exceptions.InvalidTokenException.class, () -> service.validateTokenHasCalendarScope(token));
    }
}
