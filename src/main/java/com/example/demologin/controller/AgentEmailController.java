package com.example.demologin.controller;

import com.example.demologin.annotation.ApiResponse;
import com.example.demologin.annotation.AuthenticatedEndpoint;
import com.example.demologin.dto.request.AgentSendEmailRequest;
import com.example.demologin.entity.User;
import com.example.demologin.service.EmailService;
import com.example.demologin.utils.AccountUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/email")
@Tag(name = "Agent Email", description = "APIs for sending emails on behalf of users via the AI agent")
public class AgentEmailController {

    private final EmailService emailService;
    private final AccountUtils accountUtils;

    @PostMapping("/agent-send")
    @AuthenticatedEndpoint
    @ApiResponse(message = "Email sent successfully")
    @Operation(summary = "Send email on behalf of user",
               description = "AI agent calls this to send an email from the system address with the user's name")
    public Object sendEmailOnBehalf(@RequestBody AgentSendEmailRequest request) {
        User user = accountUtils.getCurrentUser();
        String fromName = user.getFullName();

        emailService.sendEmailOnBehalf(
                fromName,
                request.getToEmail(),
                request.getSubject(),
                request.getMessage(),
                request.getCcEmail()
        );

        return "Email sent on behalf of " + fromName;
    }
}
