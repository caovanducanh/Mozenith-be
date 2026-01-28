package com.example.demologin.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.io.IOException;

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
}
