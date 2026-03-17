package com.example.demologin.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;

import com.example.demologin.serviceImpl.PaymentServiceImpl;

/**
 * Tests for PaymentServiceImpl (PayOS integration).
 * Note: Full integration tests require valid PayOS credentials.
 * These unit tests verify constructor wiring and basic error handling.
 */
public class PaymentServiceImplTest {
    private PaymentServiceImpl service;
    private com.example.demologin.service.TransactionService transactionService;

    @BeforeEach
    void setUp() {
        transactionService = mock(com.example.demologin.service.TransactionService.class);
        // Use dummy transaction service; PayOS API integration is mocked/ignored in unit tests
        service = new PaymentServiceImpl(transactionService);
    }

    @Test
    public void constructor_createsServiceWithoutError() {
        assertNotNull(service);
    }

    @Test
    public void createPremiumUrl_throwsWithInvalidCredentials() {
        // With dummy credentials, creating a payment link should throw
        assertThrows(RuntimeException.class, () -> {
            service.createPremiumUrl(123L, 50000L);
        });
    }
}
