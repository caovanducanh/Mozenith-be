package com.example.demologin.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PaymentTransactionResponse {
    private Long id;
    private Long userId;
    private String txnRef;
    private Long amount;
    private String status;

    // PayOS fields
    private String orderCode;
    private String payosCode;
    private String payosDescription;
    private String payosTransactionRef;
    private String paymentMethod;
    private String counterAccountNumber;
    private String counterAccountName;

    // Legacy VNPay fields (for old transactions)
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
