package com.example.demologin.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

import static org.junit.jupiter.api.Assertions.*;

public class CustomAuthorizationRequestResolverTest {

    @Test
    void tagsStateWhenUserAgentIsMobile() {
        ClientRegistrationRepository stubRepo = registrationId -> null;
        CustomAuthorizationRequestResolver resolver = new CustomAuthorizationRequestResolver(stubRepo, "/oauth2/authorization");

        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI("/oauth2/authorization/google");
        req.addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 9; Pixel 3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0 Mobile Safari/537.36");

        OAuth2AuthorizationRequest original = OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .clientId("test")
                .state("origstate")
                .build();

        OAuth2AuthorizationRequest res = resolver.maybeTagMobile(req, original);
        assertNotNull(res);
        assertEquals("origstate::m", res.getState());
        assertTrue(res.getAdditionalParameters().containsKey("prompt"));
        assertEquals("select_account", res.getAdditionalParameters().get("prompt"));
    }

    @Test
    void doesNotTagStateWhenUserAgentIsDesktop() {
        ClientRegistrationRepository stubRepo = registrationId -> null;
        CustomAuthorizationRequestResolver resolver = new CustomAuthorizationRequestResolver(stubRepo, "/oauth2/authorization");

        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI("/oauth2/authorization/google");
        req.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/118.0.0.0 Safari/537.36");

        OAuth2AuthorizationRequest original = OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .clientId("test")
                .state("origstate")
                .build();

        OAuth2AuthorizationRequest res = resolver.maybeTagMobile(req, original);
        assertNotNull(res);
        assertEquals("origstate", res.getState());
        // Desktop UA should not add prompt
        assertFalse(res.getAdditionalParameters().containsKey("prompt"));
    }
}
