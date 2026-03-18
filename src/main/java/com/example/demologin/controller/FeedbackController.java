package com.example.demologin.controller;

import com.example.demologin.annotation.ApiResponse;
import com.example.demologin.annotation.AuthenticatedEndpoint;
import com.example.demologin.annotation.PageResponse;
import com.example.demologin.annotation.SecuredEndpoint;
import com.example.demologin.dto.request.FeedbackRequest;
import com.example.demologin.dto.response.FeedbackResponse;
import com.example.demologin.dto.response.ResponseObject;
import com.example.demologin.service.FeedbackService;
import com.example.demologin.utils.AccountUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
@Tag(name = "Feedback", description = "Endpoints for user reviews and feedback")
public class FeedbackController {

    private final FeedbackService feedbackService;
    private final AccountUtils accountUtils;

    @PostMapping
    @AuthenticatedEndpoint
    @Operation(summary = "Submit feedback", description = "Submit a rating and comment as feedback")
    public ResponseEntity<ResponseObject> submitFeedback(@Valid @RequestBody FeedbackRequest request) {
        Long userId = accountUtils.getCurrentUser().getUserId();
        log.info("⭐ User {} submitting feedback", userId);
        FeedbackResponse response = feedbackService.submitFeedback(userId, request);
        return ResponseEntity.status(201).body(new ResponseObject(201, "Feedback submitted successfully", response));
    }

    @GetMapping("/my")
    @AuthenticatedEndpoint
    @Operation(summary = "Get my feedback", description = "Get all feedback submitted by the current user")
    public ResponseEntity<ResponseObject> getMyFeedback() {
        Long userId = accountUtils.getCurrentUser().getUserId();
        List<FeedbackResponse> feedback = feedbackService.getUserFeedback(userId);
        return ResponseEntity.ok(new ResponseObject(200, "User feedback retrieved", feedback));
    }

    @GetMapping("/all")
    @AuthenticatedEndpoint
    @PageResponse
    @ApiResponse(message = "All feedback retrieved")
    @Operation(summary = "Get all feedback", description = "Get all feedback from all users (admin dashboard)")
    public Object getAllFeedback(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return feedbackService.getAllFeedback(PageRequest.of(page, size));
    }
}
