package com.example.demologin.controller;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Controller
public class MobileOAuth2Controller {

    private static final Logger logger = LoggerFactory.getLogger(MobileOAuth2Controller.class);

    @GetMapping("/mobi/oauth2/authorization/{provider}")
    public void mobileOauthAuthorize(HttpServletRequest request, HttpServletResponse response,
                                     @PathVariable String provider) throws IOException {
        // Mark this session as a mobile-initiated OAuth flow so success handler uses deep-link
        // Use a short-lived cookie so the flag survives the OAuth redirect roundtrip
        // Note: modern browsers / WebViews may require SameSite=None; Secure for cross-site cookies.
        boolean secure = request.isSecure() || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"));

        StringBuilder setCookie = new StringBuilder();
        setCookie.append("oauth2_mobile=true; Path=/; Max-Age=300; HttpOnly=false");
        if (secure) {
            // If connection is secure, mark cookie SameSite=None and Secure so it is sent on the
            // cross-site redirect back from the provider in modern browsers and WebViews.
            setCookie.append("; SameSite=None; Secure");
        } else {
            // Non-secure fallback — use Lax so cookie still participates in top-level GET navigations
            setCookie.append("; SameSite=Lax");
        }

        logger.info("Setting oauth2_mobile cookie (secure={}) and redirecting to provider {}", secure, provider);
        response.addHeader("Set-Cookie", setCookie.toString());

        // Redirect to the default Spring OAuth2 authorization endpoint
        // Also include a mobile flag in the query so our custom resolver will tag the OAuth2 state
        response.sendRedirect("/oauth2/authorization/" + provider + "?mobile=true");
    }

    /**
     * Dedicated endpoint for linking Google Calendar via mobile.
     * Always requests calendar scope and stores credentials in calendar_credential table.
     * Use: GET /mobi/oauth2/authorization/google/calendar
     */
    @GetMapping("/mobi/oauth2/authorization/google/calendar")
    public void mobileOauthCalendar(HttpServletRequest request, HttpServletResponse response) throws IOException {
        logger.info(">>> mobileOauthCalendar endpoint HIT! URI={}, QueryString={}", request.getRequestURI(), request.getQueryString());
        
        boolean secure = request.isSecure() || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"));

        // Set mobile cookie
        StringBuilder mobileCookie = new StringBuilder();
        mobileCookie.append("oauth2_mobile=true; Path=/; Max-Age=300; HttpOnly=false");
        if (secure) {
            mobileCookie.append("; SameSite=None; Secure");
        } else {
            mobileCookie.append("; SameSite=Lax");
        }
        response.addHeader("Set-Cookie", mobileCookie.toString());
        
        // Set calendar cookie to indicate this is a calendar flow
        StringBuilder calendarCookie = new StringBuilder();
        calendarCookie.append("oauth2_calendar=true; Path=/; Max-Age=300; HttpOnly=false");
        if (secure) {
            calendarCookie.append("; SameSite=None; Secure");
        } else {
            calendarCookie.append("; SameSite=Lax");
        }
        response.addHeader("Set-Cookie", calendarCookie.toString());
        
        // Also set session attributes as backup (more reliable than cookies during redirect)
        request.getSession().setAttribute("oauth2_mobile", true);
        request.getSession().setAttribute("oauth2_calendar", true);

        logger.info("Setting oauth2_mobile and oauth2_calendar cookies/session for calendar flow (secure={})", secure);

        // Redirect to Spring OAuth2 authorization with mobile=true + calendar=true flags
        // calendar=true tells our resolver to always include calendar scope
        response.sendRedirect("/oauth2/authorization/google?mobile=true&calendar=true");
    }
}
