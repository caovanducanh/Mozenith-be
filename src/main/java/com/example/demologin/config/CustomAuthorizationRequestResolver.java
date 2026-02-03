package com.example.demologin.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import com.example.demologin.utils.UserAgentUtil;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Wraps the default resolver and encodes a mobile flag into the state if the
 * original request contains `mobile=true`.
 */
public class CustomAuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    private final OAuth2AuthorizationRequestResolver delegate;
    private static final Logger logger = LoggerFactory.getLogger(CustomAuthorizationRequestResolver.class);

    public CustomAuthorizationRequestResolver(ClientRegistrationRepository repo, String authorizationRequestBaseUri) {
        this.delegate = new DefaultOAuth2AuthorizationRequestResolver(repo, authorizationRequestBaseUri);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        OAuth2AuthorizationRequest req = delegate.resolve(request);
        return maybeTagMobile(request, req);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        OAuth2AuthorizationRequest req = delegate.resolve(request, clientRegistrationId);
        return maybeTagMobile(request, req);
    }

    OAuth2AuthorizationRequest maybeTagMobile(HttpServletRequest request, OAuth2AuthorizationRequest original) {
        if (original == null) return null;
        
        // Check for calendar via param, cookie, OR session attribute
        String calendarParam = request.getParameter("calendar");
        boolean isCalendarFromParam = calendarParam != null && (calendarParam.equalsIgnoreCase("true") || calendarParam.equals("1"));
        boolean isCalendarFromCookie = false;
        if (request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie c : request.getCookies()) {
                if ("oauth2_calendar".equals(c.getName()) && "true".equalsIgnoreCase(c.getValue())) {
                    isCalendarFromCookie = true;
                    break;
                }
            }
        }
        // Also check session attribute (set by /mobi/oauth2/authorization/google/calendar)
        boolean isCalendarFromSession = false;
        if (request.getSession(false) != null) {
            Object calAttr = request.getSession(false).getAttribute("oauth2_calendar");
            isCalendarFromSession = Boolean.TRUE.equals(calAttr);
        }
        boolean isCalendar = isCalendarFromParam || isCalendarFromCookie || isCalendarFromSession;
        
        String mobile = request.getParameter("mobile");
        if (mobile != null && (mobile.equalsIgnoreCase("true") || mobile.equals("1"))) {
                // Append ::m for mobile, and ::c for calendar
                String newState = original.getState() + "::m";
                if (isCalendar) {
                    newState = newState + "::c";
                }
                
                logger.info("Tagging OAuth2 state as mobile (calendar={}, fromParam={}, fromCookie={}, fromSession={}) (state={} -> {}) for request {}", isCalendar, isCalendarFromParam, isCalendarFromCookie, isCalendarFromSession, original.getState(), newState, request.getRequestURI());
                // preserve existing additional parameters and add offline access + consent to ensure refresh token
                java.util.Map<String, Object> additional = new java.util.HashMap<>(original.getAdditionalParameters());
                additional.put("prompt", "consent");
                additional.put("access_type", "offline");
                // allow callers to request calendar scope explicitly via ?scope=calendar or ?calendar=true
                java.util.Set<String> scopes = new java.util.HashSet<>(original.getScopes());
                String requestedScopes = request.getParameter("scope");
                if ((requestedScopes != null && requestedScopes.contains("calendar")) || isCalendar) {
                    scopes.add("https://www.googleapis.com/auth/calendar");
                }
                return OAuth2AuthorizationRequest.from(original)
                    .state(newState)
                    .additionalParameters(additional)
                    .scopes(scopes)
                    .build();
        }

        // Fallback: try to detect mobile via User-Agent if no explicit mobile param
        String ua = request.getHeader("User-Agent");
        if (ua != null) {
            try {
                UserAgentUtil.DeviceInfo info = UserAgentUtil.parseUserAgent(ua);
                String deviceType = info.getDeviceType();
                if ("Mobile".equalsIgnoreCase(deviceType) || "Tablet".equalsIgnoreCase(deviceType)) {
                        // Append ::m for mobile, and ::c for calendar (isCalendar already computed above from param/cookie)
                        String newState = original.getState() + "::m";
                        if (isCalendar) {
                            newState = newState + "::c";
                        }
                        
                        logger.info("Tagging OAuth2 state as mobile via User-Agent (deviceType={}, calendar={}) (state={} -> {}) for request {}", deviceType, isCalendar, original.getState(), newState, request.getRequestURI());
                        java.util.Map<String, Object> additional = new java.util.HashMap<>(original.getAdditionalParameters());
                        additional.put("prompt", "consent");
                        additional.put("access_type", "offline");
                        // Only add calendar scope when explicitly requested via ?calendar=true param or cookie
                        java.util.Set<String> scopes = new java.util.HashSet<>(original.getScopes());
                        if (isCalendar) {
                            scopes.add("https://www.googleapis.com/auth/calendar");
                        }
                        return OAuth2AuthorizationRequest.from(original)
                            .state(newState)
                            .additionalParameters(additional)
                            .scopes(scopes)
                            .build();
                }
            } catch (Exception e) {
                // If parsing fails, don't block the flow — just continue returning original
                logger.debug("User-Agent parsing failed: {}", e.getMessage());
            }
        }
        return original;
    }
}
