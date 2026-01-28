package com.example.demologin.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Cookie;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.io.IOException;

@Controller
public class MobileOAuth2Controller {

    @GetMapping("/mobi/oauth2/authorization/{provider}")
    public void mobileOauthAuthorize(HttpServletRequest request, HttpServletResponse response,
                                     @PathVariable String provider) throws IOException {
        // Mark this session as a mobile-initiated OAuth flow so success handler uses deep-link
        // Use a short-lived cookie so the flag survives the OAuth redirect roundtrip
        Cookie cookie = new Cookie("oauth2_mobile", "true");
        cookie.setPath("/");
        cookie.setHttpOnly(false); // readable by server-side code
        cookie.setMaxAge(300); // 5 minutes
        response.addCookie(cookie);
        // Redirect to the default Spring OAuth2 authorization endpoint
        // Also include a mobile flag in the query so our custom resolver will tag the OAuth2 state
        response.sendRedirect("/oauth2/authorization/" + provider + "?mobile=true");
    }
}
