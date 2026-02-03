package com.example.demologin.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.example.demologin.service.AuthenticationService;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class SecurityConfig {


    private final AuthenticationService authenticationService;

    private final Filter filter;

    private final PublicEndpointHandlerMapping publicEndpointHandlerMapping;

    private final ClientRegistrationRepository clientRegistrationRepository;

    public SecurityConfig(@Lazy AuthenticationService authenticationService, Filter filter, PublicEndpointHandlerMapping publicEndpointHandlerMapping, ClientRegistrationRepository clientRegistrationRepository) {
        this.authenticationService = authenticationService;
        this.filter = filter;
        this.publicEndpointHandlerMapping = publicEndpointHandlerMapping;
        this.clientRegistrationRepository = clientRegistrationRepository;
    }
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http , CustomOAuth2SuccessHandler customOAuth2SuccessHandler, CustomOAuth2FailureHandler customOAuth2FailureHandler) throws Exception {
        // Lấy danh sách các public endpoints từ annotation @PublicEndpoint
        List<String> annotatedPublicEndpoints = publicEndpointHandlerMapping.getPublicEndpoints();
        
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> {
                    // Cho phép preflight requests
                    auth.requestMatchers(CorsUtils::isPreFlightRequest).permitAll();
                    
                    // Tự động permit all các endpoints được đánh dấu @PublicEndpoint
                    if (!annotatedPublicEndpoints.isEmpty()) {
                        auth.requestMatchers(annotatedPublicEndpoints.toArray(new String[0])).permitAll();
                    }
                    
                    // Các endpoint hệ thống cần permit all (không thể dùng annotation)
                    auth.requestMatchers(
                            // Swagger/OpenAPI documentation
                            "/swagger-ui/**",
                            "/v3/api-docs/**", 
                            "/swagger-resources/**",
                            "/webjars/**",
                            // OAuth2 system endpoints (Spring Security tự động tạo)
                            "/login/oauth2/code/**",
                            "/oauth2/authorization/**",
                            // Mobile-initiated OAuth2 redirect helper
                            "/mobi/oauth2/authorization/**",
                            // Google Calendar OAuth2 endpoints (public - validate via state/JWT)
                            "/oauth2/google/calendar/callback",
                            "/oauth2/google/calendar/authorize/mobile",
                            "/debug/**"
                    ).permitAll();
                    
                    // Tất cả các API endpoints khác cần authentication
                    // Filter sẽ handle JWT validation + dynamic permission với @SecuredEndpoint
                    auth.requestMatchers("/api/**").authenticated();
                    auth.anyRequest().authenticated();
                })
                .oauth2Login(oauth2 -> oauth2
                    .loginPage("/oauth2/authorization/google")
                    .authorizationEndpoint(authEndpoint -> authEndpoint
                        .authorizationRequestResolver(
                            new CustomAuthorizationRequestResolver(clientRegistrationRepository, "/oauth2/authorization")
                        )
                    )
                    .defaultSuccessUrl("/api/oauth2/success")
                    .failureUrl("/api/oauth2/failure")
                    .successHandler(customOAuth2SuccessHandler)
                    .failureHandler(customOAuth2FailureHandler)
                )
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .userDetailsService(authenticationService)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setExposedHeaders(Arrays.asList("Access-Control-Allow-Origin", "Access-Control-Allow-Methods", "Access-Control-Allow-Headers", "Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                        )
                );
    }

}
