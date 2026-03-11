package com.example.demologin.serviceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demologin.dto.request.PaymentTransactionQueryRequest;
import com.example.demologin.dto.response.PaymentTransactionResponse;
import com.example.demologin.entity.PaymentTransaction;
import com.example.demologin.exception.exceptions.NotFoundException;
import com.example.demologin.mapper.PaymentTransactionMapper;
import com.example.demologin.repository.PaymentTransactionRepository;
import com.example.demologin.service.TransactionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class TransactionServiceImpl implements TransactionService {

    private final PaymentTransactionRepository transactionRepository;
    private final PaymentTransactionMapper mapper;

    @Override
    @Transactional
    public void createPendingTransaction(Long userId, String txnRef, long amount) {
        // Extract orderCode from txnRef (format: "{userId}_{orderCode}")
        String orderCode = txnRef.contains("_") ? txnRef.split("_", 2)[1] : txnRef;

        PaymentTransaction tx = PaymentTransaction.builder()
                .userId(userId)
                .txnRef(txnRef)
                .orderCode(orderCode)
                .amount(amount)
                .status("PENDING")
                .build();
        transactionRepository.save(tx);
    }

    @Override
    @Transactional
    public void recordPayOSWebhook(Map<String, Object> webhookBody) {
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) webhookBody.get("data");
        if (data == null) {
            log.warn("PayOS webhook without data field: {}", webhookBody);
            return;
        }

        String orderCode = String.valueOf(data.getOrDefault("orderCode", ""));
        if (orderCode.isEmpty()) {
            log.warn("PayOS webhook without orderCode: {}", data);
            return;
        }

        PaymentTransaction tx = transactionRepository.findByOrderCode(orderCode);
        if (tx == null) {
            // try to find by txnRef pattern
            tx = new PaymentTransaction();
            tx.setOrderCode(orderCode);
            tx.setTxnRef("unknown_" + orderCode);
            tx.setAmount(data.get("amount") != null ? Long.parseLong(String.valueOf(data.get("amount"))) : 0L);
        }

        // Update from PayOS webhook data
        String code = String.valueOf(data.getOrDefault("code", ""));
        tx.setPayosCode(code);
        tx.setPayosDescription(data.get("desc") != null ? String.valueOf(data.get("desc")) : null);
        tx.setPayosTransactionRef(data.get("reference") != null ? String.valueOf(data.get("reference")) : null);
        tx.setCounterAccountNumber(data.get("counterAccountNumber") != null ? String.valueOf(data.get("counterAccountNumber")) : null);
        tx.setCounterAccountName(data.get("counterAccountName") != null ? String.valueOf(data.get("counterAccountName")) : null);

        if ("00".equals(code)) {
            tx.setStatus("SUCCESS");
        } else if (code != null && !code.isEmpty()) {
            tx.setStatus("FAILED");
        }

        transactionRepository.save(tx);
    }

    @Override
    public Long getUserIdByOrderCode(String orderCode) {
        PaymentTransaction tx = transactionRepository.findByOrderCode(orderCode);
        return tx != null ? tx.getUserId() : null;
    }

    @Override
    @Transactional
    public void markCancelledByOrderCode(String orderCode) {
        PaymentTransaction tx = transactionRepository.findByOrderCode(orderCode);
        if (tx != null) {
            tx.setStatus("CANCELLED");
            transactionRepository.save(tx);
        }
    }

    @Override
    public Page<PaymentTransactionResponse> searchTransactions(PaymentTransactionQueryRequest request,
                                                               int page, int size) {
        Pageable pageable;
        Sort.Direction dir = Sort.Direction.DESC;
        if (request.getSortDir() != null && request.getSortDir().equalsIgnoreCase("ASC")) {
            dir = Sort.Direction.ASC;
        }
        String sortBy = request.getSortBy() != null ? request.getSortBy() : "createdAt";
        pageable = PageRequest.of(page, size, Sort.by(dir, sortBy));

        LocalDateTime start = null, end = null;
        if (request.getStartDate() != null) {
            start = LocalDate.parse(request.getStartDate()).atStartOfDay();
        }
        if (request.getEndDate() != null) {
            end = LocalDate.parse(request.getEndDate()).atTime(23, 59, 59);
        }

        Page<PaymentTransaction> results;
        try {
            results = transactionRepository.findWithFilters(
                    request.getUserId(),
                    request.getTxnRef(),
                    request.getStatus(),
                    request.getId(),
                    start,
                    end,
                    pageable);
        } catch (Exception ex) {
            log.error("Transaction search failed", ex);
            return Page.empty();
        }
        if (results.getContent().isEmpty()) {
            return results.map(mapper::toResponse);
        }
        return results.map(mapper::toResponse);
    }

    @Override
    public PaymentTransactionResponse getTransactionById(Long id) {
        PaymentTransaction tx = transactionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Transaction not found with ID: " + id));
        return mapper.toResponse(tx);
    }

    @Override
    public PaymentTransactionResponse getLatestTransactionForUser(Long userId) {
        PaymentTransaction tx = transactionRepository.findFirstByUserIdOrderByCreatedAtDesc(userId);
        if (tx == null) {
            throw new NotFoundException("No transactions found for user: " + userId);
        }
        return mapper.toResponse(tx);
    }

    @Override
    public Map<String, Object> getTransactionStats() {
        List<PaymentTransaction> allTransactions = transactionRepository.findAll();

        long totalTransactions = allTransactions.size();
        long successfulTransactions = 0;
        long failedTransactions = 0;
        long pendingTransactions = 0;
        long cancelledTransactions = 0;
        long totalRevenue = 0;

        for (PaymentTransaction tx : allTransactions) {
            switch (tx.getStatus()) {
                case "SUCCESS" -> {
                    successfulTransactions++;
                    totalRevenue += tx.getAmount();
                }
                case "FAILED" -> failedTransactions++;
                case "CANCELLED" -> cancelledTransactions++;
                default -> pendingTransactions++;
            }
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalTransactions", totalTransactions);
        stats.put("successfulTransactions", successfulTransactions);
        stats.put("failedTransactions", failedTransactions);
        stats.put("pendingTransactions", pendingTransactions);
        stats.put("cancelledTransactions", cancelledTransactions);
        stats.put("totalRevenue", totalRevenue);
        return stats;
    }

    /**
     * Periodic task that finds any payment transactions still marked as
     * PENDING after we've waited the maximum amount of time allowed by the
     * VNPAY integration.  According to the spec this is 15 minutes; anything
     * older is no longer considered "pending" and should be stamped expired
     * so administrators don't keep chasing ghost payments.
     * <p>
     * This method is executed automatically by Spring's scheduler (enabled
     * on this bean) every five minutes.  The actual update is performed via
     * a single bulk query in the repository.  A log entry is emitted when we
     * actually touch any rows so that ops can audit the behavior.
     */
    @Scheduled(fixedRate = 5 * 60 * 1000) // every 5 minutes
    @Transactional
    public void expirePendingTransactions() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(15);
        int updated = transactionRepository.expireOldTransactions(cutoff);
        if (updated > 0) {
            log.info("Expired {} pending payment transactions older than {}", updated, cutoff);
        }
    }
}
