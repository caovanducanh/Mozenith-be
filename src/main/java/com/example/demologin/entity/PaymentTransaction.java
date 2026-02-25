package com.example.demologin.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Records the details of a payment transaction initiated via VNPAY.  We store
 * both the parameters that we send (amount, txnRef etc) and the values we
 * receive back in the IPN callback so that administrators can audit payments
 * and troubleshoot failures.
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

    /** user who initiated the payment (may be null for guest flows) */
    @Column(nullable = true)
    private Long userId;

    /** reference we supplied to VNPAY, typically "{userId}_{timestamp}" */
    @Column(nullable = false, length = 100, unique = true)
    private String txnRef;

    /** amount in smallest currency unit (VND * 100) */
    @Column(nullable = false)
    private Long amount;

    /** status tracked locally: PENDING, SUCCESS, FAILED, etc. */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING";

    // ---- fields returned by VNPAY IPN -------------------------------
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
