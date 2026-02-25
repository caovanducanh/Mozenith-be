package com.example.demologin.serviceImpl;

import com.example.demologin.dto.request.PaymentTransactionQueryRequest;
import com.example.demologin.dto.response.PaymentTransactionResponse;
import com.example.demologin.entity.PaymentTransaction;
import com.example.demologin.exception.exceptions.NotFoundException;
import com.example.demologin.mapper.PaymentTransactionMapper;
import com.example.demologin.repository.PaymentTransactionRepository;
import com.example.demologin.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionServiceImpl implements TransactionService {

    private final PaymentTransactionRepository transactionRepository;
    private final PaymentTransactionMapper mapper;

    @Override
    @Transactional
    public void createPendingTransaction(Long userId, String txnRef, long amount) {
        PaymentTransaction tx = PaymentTransaction.builder()
                .userId(userId)
                .txnRef(txnRef)
                .amount(amount)
                .status("PENDING")
                .build();
        transactionRepository.save(tx);
    }

    @Override
    @Transactional
    public void recordIpn(Map<String, String> params) {
        String txnRef = params.get("vnp_TxnRef");
        if (txnRef == null) {
            log.warn("IPN callback without TxnRef: {}", params);
            return;
        }
        PaymentTransaction tx = transactionRepository.findByTxnRef(txnRef);
        if (tx == null) {
            tx = new PaymentTransaction();
            tx.setTxnRef(txnRef);
            // attempt to extract userId from prefix
            try {
                tx.setUserId(Long.parseLong(txnRef.split("_")[0]));
            } catch (Exception ignored) { }
        }
        // update fields from IPN
        tx.setVnpResponseCode(params.get("vnp_ResponseCode"));
        tx.setVnpTransactionStatus(params.get("vnp_TransactionStatus"));
        tx.setVnpBankCode(params.get("vnp_BankCode"));
        tx.setVnpBankTranNo(params.get("vnp_BankTranNo"));
        tx.setVnpCardType(params.get("vnp_CardType"));
        tx.setVnpOrderInfo(params.get("vnp_OrderInfo"));
        tx.setVnpTransactionNo(params.get("vnp_TransactionNo"));
        String payDate = params.get("vnp_PayDate");
        if (payDate != null) {
            try {
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
                tx.setVnpPayDate(LocalDateTime.parse(payDate, fmt));
            } catch (Exception e) {
                log.warn("Unable to parse pay date {}", payDate);
            }
        }
        String response = params.get("vnp_ResponseCode");
        String status = params.get("vnp_TransactionStatus");
        if ("00".equals(response) && "00".equals(status)) {
            tx.setStatus("SUCCESS");
        } else if (response != null || status != null) {
            tx.setStatus("FAILED");
        }
        transactionRepository.save(tx);
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
            // catch any persistence errors (missing table, bad query etc).
            // log for diagnostics and return empty page so caller can handle
            log.error("Transaction search failed", ex);
            return Page.empty();
        }
        if (results.getContent().isEmpty()) {
            // do not throw here; front end can show empty list instead of 500
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
}
