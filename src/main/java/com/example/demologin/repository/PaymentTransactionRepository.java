package com.example.demologin.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.example.demologin.entity.PaymentTransaction;
// repository interface declaration
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    // simple filters
    PaymentTransaction findByTxnRef(String txnRef);
    Page<PaymentTransaction> findByUserId(Long userId, Pageable pageable);
    Page<PaymentTransaction> findByStatus(String status, Pageable pageable);

    @Query("SELECT p FROM PaymentTransaction p WHERE p.vnpPayDate BETWEEN :start AND :end")
    Page<PaymentTransaction> findByPayDateBetween(@Param("start") LocalDateTime start,
                                                  @Param("end") LocalDateTime end,
                                                  Pageable pageable);

    // combined search accommodating nulls; order by createdAt desc by default
        @Query("SELECT p FROM PaymentTransaction p WHERE " +
            "(:id IS NULL OR p.id = :id) AND " +
            "(:userId IS NULL OR p.userId = :userId) AND " +
            "(:txnRef IS NULL OR p.txnRef = :txnRef) AND " +
            "(:status IS NULL OR p.status = :status) AND " +
            "(:start IS NULL OR p.vnpPayDate >= :start) AND " +
            "(:end IS NULL OR p.vnpPayDate <= :end)")
    Page<PaymentTransaction> findWithFilters(@Param("userId") Long userId,
                                             @Param("txnRef") String txnRef,
                                             @Param("status") String status,
                               @Param("id") Long id,
                                             @Param("start") LocalDateTime start,
                                             @Param("end") LocalDateTime end,
                                             Pageable pageable);

    List<PaymentTransaction> findByUserId(Long userId);
}
