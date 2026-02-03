package com.example.demologin.service;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demologin.entity.CalendarCredential;
import com.example.demologin.repository.CalendarCredentialRepository;

import lombok.RequiredArgsConstructor;

/**
 * Implementation of {@link GoogleCalendarService}.
 */
@Service
@RequiredArgsConstructor
public class GoogleCalendarServiceImpl implements GoogleCalendarService {

    private static final Logger log = LoggerFactory.getLogger(GoogleCalendarServiceImpl.class);

    private final CalendarCredentialRepository credentialRepository;
    private final org.springframework.web.client.RestTemplate restTemplate;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String googleClientSecret;

    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String CALENDAR_EVENTS_URL = "https://www.googleapis.com/calendar/v3/calendars/%s/events";
    private static final String TOKENINFO_URL = "https://www.googleapis.com/oauth2/v3/tokeninfo?access_token=%s";

    @Override
    public Map createEvent(Long userId, String calendarId, com.example.demologin.dto.request.CalendarEventRequest req) {
        CalendarCredential cred = credentialRepository.findByUserIdAndProvider(userId, "google").orElse(null);
        if (cred == null) throw new com.example.demologin.exception.exceptions.BadRequestException("Google account not linked");
        cred = refreshIfNeeded(cred);

        String url = String.format(CALENDAR_EVENTS_URL, calendarId == null ? "primary" : calendarId);
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("summary", req.getSummary());
        payload.put("description", req.getDescription());
        if (req.getLocation() != null) payload.put("location", req.getLocation());
        if (req.getStartDateTime() != null) {
            Map<String, Object> start = new java.util.HashMap<>();
            start.put("dateTime", req.getStartDateTime().toString());
            if (req.getTimeZone() != null) start.put("timeZone", req.getTimeZone());
            payload.put("start", start);
        }
        if (req.getEndDateTime() != null) {
            Map<String, Object> end = new java.util.HashMap<>();
            end.put("dateTime", req.getEndDateTime().toString());
            if (req.getTimeZone() != null) end.put("timeZone", req.getTimeZone());
            payload.put("end", end);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(cred.getAccessToken());
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
        ResponseEntity<Map> resp = restTemplate.postForEntity(url, entity, Map.class);
        return resp.getBody();
    }

    @Override
    public Map createEventWithToken(String accessToken, String calendarId, com.example.demologin.dto.request.CalendarEventRequest req) {
        String url = String.format(CALENDAR_EVENTS_URL, calendarId == null ? "primary" : calendarId);
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("summary", req.getSummary());
        payload.put("description", req.getDescription());
        if (req.getLocation() != null) payload.put("location", req.getLocation());
        if (req.getStartDateTime() != null) {
            Map<String, Object> start = new java.util.HashMap<>();
            start.put("dateTime", req.getStartDateTime().toString());
            if (req.getTimeZone() != null) start.put("timeZone", req.getTimeZone());
            payload.put("start", start);
        }
        if (req.getEndDateTime() != null) {
            Map<String, Object> end = new java.util.HashMap<>();
            end.put("dateTime", req.getEndDateTime().toString());
            if (req.getTimeZone() != null) end.put("timeZone", req.getTimeZone());
            payload.put("end", end);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
        try {
            ResponseEntity<Map> resp = restTemplate.postForEntity(url, entity, Map.class);
            return resp.getBody();
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            if (e.getStatusCode() == org.springframework.http.HttpStatus.FORBIDDEN && e.getResponseBodyAsString() != null && e.getResponseBodyAsString().contains("insufficientPermissions")) {
                throw new com.example.demologin.exception.exceptions.ForbiddenException("Google access token missing required calendar scope: https://www.googleapis.com/auth/calendar");
            }
            throw new com.example.demologin.exception.exceptions.BadRequestException("Google API error: " + e.getMessage());
        }
    }

    @Override
    public Map updateEvent(Long userId, String calendarId, String eventId, com.example.demologin.dto.request.CalendarEventRequest req) {
        CalendarCredential cred = credentialRepository.findByUserIdAndProvider(userId, "google").orElse(null);
        if (cred == null) throw new com.example.demologin.exception.exceptions.BadRequestException("Google account not linked");
        cred = refreshIfNeeded(cred);

        String url = String.format(CALENDAR_EVENTS_URL + "/%s", calendarId == null ? "primary" : calendarId, eventId);
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("summary", req.getSummary());
        payload.put("description", req.getDescription());
        if (req.getLocation() != null) payload.put("location", req.getLocation());
        if (req.getStartDateTime() != null) {
            Map<String, Object> start = new java.util.HashMap<>();
            start.put("dateTime", req.getStartDateTime().toString());
            if (req.getTimeZone() != null) start.put("timeZone", req.getTimeZone());
            payload.put("start", start);
        }
        if (req.getEndDateTime() != null) {
            Map<String, Object> end = new java.util.HashMap<>();
            end.put("dateTime", req.getEndDateTime().toString());
            if (req.getTimeZone() != null) end.put("timeZone", req.getTimeZone());
            payload.put("end", end);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(cred.getAccessToken());
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
        ResponseEntity<Map> resp = restTemplate.exchange(url, org.springframework.http.HttpMethod.PATCH, entity, Map.class);
        return resp.getBody();
    }

    @Override
    public Map updateEventWithToken(String accessToken, String calendarId, String eventId, com.example.demologin.dto.request.CalendarEventRequest req) {
        String url = String.format(CALENDAR_EVENTS_URL + "/%s", calendarId == null ? "primary" : calendarId, eventId);
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("summary", req.getSummary());
        payload.put("description", req.getDescription());
        if (req.getLocation() != null) payload.put("location", req.getLocation());
        if (req.getStartDateTime() != null) {
            Map<String, Object> start = new java.util.HashMap<>();
            start.put("dateTime", req.getStartDateTime().toString());
            if (req.getTimeZone() != null) start.put("timeZone", req.getTimeZone());
            payload.put("start", start);
        }
        if (req.getEndDateTime() != null) {
            Map<String, Object> end = new java.util.HashMap<>();
            end.put("dateTime", req.getEndDateTime().toString());
            if (req.getTimeZone() != null) end.put("timeZone", req.getTimeZone());
            payload.put("end", end);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
        try {
            ResponseEntity<Map> resp = restTemplate.exchange(url, org.springframework.http.HttpMethod.PATCH, entity, Map.class);
            return resp.getBody();
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            if (e.getStatusCode() == org.springframework.http.HttpStatus.FORBIDDEN && e.getResponseBodyAsString() != null && e.getResponseBodyAsString().contains("insufficientPermissions")) {
                throw new com.example.demologin.exception.exceptions.ForbiddenException("Google access token missing required calendar scope: https://www.googleapis.com/auth/calendar");
            }
            throw new com.example.demologin.exception.exceptions.BadRequestException("Google API error: " + e.getMessage());
        }
    }

    @Override
    public void deleteEvent(Long userId, String calendarId, String eventId) {
        CalendarCredential cred = credentialRepository.findByUserIdAndProvider(userId, "google").orElse(null);
        if (cred == null) throw new com.example.demologin.exception.exceptions.BadRequestException("Google account not linked");
        cred = refreshIfNeeded(cred);

        String url = String.format(CALENDAR_EVENTS_URL + "/%s", calendarId == null ? "primary" : calendarId, eventId);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(cred.getAccessToken());
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        restTemplate.exchange(url, org.springframework.http.HttpMethod.DELETE, entity, Void.class);
    }

    @Override
    public void deleteEventWithToken(String accessToken, String calendarId, String eventId) {
        String url = String.format(CALENDAR_EVENTS_URL + "/%s", calendarId == null ? "primary" : calendarId, eventId);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        try {
            restTemplate.exchange(url, org.springframework.http.HttpMethod.DELETE, entity, Void.class);
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            if (e.getStatusCode() == org.springframework.http.HttpStatus.FORBIDDEN && e.getResponseBodyAsString() != null && e.getResponseBodyAsString().contains("insufficientPermissions")) {
                throw new com.example.demologin.exception.exceptions.BadRequestException("Google access token missing required calendar scope: https://www.googleapis.com/auth/calendar");
            }
            throw new com.example.demologin.exception.exceptions.BadRequestException("Google API error: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void saveCredentialFromAuthorizedClient(Long userId, String accessToken, String refreshToken, Set<String> scopes, Instant expiresAt) {
        CalendarCredential cred = credentialRepository.findByUserIdAndProvider(userId, "google")
                .orElse(CalendarCredential.builder().userId(userId).provider("google").createdAt(Instant.now()).build());
        cred.setAccessToken(accessToken);
        if (refreshToken != null && !refreshToken.isEmpty()) cred.setRefreshToken(refreshToken);
        cred.setScopes(String.join(" ", scopes == null ? Collections.emptySet() : scopes));
        cred.setExpiresAt(expiresAt);
        cred.setUpdatedAt(Instant.now());
        credentialRepository.save(cred);
    }

    @Override
    public Optional<CalendarCredential> findCredential(Long userId) {
        return credentialRepository.findByUserIdAndProvider(userId, "google");
    }

    @Override
    @Transactional
    public CalendarCredential refreshIfNeeded(CalendarCredential cred) {
        if (cred.getRefreshToken() == null) return cred;
        if (cred.getExpiresAt() == null || cred.getExpiresAt().isBefore(Instant.now().minusSeconds(60))) {
            // suppressed informational logs per request
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            Map<String, String> body = new LinkedHashMap<>();
            body.put("client_id", googleClientId);
            body.put("client_secret", googleClientSecret);
            body.put("grant_type", "refresh_token");
            body.put("refresh_token", cred.getRefreshToken());
            HttpEntity<Map<String, String>> req = new HttpEntity<>(body, headers);

            ResponseEntity<Map> resp = restTemplate.postForEntity(TOKEN_URL, req, Map.class);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                Map b = resp.getBody();
                String newAccess = (String) b.get("access_token");
                Integer expiresIn = b.get("expires_in") == null ? null : ((Number) b.get("expires_in")).intValue();
                if (newAccess != null) {
                    cred.setAccessToken(newAccess);
                    if (expiresIn != null) cred.setExpiresAt(Instant.now().plusSeconds(expiresIn));
                    cred.setUpdatedAt(Instant.now());
                    credentialRepository.save(cred);
                }
            } else {
                // intentionally silent on refresh failures to avoid noisy logs
            }
        }
        return cred;
    }

    @Override
    public Map getEvents(Long userId, String calendarId, Instant timeMin, Instant timeMax) {
        CalendarCredential cred = credentialRepository.findByUserIdAndProvider(userId, "google").orElse(null);
        if (cred == null) throw new com.example.demologin.exception.exceptions.BadRequestException("Google account not linked");
        cred = refreshIfNeeded(cred);

        String url = String.format(CALENDAR_EVENTS_URL, calendarId == null ? "primary" : calendarId);
        StringBuilder sb = new StringBuilder(url + "?singleEvents=true&orderBy=startTime");
        if (timeMin != null) sb.append("&timeMin=").append(timeMin.toString());
        if (timeMax != null) sb.append("&timeMax=").append(timeMax.toString());

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(cred.getAccessToken());
        HttpEntity<Void> req = new HttpEntity<>(headers);
        ResponseEntity<Map> resp = restTemplate.exchange(sb.toString(), HttpMethod.GET, req, Map.class);
        return resp.getBody();
    }

    @Override
    public Map getEventsWithToken(String accessToken, String calendarId, Instant timeMin, Instant timeMax) {
        String url = String.format(CALENDAR_EVENTS_URL, calendarId == null ? "primary" : calendarId);
        StringBuilder sb = new StringBuilder(url + "?singleEvents=true&orderBy=startTime");
        if (timeMin != null) sb.append("&timeMin=").append(timeMin.toString());
        if (timeMax != null) sb.append("&timeMax=").append(timeMax.toString());

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<Void> req = new HttpEntity<>(headers);
        try {
            ResponseEntity<Map> resp = restTemplate.exchange(sb.toString(), HttpMethod.GET, req, Map.class);
            return resp.getBody();
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            // Translate Google insufficient-scope errors into a clear BadRequestException
            if (e.getStatusCode() == org.springframework.http.HttpStatus.FORBIDDEN) {
                String body = e.getResponseBodyAsString();
                if (body != null && (body.contains("ACCESS_TOKEN_SCOPE_INSUFFICIENT") || body.contains("insufficientPermissions") || body.contains("insufficient authentication scopes"))) {
                    throw new com.example.demologin.exception.exceptions.ForbiddenException("Google access token missing required calendar scope: https://www.googleapis.com/auth/calendar");
                }
            }
            // fallback: rethrow as BadRequest to map to 400 with message
            throw new com.example.demologin.exception.exceptions.BadRequestException("Google API error: " + e.getMessage());
        }
    }

    @Override
    public void validateTokenHasCalendarScope(String accessToken) {
        String url = String.format(TOKENINFO_URL, java.net.URLEncoder.encode(accessToken, java.nio.charset.StandardCharsets.UTF_8));
        try {
            ResponseEntity<Map> resp = restTemplate.getForEntity(url, Map.class);
            Map body = resp.getBody();
            String scopes = body == null ? null : (String) body.get("scope");
            if (scopes == null || !(scopes.contains("https://www.googleapis.com/auth/calendar") || scopes.contains("calendar.events"))) {
                throw new com.example.demologin.exception.exceptions.ForbiddenException("Google access token missing required calendar scope: https://www.googleapis.com/auth/calendar");
            }
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            // Invalid token or other client error
            throw new com.example.demologin.exception.exceptions.InvalidTokenException("Invalid Google access token");
        } catch (com.example.demologin.exception.exceptions.ForbiddenException e) {
            // Re-throw ForbiddenException as-is
            throw e;
        } catch (Exception e) {
            throw new com.example.demologin.exception.exceptions.BadRequestException("Google token validation error: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getAccessToken(Long userId) {
        CalendarCredential cred = credentialRepository.findByUserIdAndProvider(userId, "google").orElse(null);
        if (cred == null) throw new com.example.demologin.exception.exceptions.BadRequestException("Google account not linked");
        cred = refreshIfNeeded(cred);

        Map<String, Object> resp = new HashMap<>();
        resp.put("accessToken", cred.getAccessToken());
        resp.put("expiresAt", cred.getExpiresAt());
        // intentionally silent on token issuance
        return resp;
    }

    @Override
    @Transactional
    public void deleteCredential(Long userId) {
        credentialRepository.findByUserIdAndProvider(userId, "google").ifPresent(cred -> {
            credentialRepository.delete(cred);
            log.info("Deleted Google Calendar credential for user={}", userId);
        });
    }
}
