package com.example.demologin.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

import lombok.Data;

@Data
public class PaymentTransactionResponse {
    private Long id;
    private Long userId;
    private String txnRef;
    private Long amount;
    private String status;

    private String vnpResponseCode;
    private String vnpTransactionStatus;
    private String vnpBankCode;
    private String vnpBankTranNo;
    private String vnpCardType;
    private String vnpOrderInfo;
    private String vnpTransactionNo;
    private LocalDateTime vnpPayDate;

    private LocalDateTime createdAt;
}
