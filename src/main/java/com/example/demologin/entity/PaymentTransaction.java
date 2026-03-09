package com.example.demologin.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Records the details of a payment transaction initiated via PayOS.
 */
@Entity
@Table(name = "payment_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** user who initiated the payment */
    @Column(nullable = true)
    private Long userId;

    /** reference we supplied, typically "{userId}_{orderCode}" */
    @Column(nullable = false, length = 100, unique = true)
    private String txnRef;

    /** amount in VND */
    @Column(nullable = false)
    private Long amount;

    /** status tracked locally: PENDING, SUCCESS, FAILED, CANCELLED */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING";

    // ---- fields from PayOS webhook / response -------------------------
    /** PayOS order code (numeric) */
    @Column(length = 50)
    private String orderCode;

    /** PayOS payment status code ("00" = success) */
    @Column(length = 10)
    private String payosCode;

    /** Description from PayOS */
    @Column(length = 255)
    private String payosDescription;

    /** PayOS transaction reference */
    @Column(length = 100)
    private String payosTransactionRef;

    /** Payment method / channel info from PayOS */
    @Column(length = 50)
    private String paymentMethod;

    /** Counter account number (payer bank account) */
    @Column(length = 50)
    private String counterAccountNumber;

    /** Counter account name (payer name) */
    @Column(length = 255)
    private String counterAccountName;

    // Keep legacy VNPay fields for historical data compatibility
    @Column(length = 10)
    private String vnpResponseCode;
    @Column(length = 10)
    private String vnpTransactionStatus;
    @Column(length = 20)
    private String vnpBankCode;
    @Column(length = 50)
    private String vnpBankTranNo;
    @Column(length = 20)
    private String vnpCardType;
    @Column(length = 255)
    private String vnpOrderInfo;
    @Column(length = 50)
    private String vnpTransactionNo;
    private LocalDateTime vnpPayDate;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
