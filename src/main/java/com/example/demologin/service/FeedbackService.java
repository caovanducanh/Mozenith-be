package com.example.demologin.service;

import com.example.demologin.dto.request.FeedbackRequest;
import com.example.demologin.dto.response.FeedbackResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface FeedbackService {
    FeedbackResponse submitFeedback(Long userId, FeedbackRequest request);
    List<FeedbackResponse> getUserFeedback(Long userId);
    Page<FeedbackResponse> getAllFeedback(Pageable pageable);
}
