package com.example.demologin.serviceImpl;

import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demologin.dto.response.QuotaResponse;
import com.example.demologin.entity.User;
import com.example.demologin.enums.PackageType;
import com.example.demologin.exception.exceptions.NotFoundException;
import com.example.demologin.exception.exceptions.QuotaExceededException;
import com.example.demologin.repository.UserRepository;
import com.example.demologin.service.QuotaService;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class QuotaServiceImpl implements QuotaService {
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public QuotaResponse getQuota(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        // downgrade expired premium subscriptions before calculating
        checkExpiry(user);
        resetIfNeeded(user);
        int remaining = calculateRemaining(user);
        return new QuotaResponse(user.getPackageType(), remaining, user.getQuotaResetDate(), user.getPremiumExpiryDate());
    }

    @Override
    @Transactional
    public QuotaResponse consumeQuota(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        checkExpiry(user);
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
        return new QuotaResponse(user.getPackageType(), remaining, user.getQuotaResetDate(), user.getPremiumExpiryDate());
    }

    @Override
    @Transactional
    public void setPackage(Long userId, PackageType packageType) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        user.setPackageType(packageType);
        // reset usage when changing package
        user.resetDailyQuota();
        if (packageType == PackageType.PREMIUM) {
            // extend or start premium period by one month
            if (user.getPremiumExpiryDate() == null || user.getPremiumExpiryDate().isBefore(java.time.LocalDate.now())) {
                user.setPremiumExpiryDate(java.time.LocalDate.now().plusMonths(1));
            } else {
                user.setPremiumExpiryDate(user.getPremiumExpiryDate().plusMonths(1));
            }
        } else {
            // downgraded to basic
            user.setPremiumExpiryDate(null);
        }
        userRepository.save(user);
    }

    private void resetIfNeeded(User user) {
        LocalDate today = LocalDate.now();
        if (!today.equals(user.getQuotaResetDate())) {
            user.resetDailyQuota();
            userRepository.save(user);
        }
    }

    /**
     * If the user is on PREMIUM but their expiry date has passed, downgrade them
     * to BASIC and clear the expiry.
     */
    private void checkExpiry(User user) {
        if (user.getPackageType() == PackageType.PREMIUM && user.getPremiumExpiryDate() != null) {
            if (LocalDate.now().isAfter(user.getPremiumExpiryDate())) {
                user.setPackageType(PackageType.BASIC);
                user.setPremiumExpiryDate(null);
                userRepository.save(user);
            }
        }
    }

    private int calculateRemaining(User user) {
        if (user.getPackageType() == PackageType.PREMIUM) {
            return -1;
        }
        return Math.max(0, 3 - user.getAiUsesToday());
    }
}
