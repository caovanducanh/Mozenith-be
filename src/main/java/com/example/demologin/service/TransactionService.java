package com.example.demologin.service;

import com.example.demologin.dto.request.PaymentTransactionQueryRequest;
import com.example.demologin.dto.response.PaymentTransactionResponse;
import org.springframework.data.domain.Page;

public interface TransactionService {
    /**
     * Create a new pending transaction record when we generate the payment URL.
     */
    void createPendingTransaction(Long userId, String txnRef, long amount);

    /**
     * Process a PayOS webhook callback and update the transaction record.
     */
    void recordPayOSWebhook(java.util.Map<String, Object> webhookBody);

    /**
     * Get the userId associated with a PayOS orderCode.
     */
    Long getUserIdByOrderCode(String orderCode);

    /**
     * Mark a transaction as CANCELLED by its PayOS orderCode.
     */
    void markCancelledByOrderCode(String orderCode);

    /**
     * Mark a transaction as SUCCESS by its PayOS orderCode.
     */
    void markSuccessByOrderCode(String orderCode);

    /**
     * Search using arbitrary filters, sorting, and pagination.
     */
    Page<PaymentTransactionResponse> searchTransactions(PaymentTransactionQueryRequest request,
                                                       int page, int size);

    PaymentTransactionResponse getTransactionById(Long id);

    /**
     * Get the latest transaction for a given user.
     */
    PaymentTransactionResponse getLatestTransactionForUser(Long userId);

    /**
     * Get transaction statistics for the dashboard.
     */
    java.util.Map<String, Object> getTransactionStats();
}
