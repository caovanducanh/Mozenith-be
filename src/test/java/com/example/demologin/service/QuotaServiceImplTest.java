package com.example.demologin.service;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demologin.entity.User;
import com.example.demologin.enums.PackageType;
import com.example.demologin.exception.exceptions.QuotaExceededException;
import com.example.demologin.repository.UserRepository;
import com.example.demologin.serviceImpl.QuotaServiceImpl;

public class QuotaServiceImplTest {
    private UserRepository userRepository;
    private QuotaService quotaService;
    private User user;

    @BeforeEach
    public void setup() {
        userRepository = mock(UserRepository.class);
        quotaService = new QuotaServiceImpl(userRepository);

        user = new User();
        user.setUserId(1L);
        user.setPackageType(PackageType.BASIC);
        user.setAiUsesToday(0);
        user.setQuotaResetDate(LocalDate.now());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    }

    @Test
    public void testGetQuota_basic() {
        var q = quotaService.getQuota(1L);
        assertEquals(PackageType.BASIC, q.getPackageType());
        assertEquals(3, q.getRemainingToday());
        assertEquals(LocalDate.now(), q.getQuotaResetDate());
    }

    @Test
    public void testConsumeQuota_untilLimit() {
        for (int i = 0; i < 3; i++) {
            var q = quotaService.consumeQuota(1L);
            assertEquals(3 - (i + 1), q.getRemainingToday());
        }
        try {
            quotaService.consumeQuota(1L);
            fail("expected QuotaExceededException");
        } catch (QuotaExceededException expected) {
            // ok
        }
    }

    @Test
    public void testUpgradeToPremium_resetsAndUnlimited() {
        var q1 = quotaService.consumeQuota(1L); // 1 used
        assertEquals(2, q1.getRemainingToday());
        quotaService.setPackage(1L, PackageType.PREMIUM);
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, atLeastOnce()).save(captor.capture());
        User saved = captor.getValue();
        assertEquals(PackageType.PREMIUM, saved.getPackageType());
        assertEquals(0, saved.getAiUsesToday());

        var q2 = quotaService.getQuota(1L);
        assertEquals(-1, q2.getRemainingToday());
        assertNotNull(q2.getPremiumExpiryDate());
        assertTrue(q2.getPremiumExpiryDate().isAfter(LocalDate.now()));
    }

    @Test
    public void testPremiumExpiry_downgradesAutomatically() {
        // manually prepare user as premium expiring yesterday
        user.setPackageType(PackageType.PREMIUM);
        user.setPremiumExpiryDate(LocalDate.now().minusDays(1));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        var q = quotaService.getQuota(1L);
        // after calling getQuota, the service should downgrade to BASIC
        assertEquals(PackageType.BASIC, q.getPackageType());
        assertEquals(3, q.getRemainingToday());
    }
}
