package com.example.demologin.config;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.example.demologin.dto.response.LoginResponse;
import com.example.demologin.entity.User;
import com.example.demologin.enums.ActivityType;
import com.example.demologin.repository.UserRepository;
import com.example.demologin.service.AuthenticationService;
import com.example.demologin.service.UserActivityLogService;
import com.example.demologin.utils.UserAgentUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CustomOAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger logger = LoggerFactory.getLogger(CustomOAuth2SuccessHandler.class);
    // For debugging: store the most recent redirect URL and whether it was mobile
    private static volatile String lastRedirectUrl = null;
    private static volatile boolean lastRedirectIsMobile = false;

    public static String getLastRedirectUrl() {
        return lastRedirectUrl;
    }

    public static boolean getLastRedirectIsMobile() {
        return lastRedirectIsMobile;
    }


    private final AuthenticationService authenticationService;


    private final UserActivityLogService userActivityLogService;

    private final UserRepository userRepository;
    private final OAuth2AuthorizedClientService authorizedClientService;
    
    @Lazy
    private final com.example.demologin.service.GoogleCalendarService googleCalendarService;

    @Value("${frontend.url.base}")
    private String frontendUrl;
    
    @Value("${frontend.url.mobile}")
    private String frontendMobileUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        
        if (email == null) {
            response.sendRedirect(frontendUrl + "login?error=missing_email");
            return;
        }
        
        boolean isMobile = false;
        try {
            String incomingState = request.getParameter("state");
            logger.info("OAuth2 callback received state={}", incomingState);
            // If the OAuth flow was initiated via the mobile initiation endpoint, a session
            // attribute 'oauth2_mobile' will be set. Read and clear it here.
            if (request.getSession(false) != null) {
                Object mobileAttr = request.getSession(false).getAttribute("oauth2_mobile");
                isMobile = Boolean.TRUE.equals(mobileAttr);
                if (isMobile) {
                    request.getSession(false).removeAttribute("oauth2_mobile");
                    logger.info("Detected mobile OAuth via session attribute for {}", email);
                }
            }

            boolean isCalendar = false;
            
            if (request.getCookies() != null) {
                for (Cookie c : request.getCookies()) {
                    if ("oauth2_mobile".equals(c.getName()) && "true".equalsIgnoreCase(c.getValue())) {
                        isMobile = true;
                        // clear cookie by setting maxAge=0
                        Cookie clear = new Cookie("oauth2_mobile", "");
                        clear.setPath("/");
                        clear.setMaxAge(0);
                        response.addCookie(clear);
                        logger.info("Detected mobile OAuth via cookie for {}", email);
                    }
                    if ("oauth2_calendar".equals(c.getName()) && "true".equalsIgnoreCase(c.getValue())) {
                        isCalendar = true;
                        // clear cookie by setting maxAge=0
                        Cookie clearCal = new Cookie("oauth2_calendar", "");
                        clearCal.setPath("/");
                        clearCal.setMaxAge(0);
                        response.addCookie(clearCal);
                        logger.info("Detected calendar OAuth via cookie for {}", email);
                    }
                }
            }

            // Also inspect OAuth2 state — the custom resolver appends ::m when mobile=true and ::c when calendar=true
            String state = request.getParameter("state");
            if (state != null) {
                if (!isCalendar && state.contains("::c")) {
                    isCalendar = true;
                    logger.info("Detected calendar OAuth flow via state parameter for {} (state={})", email, state);
                }
                // Check contains instead of endsWith since state can be ::m::c
                if (!isMobile && state.contains("::m")) {
                    isMobile = true;
                    logger.info("Detected mobile OAuth via state parameter for {} (state={})", email, state);
                }
            }

            // Fallback: detect mobile via User-Agent header if detection above failed
            if (!isMobile) {
                try {
                    String ua = request.getHeader("User-Agent");
                    if (ua != null) {
                        UserAgentUtil.DeviceInfo info = UserAgentUtil.parseUserAgent(ua);
                        String deviceType = info.getDeviceType();
                        if ("Mobile".equalsIgnoreCase(deviceType) || "Tablet".equalsIgnoreCase(deviceType)) {
                            isMobile = true;
                            logger.info("Detected mobile OAuth via User-Agent for {} (deviceType={})", email, deviceType);
                        }
                    }
                } catch (Exception e) {
                    logger.debug("User-Agent parsing fallback failed: {}", e.getMessage());
                }
            }

            LoginResponse userResponse = authenticationService.getUserResponse(email, name);
            
            // Log successful OAuth2 login
            User user = userRepository.findByEmail(email).orElse(null);
            userActivityLogService.logUserActivity(user, ActivityType.LOGIN_ATTEMPT, 
                "OAuth2 login successful via " + getProviderName(authentication));

            // Persist Google OAuth tokens if available
            try {
                if (authentication instanceof OAuth2AuthenticationToken && user != null) {
                    OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
                    String registrationId = oauthToken.getAuthorizedClientRegistrationId();
                    OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(registrationId, oauthToken.getName());
                    if (client != null && client.getAccessToken() != null) {
                        String accessToken = client.getAccessToken().getTokenValue();
                        String refreshToken = client.getRefreshToken() == null ? null : client.getRefreshToken().getTokenValue();
                        java.time.Instant expiresAt = client.getAccessToken().getExpiresAt();
                        java.util.Set<String> scopes = client.getAccessToken().getScopes();
                        googleCalendarService.saveCredentialFromAuthorizedClient(user.getUserId(), accessToken, refreshToken, scopes, expiresAt);
                        logger.info("Saved Google credential for user={}", user.getUserId());
                    }
                }
            } catch (Exception e) {
                logger.warn("Failed to persist OAuth credentials: {}", e.getMessage());
            }
            
                String base = isMobile ? frontendMobileUrl : frontendUrl;
                // If calendar flow, redirect to calendar page instead of home
                String page = (isMobile && isCalendar) ? "calendar" : "";
                String redirectUrl = base + page + (page.isEmpty() ? "" : "?") + "token=" + userResponse.getToken() + "&refreshToken="
                    + userResponse.getRefreshToken();
                logger.info("OAuth2 success for {} (mobile={}, calendar={}): redirecting to {}", email, isMobile, isCalendar, redirectUrl);
                // record for debugging
                lastRedirectUrl = redirectUrl;
                lastRedirectIsMobile = isMobile;
                response.sendRedirect(redirectUrl);
            
        } catch (Exception e) {
            // Log failed OAuth2 login attempt  
            User user = null;
            try {
                user = userRepository.findByEmail(email).orElse(null);
            } catch (Exception ignored) {
                // User might not exist
            }
            
            userActivityLogService.logUserActivity(user, ActivityType.LOGIN_ATTEMPT, 
                "OAuth2 login failed: " + e.getMessage());
            
                String base = isMobile ? frontendMobileUrl : frontendUrl;
                String errorMessage = e.getMessage() == null ? "unknown_error" : e.getMessage().replace(" ", "_");
                logger.info("OAuth2 failure redirect (mobile={}) to {}error={}", isMobile, base, errorMessage);
                response.sendRedirect(base + "error=" + errorMessage);
        }
    }
    
    private String getProviderName(Authentication authentication) {
        // Extract provider name from OAuth2 authentication
        if (authentication != null && authentication.getName() != null) {
            String authName = authentication.getName().toLowerCase();
            if (authName.contains("google")) {
                return "Google";
            } else if (authName.contains("facebook")) {
                return "Facebook";
            }
        }
        return "OAuth2 Provider";
    }
}