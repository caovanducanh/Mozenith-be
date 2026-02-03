package com.example.demologin.controller;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.example.demologin.annotation.SecuredEndpoint;
import com.example.demologin.exception.exceptions.BadRequestException;
import com.example.demologin.service.GoogleCalendarOAuthStateService;
import com.example.demologin.service.GoogleCalendarService;
import com.example.demologin.utils.AccountUtils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * Dedicated controller for Google Calendar OAuth2 Authorization.
 * 
 * This is SEPARATE from Google Login - follows the architecture principle:
 * - Google Login = identity verification (profile, email)
 * - Google Calendar OAuth = authorization to access Calendar API (calendar scope + refresh_token)
 * 
 * Flow:
 * 1. User calls POST /api/calendar/link → triggers OAuth for Calendar scope
 * 2. User is redirected to Google consent screen with calendar scopes
 * 3. Google redirects back to /oauth2/google/calendar/callback with authorization code
 * 4. Backend exchanges code for tokens, stores in calendar_credential table
 * 5. Mobile receives deep-link with success/failure status
 */
@RestController
@RequestMapping("/oauth2/google/calendar")
@RequiredArgsConstructor
public class GoogleCalendarOAuthController {

    private static final Logger log = LoggerFactory.getLogger(GoogleCalendarOAuthController.class);

    private final GoogleCalendarService googleCalendarService;
    private final GoogleCalendarOAuthStateService stateService;
    private final AccountUtils accountUtils;
    private final RestTemplate restTemplate;
    private final com.example.demologin.utils.JwtUtil jwtUtil;
    private final com.example.demologin.repository.UserRepository userRepository;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String googleClientSecret;

    @Value("${google.calendar.oauth.redirect-uri:#{null}}")
    private String configuredRedirectUri;

    @Value("${frontend.url.mobile}")
    private String frontendMobileUrl;

    @Value("${frontend.url.base}")
    private String frontendBaseUrl;

    // Google OAuth2 URLs
    private static final String GOOGLE_AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";

    // Calendar scopes - these are DIFFERENT from login scopes
    private static final String CALENDAR_SCOPE = "https://www.googleapis.com/auth/calendar";
    private static final String CALENDAR_EVENTS_SCOPE = "https://www.googleapis.com/auth/calendar.events";

    /**
     * Initiate Google Calendar authorization for the current user.
     * 
     * This endpoint is called when a logged-in user wants to link their Google Calendar.
     * The user MUST be authenticated first (via normal login).
     * 
     * @param mobile whether this is a mobile flow (returns deep-link)
     */
    @GetMapping("/authorize")
    @SecuredEndpoint("CALENDAR_READ")
    public void authorizeCalendar(
            HttpServletRequest request,
            HttpServletResponse response,
            @RequestParam(required = false, defaultValue = "false") boolean mobile
    ) throws IOException {
        Long userId = accountUtils.getCurrentUser().getUserId();
        
        // Generate unique state to prevent CSRF and track the user
        String state = UUID.randomUUID().toString();
        
        // Store state with user info (will be validated in callback)
        stateService.saveState(state, userId, mobile);
        
        // Build the redirect URI dynamically based on request if not configured
        String redirectUri = getRedirectUri(request);

        // Build Google OAuth authorization URL with Calendar scopes
        // CRITICAL: access_type=offline and prompt=consent are REQUIRED for refresh_token
        StringBuilder authUrl = new StringBuilder(GOOGLE_AUTH_URL);
        authUrl.append("?client_id=").append(URLEncoder.encode(googleClientId, StandardCharsets.UTF_8));
        authUrl.append("&redirect_uri=").append(URLEncoder.encode(redirectUri, StandardCharsets.UTF_8));
        authUrl.append("&response_type=code");
        authUrl.append("&scope=").append(URLEncoder.encode(CALENDAR_SCOPE + " " + CALENDAR_EVENTS_SCOPE, StandardCharsets.UTF_8));
        authUrl.append("&access_type=offline");  // REQUIRED for refresh_token
        authUrl.append("&prompt=consent");        // REQUIRED to force consent and get refresh_token
        authUrl.append("&state=").append(URLEncoder.encode(state, StandardCharsets.UTF_8));

        log.info("🗓️ Initiating Google Calendar OAuth for user={} (mobile={}, state={})", userId, mobile, state);
        
        response.sendRedirect(authUrl.toString());
    }

    /**
     * Mobile-specific endpoint for initiating Google Calendar authorization.
     * 
     * Since mobile browsers/WebViews cannot set Authorization headers when opening URLs,
     * this endpoint accepts the JWT token as a query parameter.
     * 
     * Usage: GET /oauth2/google/calendar/authorize/mobile?token=<jwt_token>
     * 
     * The endpoint validates the JWT, extracts the user, and proceeds with the OAuth flow.
     */
    @GetMapping("/authorize/mobile")
    public void authorizeCalendarMobile(
            HttpServletRequest request,
            HttpServletResponse response,
            @RequestParam(name = "token") String jwtToken
    ) throws IOException {
        // URL decode token in case it was double-encoded
        // JWT tokens may contain +, /, = which get URL encoded
        String decodedToken = jwtToken;
        try {
            // Check if token looks URL-encoded (contains % followed by hex digits)
            if (jwtToken != null && jwtToken.contains("%")) {
                decodedToken = java.net.URLDecoder.decode(jwtToken, StandardCharsets.UTF_8);
                log.debug("Token was URL-encoded, decoded it");
            }
        } catch (Exception e) {
            log.debug("Token decode failed, using original: {}", e.getMessage());
        }
        
        // Log received token for debugging (first 50 chars only for security)
        String tokenPreview = decodedToken != null && decodedToken.length() > 50 
            ? decodedToken.substring(0, 50) + "..." 
            : decodedToken;
        log.info("🗓️ Calendar OAuth mobile request received, token preview: {}", tokenPreview);
        
        // Validate JWT token and extract user
        Long userId;
        try {
            // Validate token structure first
            if (!jwtUtil.validateTokenStructure(decodedToken)) {
                log.warn("❌ Invalid JWT token structure for calendar authorize. Token length: {}", 
                    decodedToken != null ? decodedToken.length() : 0);
                handleCallbackRedirect(response, true, false, "Invalid token. Please login again.");
                return;
            }
            
            // Check if token is expired
            if (jwtUtil.isTokenExpired(decodedToken)) {
                log.warn("❌ Expired JWT token for calendar authorize");
                handleCallbackRedirect(response, true, false, "Token expired. Please login again.");
                return;
            }
            
            // Extract user ID from token (stored in subject)
            String userIdStr = jwtUtil.extractUsername(decodedToken);
            userId = Long.parseLong(userIdStr);
            log.info("🗓️ JWT validated successfully for user={}", userId);
            
            // Verify user exists
            if (!userRepository.existsById(userId)) {
                log.warn("❌ User {} not found for calendar authorize", userId);
                handleCallbackRedirect(response, true, false, "User not found. Please login again.");
                return;
            }
        } catch (Exception e) {
            log.error("❌ Error validating JWT for calendar authorize: {}", e.getMessage(), e);
            handleCallbackRedirect(response, true, false, "Authentication failed. Please login again.");
            return;
        }
        
        // Generate unique state to prevent CSRF and track the user
        String state = UUID.randomUUID().toString();
        
        // Store state with user info (will be validated in callback)
        stateService.saveState(state, userId, true); // always mobile
        
        // Build the redirect URI dynamically based on request if not configured
        String redirectUri = getRedirectUri(request);

        // Build Google OAuth authorization URL with Calendar scopes
        // CRITICAL: access_type=offline and prompt=consent are REQUIRED for refresh_token
        StringBuilder authUrl = new StringBuilder(GOOGLE_AUTH_URL);
        authUrl.append("?client_id=").append(URLEncoder.encode(googleClientId, StandardCharsets.UTF_8));
        authUrl.append("&redirect_uri=").append(URLEncoder.encode(redirectUri, StandardCharsets.UTF_8));
        authUrl.append("&response_type=code");
        authUrl.append("&scope=").append(URLEncoder.encode(CALENDAR_SCOPE + " " + CALENDAR_EVENTS_SCOPE, StandardCharsets.UTF_8));
        authUrl.append("&access_type=offline");  // REQUIRED for refresh_token
        authUrl.append("&prompt=consent");        // REQUIRED to force consent and get refresh_token
        authUrl.append("&state=").append(URLEncoder.encode(state, StandardCharsets.UTF_8));

        log.info("🗓️ Initiating Google Calendar OAuth for mobile user={} (state={})", userId, state);
        
        response.sendRedirect(authUrl.toString());
    }

    /**
     * Callback endpoint for Google Calendar OAuth.
     * 
     * Google redirects here after user grants/denies calendar access.
     * This endpoint:
     * 1. Validates the state parameter
     * 2. Exchanges authorization code for tokens
     * 3. Validates that returned tokens have calendar scope
     * 4. Stores tokens in calendar_credential table
     * 5. Redirects user back to app with success/failure
     */
    @GetMapping("/callback")
    public void calendarCallback(
            HttpServletRequest request,
            HttpServletResponse response,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            @RequestParam(required = false, name = "error_description") String errorDescription
    ) throws IOException {
        
        // Handle error from Google (user denied access, etc.)
        if (error != null) {
            log.warn("⚠️ Google Calendar OAuth error: {} - {}", error, errorDescription);
            handleCallbackError(response, state, "Google denied access: " + error);
            return;
        }
        
        // Validate state parameter
        if (state == null || state.isEmpty()) {
            log.error("❌ Missing state parameter in calendar callback");
            handleCallbackError(response, null, "Missing state parameter");
            return;
        }
        
        // Retrieve and validate state
        GoogleCalendarOAuthStateService.OAuthState oauthState = stateService.getAndRemoveState(state);
        if (oauthState == null) {
            log.error("❌ Invalid or expired state: {}", state);
            handleCallbackError(response, state, "Invalid or expired state");
            return;
        }
        
        Long userId = oauthState.getUserId();
        boolean isMobile = oauthState.isMobile();
        
        log.info("🗓️ Google Calendar OAuth callback for user={} (mobile={}, state={})", userId, isMobile, state);
        
        if (code == null || code.isEmpty()) {
            log.error("❌ Missing authorization code for user={}", userId);
            handleCallbackRedirect(response, isMobile, false, "Missing authorization code");
            return;
        }
        
        try {
            // Exchange authorization code for tokens
            String redirectUri = getRedirectUri(request);
            Map<String, Object> tokenResponse = exchangeCodeForTokens(code, redirectUri);
            
            String accessToken = (String) tokenResponse.get("access_token");
            String refreshToken = (String) tokenResponse.get("refresh_token");
            String scope = (String) tokenResponse.get("scope");
            Integer expiresIn = tokenResponse.get("expires_in") == null ? null 
                : ((Number) tokenResponse.get("expires_in")).intValue();
            
            // CRITICAL: Validate that we got calendar scope
            if (scope == null || !scope.contains("calendar")) {
                log.error("❌ Token missing calendar scope for user={}. Got scopes: {}", userId, scope);
                handleCallbackRedirect(response, isMobile, false, 
                    "Google did not grant calendar access. Please try again and allow calendar permissions.");
                return;
            }
            
            // CRITICAL: Check for refresh_token - if missing, user may need to revoke and re-auth
            if (refreshToken == null || refreshToken.isEmpty()) {
                log.warn("⚠️ No refresh_token received for user={}. This may happen if user already granted access before.", userId);
                // Still save the credential, but warn in logs
            }
            
            // Calculate expiry time
            Instant expiresAt = expiresIn != null 
                ? Instant.now().plusSeconds(expiresIn) 
                : Instant.now().plusSeconds(3600);
            
            // Save credential to database (SEPARATE from login token)
            java.util.Set<String> scopes = scope != null 
                ? java.util.Set.of(scope.split(" "))
                : java.util.Collections.emptySet();
            
            googleCalendarService.saveCredentialFromAuthorizedClient(
                userId, 
                accessToken, 
                refreshToken, 
                scopes, 
                expiresAt
            );
            
            log.info("✅ Google Calendar linked successfully for user={} (hasRefreshToken={}, scopes={})", 
                userId, refreshToken != null, scope);
            
            handleCallbackRedirect(response, isMobile, true, null);
            
        } catch (Exception e) {
            log.error("❌ Failed to exchange code for tokens for user={}: {}", userId, e.getMessage(), e);
            handleCallbackRedirect(response, isMobile, false, "Failed to link calendar: " + e.getMessage());
        }
    }

    /**
     * Exchange authorization code for access/refresh tokens.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> exchangeCodeForTokens(String code, String redirectUri) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", googleClientId);
        body.add("client_secret", googleClientSecret);
        body.add("code", code);
        body.add("redirect_uri", redirectUri);
        body.add("grant_type", "authorization_code");
        
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        
        ResponseEntity<Map> response = restTemplate.postForEntity(GOOGLE_TOKEN_URL, request, Map.class);
        
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new BadRequestException("Failed to exchange code for tokens");
        }
        
        return response.getBody();
    }

    /**
     * Build redirect URI dynamically based on request.
     */
    private String getRedirectUri(HttpServletRequest request) {
        if (configuredRedirectUri != null && !configuredRedirectUri.isEmpty()) {
            return configuredRedirectUri;
        }
        
        // Build from request (respecting forwarded headers)
        String scheme = request.getScheme();
        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        if (forwardedProto != null) {
            scheme = forwardedProto;
        }
        
        String host = request.getHeader("X-Forwarded-Host");
        if (host == null) {
            host = request.getServerName();
            int port = request.getServerPort();
            if ((scheme.equals("http") && port != 80) || (scheme.equals("https") && port != 443)) {
                host += ":" + port;
            }
        }
        
        return scheme + "://" + host + "/oauth2/google/calendar/callback";
    }

    /**
     * Handle callback error when we can't determine the state.
     */
    private void handleCallbackError(HttpServletResponse response, String state, String error) throws IOException {
        // Try to determine if mobile from state, default to web
        boolean isMobile = false;
        if (state != null) {
            GoogleCalendarOAuthStateService.OAuthState oauthState = stateService.getAndRemoveState(state);
            if (oauthState != null) {
                isMobile = oauthState.isMobile();
            }
        }
        handleCallbackRedirect(response, isMobile, false, error);
    }

    /**
     * Redirect user back to app after callback (success or failure).
     */
    private void handleCallbackRedirect(HttpServletResponse response, boolean isMobile, boolean success, String error) throws IOException {
        String redirectUrl;
        
        if (isMobile) {
            // Deep-link for mobile app
            // frontendMobileUrl may be "bestie://login?" - we need to use proper calendar deep-link
            // Use bestie://calendar?linked=... format
            String mobileBase = frontendMobileUrl;
            // Remove trailing ? if present
            if (mobileBase.endsWith("?")) {
                mobileBase = mobileBase.substring(0, mobileBase.length() - 1);
            }
            // Replace /login with empty or use calendar path
            if (mobileBase.contains("login")) {
                mobileBase = mobileBase.replace("login", "calendar");
            } else if (!mobileBase.endsWith("calendar")) {
                mobileBase = mobileBase + (mobileBase.endsWith("/") ? "" : "/") + "calendar";
            }
            redirectUrl = mobileBase + "?linked=" + success;
            if (!success && error != null) {
                redirectUrl += "&error=" + URLEncoder.encode(error, StandardCharsets.UTF_8);
            }
        } else {
            // Web URL
            redirectUrl = frontendBaseUrl + "/calendar?linked=" + success;
            if (!success && error != null) {
                redirectUrl += "&error=" + URLEncoder.encode(error, StandardCharsets.UTF_8);
            }
        }
        
        log.info("🔄 Redirecting to {} (mobile={}, success={})", redirectUrl, isMobile, success);
        response.sendRedirect(redirectUrl);
    }

    /**
     * REST endpoint to check if current user has calendar linked.
     * Returns { linked: true/false, email: "...", scopes: "..." }
     */
    @GetMapping("/status")
    @SecuredEndpoint("CALENDAR_READ")
    public ResponseEntity<Map<String, Object>> getCalendarLinkStatus() {
        Long userId = accountUtils.getCurrentUser().getUserId();
        String userEmail = accountUtils.getCurrentUser().getEmail();
        
        java.util.Optional<com.example.demologin.entity.CalendarCredential> opt = googleCalendarService.findCredential(userId);
        
        if (opt.isPresent()) {
            com.example.demologin.entity.CalendarCredential cred = opt.get();
            
            // Check if credential has calendar scope
            String scopes = cred.getScopes();
            boolean hasCalendarScope = scopes != null && scopes.contains("calendar");
            boolean hasRefreshToken = cred.getRefreshToken() != null && !cred.getRefreshToken().isEmpty();
            
            Map<String, Object> resp = new java.util.HashMap<>();
            resp.put("linked", true);
            resp.put("email", userEmail);
            resp.put("scopes", scopes);
            resp.put("hasCalendarScope", hasCalendarScope);
            resp.put("hasRefreshToken", hasRefreshToken);
            resp.put("expiresAt", cred.getExpiresAt());
            
            if (!hasCalendarScope) {
                resp.put("warning", "Credential exists but missing calendar scope. Please re-link your calendar.");
            }
            if (!hasRefreshToken) {
                resp.put("warning", "Missing refresh token. Calendar access may stop working after token expires.");
            }
            
            return ResponseEntity.ok(resp);
        } else {
            return ResponseEntity.ok(Map.of(
                "linked", false,
                "message", "Google Calendar not linked. Use /oauth2/google/calendar/authorize to link."
            ));
        }
    }

    /**
     * REST endpoint to unlink Google Calendar.
     * Removes stored credentials.
     */
    @GetMapping("/unlink")
    @SecuredEndpoint("CALENDAR_DELETE")
    public ResponseEntity<Map<String, Object>> unlinkCalendar() {
        Long userId = accountUtils.getCurrentUser().getUserId();
        
        java.util.Optional<com.example.demologin.entity.CalendarCredential> opt = googleCalendarService.findCredential(userId);
        
        if (opt.isPresent()) {
            // TODO: Also revoke token at Google (optional but recommended)
            // For now, just delete from DB
            googleCalendarService.deleteCredential(userId);
            log.info("🗑️ Unlinked Google Calendar for user={}", userId);
            return ResponseEntity.ok(Map.of("success", true, "message", "Google Calendar unlinked successfully"));
        } else {
            return ResponseEntity.ok(Map.of("success", false, "message", "No calendar linked to unlink"));
        }
    }
}
