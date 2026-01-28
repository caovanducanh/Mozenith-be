package com.example.demologin.config;

import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Wraps the default resolver and encodes a mobile flag into the state if the
 * original request contains `mobile=true`.
 */
public class CustomAuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    private final OAuth2AuthorizationRequestResolver delegate;

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

    private OAuth2AuthorizationRequest maybeTagMobile(HttpServletRequest request, OAuth2AuthorizationRequest original) {
        if (original == null) return null;
        String mobile = request.getParameter("mobile");
        if (mobile != null && (mobile.equalsIgnoreCase("true") || mobile.equals("1"))) {
            String newState = original.getState() + "::m"; // append marker
            return OAuth2AuthorizationRequest.from(original)
                    .state(newState)
                    .build();
        }
        return original;
    }
}
