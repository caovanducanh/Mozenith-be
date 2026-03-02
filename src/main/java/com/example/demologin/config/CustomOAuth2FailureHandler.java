package com.example.demologin.config;

import com.example.demologin.utils.UserAgentUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomOAuth2FailureHandler implements AuthenticationFailureHandler {

    private static final Logger logger = LoggerFactory.getLogger(CustomOAuth2FailureHandler.class);

    @Value("${frontend.url.base}")
    private String frontendUrl;

    @Value("${frontend.url.mobile}")
    private String frontendMobileUrl;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        org.springframework.security.core.AuthenticationException exception)
            throws IOException, ServletException {

        logger.error("OAuth2 login failed: {}", exception.getMessage(), exception);

        boolean isMobile = false;

        // Check session attribute
        if (request.getSession(false) != null) {
            Object mobileAttr = request.getSession(false).getAttribute("oauth2_mobile");
            isMobile = Boolean.TRUE.equals(mobileAttr);
        }

        // Check cookie
        if (!isMobile && request.getCookies() != null) {
            for (Cookie c : request.getCookies()) {
                if ("oauth2_mobile".equals(c.getName()) && "true".equalsIgnoreCase(c.getValue())) {
                    isMobile = true;
                    Cookie clear = new Cookie("oauth2_mobile", "");
                    clear.setPath("/");
                    clear.setMaxAge(0);
                    response.addCookie(clear);
                    break;
                }
            }
        }

        // Check OAuth2 state parameter
        if (!isMobile) {
            String state = request.getParameter("state");
            if (state != null && state.contains("::m")) {
                isMobile = true;
            }
        }

        // Fallback: User-Agent detection
        if (!isMobile) {
            try {
                String ua = request.getHeader("User-Agent");
                if (ua != null) {
                    UserAgentUtil.DeviceInfo info = UserAgentUtil.parseUserAgent(ua);
                    String deviceType = info.getDeviceType();
                    if ("Mobile".equalsIgnoreCase(deviceType) || "Tablet".equalsIgnoreCase(deviceType)) {
                        isMobile = true;
                    }
                }
            } catch (Exception ignored) {
            }
        }

        String base = isMobile ? frontendMobileUrl : frontendUrl;
        String errorMessage = exception.getMessage() == null ? "unknown_error" : exception.getMessage().replace(" ", "_");
        String redirectUrl = base + "error=" + errorMessage;
        logger.info("OAuth2 failure redirect (mobile={}) to {}", isMobile, redirectUrl);
        response.sendRedirect(redirectUrl);
    }
}