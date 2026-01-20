package com.example.demologin.controller;

import com.example.demologin.annotation.ApiResponse;
import com.example.demologin.annotation.SecuredEndpoint;
import com.example.demologin.dto.request.BaseActionRequest;
import com.example.demologin.service.TokenVersionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/token-version")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Token Version Management", description = "APIs for managing user token versions and invalidating tokens")
    public class TokenVersionController {
    
    private final TokenVersionService tokenVersionService;
    
    @PostMapping("/invalidate-all")
    @SecuredEndpoint("TOKEN_INVALIDATE_OWN")
    @Operation(summary = "Invalidate all tokens for current user", 
               description = "Increment token version to invalidate all existing tokens for the current user")
    @ApiResponse(message = "All tokens invalidated successfully")
    public Object invalidateAllCurrentUserTokens(
            @Valid @RequestBody BaseActionRequest request) {
        
        return tokenVersionService.incrementCurrentUserTokenVersion();
    }
    
    @PostMapping("/invalidate-user/{userId}")
    @SecuredEndpoint("TOKEN_INVALIDATE_USER")
    @Operation(summary = "Admin invalidate all tokens for user by ID", 
               description = "Admin operation to increment token version and invalidate all tokens for specific user")
    @ApiResponse(message = "User tokens invalidated successfully")
    public Object invalidateUserTokensByUserId(
            @Parameter(description = "User ID") @PathVariable Long userId,
            @Valid @RequestBody BaseActionRequest request) {
        
        return tokenVersionService.incrementUserTokenVersionByUserId(userId);
    }
    

    
    @GetMapping("/current")
    @SecuredEndpoint("TOKEN_VIEW_OWN")
    @Operation(summary = "Get current user token version", 
               description = "Get the current token version for the authenticated user")
    @ApiResponse(message = "Token version retrieved successfully")
    public Object getCurrentUserTokenVersion() {
        return tokenVersionService.getCurrentUserTokenVersion();
    }
    
    @GetMapping("/user/{userId}")
    @SecuredEndpoint("TOKEN_VIEW_USER")
    @Operation(summary = "Get user token version by ID", 
               description = "Admin operation to get token version for specific user by ID")
    @ApiResponse(message = "User token version retrieved successfully")
    public Object getUserTokenVersionByUserId(
            @Parameter(description = "User ID") @PathVariable Long userId) {
        
        return tokenVersionService.getUserTokenVersionByUserId(userId);
    }
}
