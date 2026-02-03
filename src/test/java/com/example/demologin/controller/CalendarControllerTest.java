package com.example.demologin.controller;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.http.ResponseEntity;

import com.example.demologin.dto.request.CalendarEventRequest;
import com.example.demologin.entity.User;
import com.example.demologin.service.GoogleCalendarService;
import com.example.demologin.utils.AccountUtils;

class CalendarControllerTest {

    AccountUtils accountUtils;
    GoogleCalendarService googleCalendarService;
    CalendarController controller;

    @BeforeEach
    void setUp() {
        accountUtils = mock(AccountUtils.class);
        googleCalendarService = mock(GoogleCalendarService.class);
        controller = new CalendarController(accountUtils, googleCalendarService);
    }

    @Test
    void createEvent_calls_service() {
        User u = new User(); u.setUserId(5L);
        when(accountUtils.getCurrentUser()).thenReturn(u);
        CalendarEventRequest req = new CalendarEventRequest();
        req.setSummary("S");
        when(googleCalendarService.createEvent(eq(5L), isNull(), any())).thenReturn(Map.of("id", "evt1"));

        ResponseEntity<Map> resp = controller.createEvent(req, null, null);
        assertEquals(201, resp.getStatusCodeValue());
        assertEquals("evt1", resp.getBody().get("id"));
        verify(googleCalendarService, times(1)).createEvent(eq(5L), isNull(), any());
    }

    @Test
    void createEvent_with_token_calls_serviceWithToken() {
        User u = new User(); u.setUserId(5L);
        when(accountUtils.getCurrentUser()).thenReturn(u);
        CalendarEventRequest req = new CalendarEventRequest();
        req.setSummary("S");
        when(googleCalendarService.createEventWithToken(eq("tkn"), isNull(), any())).thenReturn(Map.of("id", "evt2"));

        ResponseEntity<Map> resp = controller.createEvent(req, null, "tkn");
        assertEquals(201, resp.getStatusCodeValue());
        assertEquals("evt2", resp.getBody().get("id"));
        verify(googleCalendarService, times(1)).createEventWithToken(eq("tkn"), isNull(), any());
    }

    @Test
    void getEvents_with_token_calls_serviceWithToken() {
        User u = new User(); u.setUserId(5L);
        when(accountUtils.getCurrentUser()).thenReturn(u);
        when(googleCalendarService.getEventsWithToken(eq("tkn"), isNull(), isNull(), isNull())).thenReturn(Map.of("items", java.util.List.of()));

        ResponseEntity<Map> resp = controller.getEvents(null, null, null, "tkn");
        assertEquals(200, resp.getStatusCodeValue());
        verify(googleCalendarService, times(1)).getEventsWithToken(eq("tkn"), isNull(), isNull(), isNull());
    }

    @Test
    void getEvents_with_token_insufficient_scope_throws() {
        User u = new User(); u.setUserId(5L);
        when(accountUtils.getCurrentUser()).thenReturn(u);
        org.mockito.Mockito.doThrow(new com.example.demologin.exception.exceptions.ForbiddenException("Google access token missing required calendar scope: https://www.googleapis.com/auth/calendar"))
                .when(googleCalendarService).validateTokenHasCalendarScope(eq("tkn"));

        try {
            controller.getEvents(null, null, null, "tkn");
            fail("Expected ForbiddenException");
        } catch (com.example.demologin.exception.exceptions.ForbiddenException ex) {
            // expected
            assertTrue(ex.getMessage().contains("missing required calendar scope"));
        }
    }

    @Test
    void validateToken_endpoint_returns_true_when_valid() {
        org.mockito.Mockito.doNothing().when(googleCalendarService).validateTokenHasCalendarScope(eq("tkn"));
        ResponseEntity<Map<String, Object>> resp = controller.validateToken("tkn");
        assertEquals(200, resp.getStatusCodeValue());
        assertEquals(true, resp.getBody().get("valid"));
    }

    @Test
    void updateEvent_with_token_calls_serviceWithToken() {
        User u = new User(); u.setUserId(5L);
        when(accountUtils.getCurrentUser()).thenReturn(u);
        CalendarEventRequest req = new CalendarEventRequest();
        when(googleCalendarService.updateEventWithToken(eq("tkn"), isNull(), eq("evt1"), any())).thenReturn(Map.of("id", "evt1"));

        ResponseEntity<Map> resp = controller.updateEvent("evt1", req, null, "tkn");
        assertEquals(200, resp.getStatusCodeValue());
        verify(googleCalendarService, times(1)).updateEventWithToken(eq("tkn"), isNull(), eq("evt1"), any());
    }

    @Test
    void deleteEvent_with_token_calls_serviceWithToken() {
        User u = new User(); u.setUserId(5L);
        when(accountUtils.getCurrentUser()).thenReturn(u);

        controller.deleteEvent("evt1", null, "tkn");
        verify(googleCalendarService, times(1)).deleteEventWithToken(eq("tkn"), isNull(), eq("evt1"));
    }

    @Test
    void getCredential_returns_linked_false_when_missing() {
        User u = new User(); u.setUserId(5L);
        when(accountUtils.getCurrentUser()).thenReturn(u);
        when(googleCalendarService.findCredential(eq(5L))).thenReturn(java.util.Optional.empty());

        ResponseEntity<Map<String, Object>> resp = controller.getCredential();
        assertEquals(200, resp.getStatusCodeValue());
        assertEquals(false, resp.getBody().get("linked"));
        verify(googleCalendarService, times(1)).findCredential(eq(5L));
    }

    @Test
    void getCredential_returns_linked_true_with_metadata() {
        User u = new User(); u.setUserId(5L);
        when(accountUtils.getCurrentUser()).thenReturn(u);
        com.example.demologin.entity.CalendarCredential cred = com.example.demologin.entity.CalendarCredential.builder().userId(5L).scopes("https://www.googleapis.com/auth/calendar").expiresAt(java.time.Instant.now()).build();
        when(googleCalendarService.findCredential(eq(5L))).thenReturn(java.util.Optional.of(cred));

        ResponseEntity<Map<String, Object>> resp = controller.getCredential();
        assertEquals(200, resp.getStatusCodeValue());
        assertEquals(true, resp.getBody().get("linked"));
        assertNotNull(resp.getBody().get("scopes"));
        assertNotNull(resp.getBody().get("expiresAt"));
        verify(googleCalendarService, times(1)).findCredential(eq(5L));
    }

    @Test
    void getCredentialToken_returns_400_when_missing() {
        User u = new User(); u.setUserId(5L);
        when(accountUtils.getCurrentUser()).thenReturn(u);
        when(googleCalendarService.findCredential(eq(5L))).thenReturn(java.util.Optional.empty());

        // getCredentialToken should throw BadRequestException via service
        when(googleCalendarService.getAccessToken(eq(5L))).thenThrow(new com.example.demologin.exception.exceptions.BadRequestException("Google account not linked"));

        try {
            controller.getCredentialToken();
            fail("Expected BadRequestException");
        } catch (com.example.demologin.exception.exceptions.BadRequestException ex) {
            // expected
        }
        verify(googleCalendarService, times(1)).getAccessToken(eq(5L));
    }

    @Test
    void getCredentialToken_returns_token_when_linked() {
        User u = new User(); u.setUserId(5L);
        when(accountUtils.getCurrentUser()).thenReturn(u);
        Map<String, Object> token = Map.of("accessToken", "abc", "expiresAt", java.time.Instant.now());
        when(googleCalendarService.getAccessToken(eq(5L))).thenReturn(token);

        ResponseEntity<Map<String, Object>> resp = controller.getCredentialToken();
        assertEquals(200, resp.getStatusCodeValue());
        assertEquals("abc", resp.getBody().get("accessToken"));
        verify(googleCalendarService, times(1)).getAccessToken(eq(5L));
    }
}
