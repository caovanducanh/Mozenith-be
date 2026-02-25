package com.example.demologin.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demologin.annotation.ApiResponse;
import com.example.demologin.annotation.AuthenticatedEndpoint;
import com.example.demologin.entity.User;
import com.example.demologin.enums.PackageType;
import com.example.demologin.service.QuotaService;
import com.example.demologin.utils.AccountUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/quota")
@Tag(name = "Quota", description = "APIs for querying and consuming AI quota")
public class QuotaController {
    private final QuotaService quotaService;
    private final AccountUtils accountUtils;

    @GetMapping
    @AuthenticatedEndpoint
    @ApiResponse(message = "Quota retrieved")
    @Operation(summary = "Get current user's quota",
               description = "Returns remaining AI calls and package information for the authenticated user")
    public Object getQuota() {
        User user = accountUtils.getCurrentUser();
        return quotaService.getQuota(user.getUserId());
    }

    @PostMapping("/consume")
    @AuthenticatedEndpoint
    @ApiResponse(message = "Quota consumed")
    @Operation(summary = "Consume one AI call",
               description = "Decrements daily quota for free users, throws if limit exceeded")
    public Object consumeQuota() {
        User user = accountUtils.getCurrentUser();
        return quotaService.consumeQuota(user.getUserId());
    }

    @PostMapping("/upgrade")
    @AuthenticatedEndpoint
    @ApiResponse(message = "Subscription upgraded")
    @Operation(summary = "Upgrade to premium",
               description = "User upgrades their own package to premium (billing handled separately)")
    public Object upgradeToPremium() {
        User user = accountUtils.getCurrentUser();
        quotaService.setPackage(user.getUserId(), PackageType.PREMIUM);
        return quotaService.getQuota(user.getUserId());
    }
}
