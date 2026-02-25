package com.example.demologin.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demologin.annotation.ApiResponse;
import com.example.demologin.annotation.AuthenticatedEndpoint;
import com.example.demologin.annotation.PageResponse;
import com.example.demologin.annotation.SecuredEndpoint;
import com.example.demologin.dto.request.PackageRequest;
import com.example.demologin.dto.response.QuotaResponse;
import com.example.demologin.dto.response.UserResponse;
import com.example.demologin.entity.User;
import com.example.demologin.service.UserService;
import com.example.demologin.utils.AccountUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/users")
@Tag(name = "User Management", description = "APIs for managing users (admin only)")
public class UserController {

    private final UserService userService;
    private final AccountUtils accountUtils;
    private final com.example.demologin.service.QuotaService quotaService;

    @GetMapping
    @PageResponse
    @ApiResponse(message = "Users retrieved successfully")
    @SecuredEndpoint("USER_MANAGE")
    @Operation(summary = "Get all users (paginated)",
            description = "Retrieve paginated list of all users in the system (admin only)")
    public Object getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return userService.getAllUsers(page, size);
    }

        /**
         * Admin endpoint to change the subscription package for a specific user.
         */
        @PutMapping("/{userId}/package")
        @ApiResponse(message = "User package updated")
        @SecuredEndpoint("USER_MANAGE")
        @Operation(summary = "Update user package",
               description = "Admin can upgrade or downgrade a user's subscription package")
        public Object updateUserPackage(
            @PathVariable("userId") Long userId,
            @RequestBody PackageRequest request) {
        // delegate to new quota service
        quotaService.setPackage(userId, request.getPackageType());
        return QuotaResponse.builder()
            .packageType(request.getPackageType())
            .remainingToday(request.getPackageType() == com.example.demologin.enums.PackageType.PREMIUM ? -1 : 3)
            .quotaResetDate(java.time.LocalDate.now())
            .premiumExpiryDate(request.getPackageType() == com.example.demologin.enums.PackageType.PREMIUM ? java.time.LocalDate.now().plusMonths(1) : null)
            .build();
        }

    @ApiResponse(message = "Users retrieved successfully")
    @GetMapping("/me")
    @AuthenticatedEndpoint
    @Operation(summary = "Get current user profile", description = "Retrieve profile of the authenticated user")
    public Object getCurrentUserProfile() {
        User currentUser = accountUtils.getCurrentUser();
        return  UserResponse.toUserResponse(currentUser);
    }
}
