package com.example.demologin.mapper;

import com.example.demologin.dto.response.PaymentTransactionResponse;
import com.example.demologin.entity.PaymentTransaction;
import com.example.demologin.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentTransactionMapper {

    private final UserRepository userRepository;

    public PaymentTransactionResponse toResponse(PaymentTransaction tx) {
        PaymentTransactionResponse r = new PaymentTransactionResponse();
        r.setId(tx.getId());
        r.setUserId(tx.getUserId());
        r.setTxnRef(tx.getTxnRef());
        r.setAmount(tx.getAmount());
        r.setStatus(tx.getStatus());
        r.setVnpResponseCode(tx.getVnpResponseCode());
        r.setVnpTransactionStatus(tx.getVnpTransactionStatus());
        r.setVnpBankCode(tx.getVnpBankCode());
        r.setVnpBankTranNo(tx.getVnpBankTranNo());
        r.setVnpCardType(tx.getVnpCardType());
        r.setVnpOrderInfo(tx.getVnpOrderInfo());
        r.setVnpTransactionNo(tx.getVnpTransactionNo());
        r.setVnpPayDate(tx.getVnpPayDate());
        r.setCreatedAt(tx.getCreatedAt());

        // optionally include username if needed
        if (tx.getUserId() != null) {
            userRepository.findById(tx.getUserId())
                    .ifPresent(user -> r.setTxnRef(user.getUsername() + " (" + r.getTxnRef() + ")"));
        }
        return r;
    }
}
