package com.example.demologin.config;

import com.example.demologin.dto.response.LoginResponse;
import com.example.demologin.entity.User;
import com.example.demologin.enums.ActivityType;
import com.example.demologin.repository.UserRepository;
import com.example.demologin.service.AuthenticationService;
import com.example.demologin.service.UserActivityLogService;
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

@Component
@RequiredArgsConstructor
public class CustomOAuth2SuccessHandler implements AuthenticationSuccessHandler {


    private final AuthenticationService authenticationService;


    private final UserActivityLogService userActivityLogService;

    private final UserRepository userRepository;

    @Value("${frontend.url.base}")
    private String frontendUrl;

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
        
        try {
            LoginResponse userResponse = authenticationService.getUserResponse(email, name);
            
            // Log successful OAuth2 login
            User user = userRepository.findByEmail(email).orElse(null);
            userActivityLogService.logUserActivity(user, ActivityType.LOGIN_ATTEMPT, 
                "OAuth2 login successful via " + getProviderName(authentication));
            
            String redirectUrl = frontendUrl + "login?token=" + userResponse.getToken() + "&refreshToken="
                    + userResponse.getRefreshToken();
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
            
            response.sendRedirect(
                    frontendUrl + "login?error=" + e.getMessage().replace(" ", "_"));
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