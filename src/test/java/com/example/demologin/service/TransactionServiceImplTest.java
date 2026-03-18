package com.example.demologin.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.data.domain.Page;

import com.example.demologin.dto.request.PaymentTransactionQueryRequest;
import com.example.demologin.dto.response.PaymentTransactionResponse;
import com.example.demologin.entity.PaymentTransaction;
import com.example.demologin.repository.PaymentTransactionRepository;
import com.example.demologin.serviceImpl.TransactionServiceImpl;

class TransactionServiceImplTest {

    PaymentTransactionRepository repo;
    TransactionServiceImpl service;
    com.example.demologin.mapper.PaymentTransactionMapper mapper;

    @BeforeEach
    void setUp() {
        repo = mock(PaymentTransactionRepository.class);
        mapper = mock(com.example.demologin.mapper.PaymentTransactionMapper.class);
        service = new TransactionServiceImpl(repo, mapper);
    }

    @Test
    void createPendingTransaction_savesRecordWithOrderCode() {
        service.createPendingTransaction(5L, "5_123456", 50000);
        verify(repo).save(argThat(tx -> tx.getUserId().equals(5L)
                && tx.getTxnRef().equals("5_123456")
                && tx.getOrderCode().equals("123456")
                && tx.getAmount().equals(50000L)
                && tx.getStatus().equals("PENDING")));
    }

    @Test
    void recordPayOSWebhook_updatesExisting() {
        PaymentTransaction existing = new PaymentTransaction();
        existing.setTxnRef("1_999");
        existing.setOrderCode("999");
        when(repo.findByOrderCode("999")).thenReturn(existing);

        Map<String, Object> data = new HashMap<>();
        data.put("orderCode", "999");
        data.put("code", "00");
        data.put("desc", "Payment success");
        data.put("counterAccountName", "NGUYEN VAN A");
        Map<String, Object> body = new HashMap<>();
        body.put("data", data);

        service.recordPayOSWebhook(body);
        assertEquals("SUCCESS", existing.getStatus());
        assertEquals("00", existing.getPayosCode());
        assertEquals("NGUYEN VAN A", existing.getCounterAccountName());
        verify(repo).save(existing);
    }

    @Test
    void getUserIdByOrderCode_returnsUserId() {
        PaymentTransaction tx = new PaymentTransaction();
        tx.setUserId(42L);
        when(repo.findByOrderCode("12345")).thenReturn(tx);
        assertEquals(42L, service.getUserIdByOrderCode("12345"));
    }

    @Test
    void getUserIdByOrderCode_returnsNullWhenNotFound() {
        when(repo.findByOrderCode("99999")).thenReturn(null);
        assertNull(service.getUserIdByOrderCode("99999"));
    }

    @Test
    void markCancelledByOrderCode_updatesStatus() {
        PaymentTransaction tx = new PaymentTransaction();
        tx.setStatus("PENDING");
        when(repo.findByOrderCode("111")).thenReturn(tx);
        service.markCancelledByOrderCode("111");
        assertEquals("CANCELLED", tx.getStatus());
        verify(repo).save(tx);
    }

    @Test
    void getTransactionStats_calculatesCorrectly() {
        PaymentTransaction success1 = PaymentTransaction.builder()
                .status("SUCCESS").amount(50000L).txnRef("a").build();
        PaymentTransaction success2 = PaymentTransaction.builder()
                .status("SUCCESS").amount(50000L).txnRef("b").build();
        PaymentTransaction failed = PaymentTransaction.builder()
                .status("FAILED").amount(50000L).txnRef("c").build();
        PaymentTransaction pending = PaymentTransaction.builder()
                .status("PENDING").amount(50000L).txnRef("d").build();
        when(repo.findAll()).thenReturn(List.of(success1, success2, failed, pending));

        Map<String, Object> stats = service.getTransactionStats();
        assertEquals(4L, stats.get("totalTransactions"));
        assertEquals(2L, stats.get("successfulTransactions"));
        assertEquals(1L, stats.get("failedTransactions"));
        assertEquals(1L, stats.get("pendingTransactions"));
        assertEquals(100000L, stats.get("totalRevenue"));
    }

    @Test
    void searchTransactions_delegatesToRepo() {
        PaymentTransactionQueryRequest req = new PaymentTransactionQueryRequest();
        req.setUserId(7L);
        when(repo.findWithFilters(eq(7L), any(), any(), any(), any(), any(), any()))
            .thenReturn(Page.empty());
        Page<PaymentTransactionResponse> result = service.searchTransactions(req, 0, 10);
        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
    }

    @Test
    void searchTransactions_handlesRepoException() {
        PaymentTransactionQueryRequest req = new PaymentTransactionQueryRequest();
        when(repo.findWithFilters(any(), any(), any(), any(), any(), any(), any()))
            .thenThrow(new RuntimeException("db error"));
        Page<PaymentTransactionResponse> result = service.searchTransactions(req, 1, 5);
        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
    }

        @Test
        void expirePendingTransactions_invokesRepositoryWithCutoff() {
            // we can't deterministically know the instant, but we can capture
            // the argument using Mockito's argument captor and ensure it's roughly
            // 15 minutes ago
            when(repo.expireOldTransactions(any())).thenReturn(3);

            service.expirePendingTransactions();

            // verify that the repository method was called once and that the
            // cutoff time is between now-16 and now-14 minutes (allowing some slack)
            verify(repo).expireOldTransactions(argThat(dt -> {
                LocalDateTime now = LocalDateTime.now();
                return !dt.isAfter(now.minusMinutes(14)) && !dt.isBefore(now.minusMinutes(16));
            }));
        }
}
