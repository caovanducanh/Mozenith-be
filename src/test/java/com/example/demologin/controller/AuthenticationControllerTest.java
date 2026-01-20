package com.example.demologin.controller;

import com.example.demologin.service.AuthenticationService;
import com.example.demologin.service.RefreshTokenService;
import com.example.demologin.dto.request.user.UserRegistrationRequest;
import com.example.demologin.dto.request.login.LoginRequest;
import com.example.demologin.dto.request.login.GoogleLoginRequest;
import com.example.demologin.dto.request.login.FacebookLoginRequest;
import com.example.demologin.dto.request.token.TokenRefreshRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import com.example.demologin.dto.response.LoginResponse;
import com.example.demologin.dto.response.TokenRefreshResponse;

class AuthenticationControllerTest {
    @Test
    void oauth2LoginSuccess() {
        org.springframework.security.core.Authentication authentication = mock(org.springframework.security.core.Authentication.class);
        com.example.demologin.dto.response.LoginResponse resp = new com.example.demologin.dto.response.LoginResponse("token", "refresh");
        when(authenticationService.authenticateWithOAuth2FromAuthentication(authentication)).thenReturn(resp);
        Object result = controller.oauth2LoginSuccess(authentication);
        assertEquals(resp, result);
        verify(authenticationService).authenticateWithOAuth2FromAuthentication(authentication);
    }


    @Mock
    private AuthenticationService authenticationService;
    @Mock
    private RefreshTokenService refreshTokenService;
    @InjectMocks
    private AuthenticationController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void register() {
        UserRegistrationRequest req = new UserRegistrationRequest();
        LoginResponse resp = new LoginResponse("token", "refresh");
        when(authenticationService.register(req)).thenReturn(resp);
        Object result = controller.register(req);
        assertEquals(resp, result);
        verify(authenticationService).register(req);
    }

    @Test
    void login() {
        LoginRequest req = new LoginRequest();
        LoginResponse resp = new LoginResponse("token", "refresh");
        when(authenticationService.login(req)).thenReturn(resp);
        Object result = controller.login(req);
        assertEquals(resp, result);
        verify(authenticationService).login(req);
    }

    @Test
    void loginWithGoogle() {
        GoogleLoginRequest req = new GoogleLoginRequest();
        LoginResponse resp = new LoginResponse("token", "refresh");
        when(authenticationService.authenticateWithGoogle(req)).thenReturn(resp);
        Object result = controller.loginWithGoogle(req);
        assertEquals(resp, result);
        verify(authenticationService).authenticateWithGoogle(req);
    }

    @Test
    void loginWithFacebook() {
        FacebookLoginRequest req = new FacebookLoginRequest();
        LoginResponse resp = new LoginResponse("token", "refresh");
        when(authenticationService.authenticateWithFacebook(req)).thenReturn(resp);
        Object result = controller.loginWithFacebook(req);
        assertEquals(resp, result);
        verify(authenticationService).authenticateWithFacebook(req);
    }

    @Test
    void refreshToken() {
        TokenRefreshRequest req = new TokenRefreshRequest();
        req.setRefreshToken("abc");
        TokenRefreshResponse resp = new TokenRefreshResponse("access", "refresh");
        when(refreshTokenService.refreshToken("abc")).thenReturn(resp);
        Object result = controller.refreshToken(req);
        assertEquals(resp, result);
        verify(refreshTokenService).refreshToken("abc");
    }
}
