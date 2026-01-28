package com.example.demologin.config;

import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import jakarta.servlet.http.HttpServletRequest;
import com.example.demologin.utils.UserAgentUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wraps the default resolver and encodes a mobile flag into the state if the
 * original request contains `mobile=true`.
 */
public class CustomAuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    private final OAuth2AuthorizationRequestResolver delegate;
    private static final Logger logger = LoggerFactory.getLogger(CustomAuthorizationRequestResolver.class);

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

    OAuth2AuthorizationRequest maybeTagMobile(HttpServletRequest request, OAuth2AuthorizationRequest original) {
        if (original == null) return null;
        String mobile = request.getParameter("mobile");
        if (mobile != null && (mobile.equalsIgnoreCase("true") || mobile.equals("1"))) {
                String newState = original.getState() + "::m"; // append marker
                logger.info("Tagging OAuth2 state as mobile (state={} -> {}) for request {}", original.getState(), newState, request.getRequestURI());
                // preserve existing additional parameters and add prompt=select_account for Google flows
                java.util.Map<String, Object> additional = new java.util.HashMap<>(original.getAdditionalParameters());
                additional.put("prompt", "select_account");
                return OAuth2AuthorizationRequest.from(original)
                    .state(newState)
                    .additionalParameters(additional)
                    .build();
        }

        // Fallback: try to detect mobile via User-Agent if no explicit mobile param
        String ua = request.getHeader("User-Agent");
        if (ua != null) {
            try {
                UserAgentUtil.DeviceInfo info = UserAgentUtil.parseUserAgent(ua);
                String deviceType = info.getDeviceType();
                if ("Mobile".equalsIgnoreCase(deviceType) || "Tablet".equalsIgnoreCase(deviceType)) {
                        String newState = original.getState() + "::m"; // append marker
                        logger.info("Tagging OAuth2 state as mobile via User-Agent (deviceType={}) (state={} -> {}) for request {}", deviceType, original.getState(), newState, request.getRequestURI());
                        java.util.Map<String, Object> additional = new java.util.HashMap<>(original.getAdditionalParameters());
                        additional.put("prompt", "select_account");
                        return OAuth2AuthorizationRequest.from(original)
                            .state(newState)
                            .additionalParameters(additional)
                            .build();
                }
            } catch (Exception e) {
                // If parsing fails, don't block the flow — just continue returning original
                logger.debug("User-Agent parsing failed: {}", e.getMessage());
            }
        }
        return original;
    }
}
