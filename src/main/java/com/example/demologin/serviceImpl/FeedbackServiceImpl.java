package com.example.demologin.serviceImpl;

import com.example.demologin.dto.request.FeedbackRequest;
import com.example.demologin.dto.response.FeedbackResponse;
import com.example.demologin.entity.Feedback;
import com.example.demologin.entity.User;
import com.example.demologin.repository.FeedbackRepository;
import com.example.demologin.repository.UserRepository;
import com.example.demologin.service.FeedbackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackServiceImpl implements FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final UserRepository userRepository;

    @Override
    public FeedbackResponse submitFeedback(Long userId, FeedbackRequest request) {
        Feedback feedback = Feedback.builder()
                .userId(userId)
                .rating(request.getRating())
                .comment(request.getComment())
                .build();

        feedback = feedbackRepository.save(feedback);
        log.info("⭐ User {} submitted feedback with rating {}", userId, request.getRating());

        User user = userRepository.findById(userId).orElse(null);
        return toResponse(feedback, user);
    }

    @Override
    public List<FeedbackResponse> getUserFeedback(Long userId) {
        List<Feedback> feedbackList = feedbackRepository.findByUserIdOrderByCreatedAtDesc(userId);
        User user = userRepository.findById(userId).orElse(null);
        return feedbackList.stream()
                .map(f -> toResponse(f, user))
                .collect(Collectors.toList());
    }

    @Override
    public Page<FeedbackResponse> getAllFeedback(Pageable pageable) {
        Page<Feedback> page = feedbackRepository.findAllByOrderByCreatedAtDesc(pageable);

        // Batch-load users for the page
        List<Long> userIds = page.getContent().stream()
                .map(Feedback::getUserId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getUserId, Function.identity()));

        return page.map(f -> toResponse(f, userMap.get(f.getUserId())));
    }

    private FeedbackResponse toResponse(Feedback feedback, User user) {
        return FeedbackResponse.builder()
                .id(feedback.getId())
                .userId(feedback.getUserId())
                .username(user != null ? user.getUsername() : null)
                .fullName(user != null ? user.getFullName() : null)
                .rating(feedback.getRating())
                .comment(feedback.getComment())
                .createdAt(feedback.getCreatedAt())
                .build();
    }
}
