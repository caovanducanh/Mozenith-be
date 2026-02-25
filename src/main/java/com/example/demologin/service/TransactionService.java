package com.example.demologin.service;

import com.example.demologin.dto.request.PaymentTransactionQueryRequest;
import com.example.demologin.dto.response.PaymentTransactionResponse;
import org.springframework.data.domain.Page;

public interface TransactionService {
    /**
     * create a new pending transaction record when we generate the payment URL.
     */
    void createPendingTransaction(Long userId, String txnRef, long amount);

    /**
     * update an existing transaction based on the parameters received from the
     * IPN callback.  If no record exists for the given txnRef, a new one is
     * created to ensure history is preserved.
     */
    void recordIpn(java.util.Map<String, String> params);

    /**
     * search using arbitrary filters, sorting, and pagination.
     */
    Page<PaymentTransactionResponse> searchTransactions(PaymentTransactionQueryRequest request,
                                                       int page, int size);

    PaymentTransactionResponse getTransactionById(Long id);
}
