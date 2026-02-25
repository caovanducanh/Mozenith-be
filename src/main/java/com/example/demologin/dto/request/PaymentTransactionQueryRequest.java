package com.example.demologin.dto.request;

import lombok.Data;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

@Data
public class PaymentTransactionQueryRequest {
    private Long userId;
    private String txnRef;
    private Long id;
    private String status;
    // date strings in yyyy-MM-dd format; controller will convert to LocalDateTime
    private String startDate;
    private String endDate;
    private String sortBy;      // column name to sort on
    private String sortDir;     // ASC or DESC
}
