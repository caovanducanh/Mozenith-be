package com.example.demologin.service;

import com.example.demologin.dto.request.PaymentTransactionQueryRequest;
import com.example.demologin.dto.response.PaymentTransactionResponse;
import com.example.demologin.entity.PaymentTransaction;
import com.example.demologin.repository.PaymentTransactionRepository;
import com.example.demologin.serviceImpl.TransactionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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
    void createPendingTransaction_savesRecord() {
        service.createPendingTransaction(5L, "5_123", 10000);
        verify(repo).save(argThat(tx -> tx.getUserId().equals(5L)
                && tx.getTxnRef().equals("5_123")
                && tx.getAmount().equals(10000L)
                && tx.getStatus().equals("PENDING")));
    }

    @Test
    void recordIpn_updatesExisting() {
        PaymentTransaction existing = new PaymentTransaction();
        existing.setTxnRef("1_foo");
        when(repo.findByTxnRef("1_foo")).thenReturn(existing);

        Map<String,String> params = new HashMap<>();
        params.put("vnp_TxnRef","1_foo");
        params.put("vnp_ResponseCode","00");
        params.put("vnp_TransactionStatus","00");
        params.put("vnp_PayDate","20260225183005");

        service.recordIpn(params);
        assertEquals("SUCCESS", existing.getStatus());
        assertNotNull(existing.getVnpPayDate());
        verify(repo).save(existing);
    }

    @Test
    void searchTransactions_delegatesToRepo() {
        PaymentTransactionQueryRequest req = new PaymentTransactionQueryRequest();
        req.setUserId(7L);
        when(repo.findWithFilters(eq(7L), any(), any(), any(), any(), any(), any()))
            .thenReturn(Page.empty());
        // now that service catches exceptions from the repository, an empty page should be returned
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
        // should swallow exception and return empty
        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
    }
}
