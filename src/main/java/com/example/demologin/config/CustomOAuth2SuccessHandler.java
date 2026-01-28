package com.example.demologin.config;

import com.example.demologin.dto.response.LoginResponse;
import com.example.demologin.entity.User;
import com.example.demologin.enums.ActivityType;
import com.example.demologin.repository.UserRepository;
import com.example.demologin.service.AuthenticationService;
import com.example.demologin.service.UserActivityLogService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@RequiredArgsConstructor
public class CustomOAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger logger = LoggerFactory.getLogger(CustomOAuth2SuccessHandler.class);


    private final AuthenticationService authenticationService;


    private final UserActivityLogService userActivityLogService;

    private final UserRepository userRepository;

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

            if (!isMobile && request.getCookies() != null) {
                for (Cookie c : request.getCookies()) {
                    if ("oauth2_mobile".equals(c.getName()) && "true".equalsIgnoreCase(c.getValue())) {
                        isMobile = true;
                        // clear cookie by setting maxAge=0
                        Cookie clear = new Cookie("oauth2_mobile", "");
                        clear.setPath("/");
                        clear.setMaxAge(0);
                        response.addCookie(clear);
                        logger.info("Detected mobile OAuth via cookie for {}", email);
                        break;
                    }
                }
            }

            // Also inspect OAuth2 state — the custom resolver appends ::m when mobile=true
            String state = request.getParameter("state");
            if (!isMobile && state != null && state.endsWith("::m")) {
                isMobile = true;
                logger.info("Detected mobile OAuth via state parameter for {} (state={})", email, state);
            }

            LoginResponse userResponse = authenticationService.getUserResponse(email, name);
            
            // Log successful OAuth2 login
            User user = userRepository.findByEmail(email).orElse(null);
            userActivityLogService.logUserActivity(user, ActivityType.LOGIN_ATTEMPT, 
                "OAuth2 login successful via " + getProviderName(authentication));
            
                String base = isMobile ? frontendMobileUrl : frontendUrl;
                String redirectUrl = base + "token=" + userResponse.getToken() + "&refreshToken="
                    + userResponse.getRefreshToken();
                logger.info("OAuth2 success for {} (mobile={}): redirecting to {}", email, isMobile, redirectUrl);
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