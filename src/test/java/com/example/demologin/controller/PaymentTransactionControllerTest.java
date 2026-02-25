package com.example.demologin.controller;

import com.example.demologin.dto.request.PaymentTransactionQueryRequest;
import com.example.demologin.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.*;

import org.springframework.web.bind.annotation.GetMapping;
import com.example.demologin.annotation.SecuredEndpoint;

import java.lang.reflect.Method;

class PaymentTransactionControllerTest {

    TransactionService transactionService;
    PaymentTransactionController controller;

    @BeforeEach
    void setUp() {
        transactionService = mock(TransactionService.class);
        controller = new PaymentTransactionController(transactionService);
    }

    @Test
    void search_endpoint_is_secured() throws NoSuchMethodException {
        Method m = PaymentTransactionController.class.getMethod("searchTransactions",
            String.class, String.class, String.class, String.class, String.class, String.class,
            int.class, int.class, String.class, String.class);
        assertTrue(m.isAnnotationPresent(SecuredEndpoint.class));
        SecuredEndpoint ann = m.getAnnotation(SecuredEndpoint.class);
        assertTrue(ann.value().contains("ADMIN_TRANSACTION_VIEW"));
        assertTrue(m.isAnnotationPresent(GetMapping.class));
    }

    @Test
    void getById_endpoint_is_secured() throws NoSuchMethodException {
        Method m = PaymentTransactionController.class.getMethod("getTransactionById", Long.class);
        assertTrue(m.isAnnotationPresent(SecuredEndpoint.class));
        assertTrue(m.isAnnotationPresent(GetMapping.class));
        // verify it still exists but is clearly documented as legacy
        SecuredEndpoint ann = m.getAnnotation(SecuredEndpoint.class);
        assertTrue(ann.value().contains("ADMIN_TRANSACTION_VIEW"));
    }

    @Test
    void searchTransactions_returnsDataFromService() {
        // ensure controller proxies to service without error
        PaymentTransactionQueryRequest req = new PaymentTransactionQueryRequest();
        // mock service to return empty page
        when(transactionService.searchTransactions(any(PaymentTransactionQueryRequest.class), anyInt(), anyInt()))
            .thenReturn(org.springframework.data.domain.Page.empty());
        Object resp = controller.searchTransactions(null, null, null, null, null, null, 0, 10, null, null);
        assertTrue(resp != null);
    }
}
