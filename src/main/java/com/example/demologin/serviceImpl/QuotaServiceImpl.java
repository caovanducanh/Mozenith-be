package com.example.demologin.serviceImpl;

import com.example.demologin.dto.response.QuotaResponse;
import com.example.demologin.entity.User;
import com.example.demologin.enums.PackageType;
import com.example.demologin.exception.exceptions.NotFoundException;
import com.example.demologin.exception.exceptions.QuotaExceededException;
import com.example.demologin.repository.UserRepository;
import com.example.demologin.service.QuotaService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@AllArgsConstructor
public class QuotaServiceImpl implements QuotaService {
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public QuotaResponse getQuota(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        resetIfNeeded(user);
        int remaining = calculateRemaining(user);
        return new QuotaResponse(user.getPackageType(), remaining, user.getQuotaResetDate());
    }

    @Override
    @Transactional
    public QuotaResponse consumeQuota(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        resetIfNeeded(user);
        if (user.getPackageType() == PackageType.PREMIUM) {
            // unlimited, nothing to decrement
        } else {
            int used = user.getAiUsesToday();
            if (used >= 3) {
                throw new QuotaExceededException("Daily free quota exceeded");
            }
            user.incrementAiUsesToday();
        }
        userRepository.save(user);
        int remaining = calculateRemaining(user);
        return new QuotaResponse(user.getPackageType(), remaining, user.getQuotaResetDate());
    }

    @Override
    @Transactional
    public void setPackage(Long userId, PackageType packageType) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        user.setPackageType(packageType);
        // reset usage when changing package
        user.resetDailyQuota();
        userRepository.save(user);
    }

    private void resetIfNeeded(User user) {
        LocalDate today = LocalDate.now();
        if (!today.equals(user.getQuotaResetDate())) {
            user.resetDailyQuota();
            userRepository.save(user);
        }
    }

    private int calculateRemaining(User user) {
        if (user.getPackageType() == PackageType.PREMIUM) {
            return -1;
        }
        return Math.max(0, 3 - user.getAiUsesToday());
    }
}
