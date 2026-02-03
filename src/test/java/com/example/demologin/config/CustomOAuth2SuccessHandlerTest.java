package com.example.demologin.config;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

import com.example.demologin.entity.User;
import com.example.demologin.repository.UserRepository;
import com.example.demologin.service.AuthenticationService;
import com.example.demologin.service.GoogleCalendarService;
import com.example.demologin.service.UserActivityLogService;

class CustomOAuth2SuccessHandlerTest {

    AuthenticationService authenticationService;
    UserActivityLogService userActivityLogService;
    UserRepository userRepository;
    OAuth2AuthorizedClientService authorizedClientService;
    GoogleCalendarService googleCalendarService;
    CustomOAuth2SuccessHandler handler;

    @BeforeEach
    void setUp() {
        authenticationService = mock(AuthenticationService.class);
        userActivityLogService = mock(UserActivityLogService.class);
        userRepository = mock(UserRepository.class);
        authorizedClientService = mock(OAuth2AuthorizedClientService.class);
        googleCalendarService = mock(GoogleCalendarService.class);
        handler = new CustomOAuth2SuccessHandler(authenticationService, userActivityLogService, userRepository, authorizedClientService, googleCalendarService);
    }

    @Test
    void onAuthenticationSuccess_saves_credential_when_present() throws Exception {
        Map<String, Object> attrs = Map.of("email", "a@example.com", "name", "A User");
        DefaultOAuth2User user = new DefaultOAuth2User(Set.of(new SimpleGrantedAuthority("ROLE_USER")), attrs, "email");
        ClientRegistration reg = ClientRegistration.withRegistrationId("google")
            .clientId("cid")
            .clientSecret("cs")
            .authorizationUri("auth")
            .tokenUri("token")
            .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
            .authorizationGrantType(org.springframework.security.oauth2.core.AuthorizationGrantType.AUTHORIZATION_CODE)
            .build();
        OAuth2AccessToken at = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "access", Instant.now(), Instant.now().plusSeconds(3600), Set.of("openid"));
        OAuth2RefreshToken rt = new OAuth2RefreshToken("refresh", Instant.now());
        OAuth2AuthorizedClient client = new OAuth2AuthorizedClient(reg, "principal", at, rt);

        when(authorizedClientService.loadAuthorizedClient("google", "a@example.com")).thenReturn(client);
        User u = new User(); u.setUserId(42L); u.setEmail("a@example.com");
        when(userRepository.findByEmail("a@example.com")).thenReturn(java.util.Optional.of(u));
        when(authenticationService.getUserResponse(anyString(), anyString())).thenReturn(new com.example.demologin.dto.response.LoginResponse("t","r"));

        OAuth2AuthenticationToken token = new OAuth2AuthenticationToken(user, Set.of(new SimpleGrantedAuthority("ROLE_USER")), "google");

        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse resp = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(req, resp, token);

        verify(googleCalendarService, times(1)).saveCredentialFromAuthorizedClient(eq(42L), eq("access"), eq("refresh"), anySet(), any());
    }
}
