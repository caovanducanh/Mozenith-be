package com.example.demologin.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.example.demologin.annotation.ApiResponse;
import com.example.demologin.annotation.ApiResponse;
import com.example.demologin.annotation.PageResponse;
import com.example.demologin.annotation.SecuredEndpoint;
import com.example.demologin.dto.request.PaymentTransactionQueryRequest;
import com.example.demologin.service.TransactionService;
import com.example.demologin.annotation.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import lombok.RequiredArgsConstructor;

import com.example.demologin.annotation.ApiResponse;
import com.example.demologin.annotation.PageResponse;

import lombok.RequiredArgsConstructor;

import com.example.demologin.annotation.ApiResponse;
import com.example.demologin.annotation.PageResponse;

import lombok.RequiredArgsConstructor;

import com.example.demologin.annotation.ApiResponse;
import com.example.demologin.annotation.PageResponse;

import lombok.RequiredArgsConstructor;

import com.example.demologin.annotation.ApiResponse;
import com.example.demologin.annotation.PageResponse;

import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Tag(name = "Transaction Management", description = "APIs for viewing and searching payment transactions")
public class PaymentTransactionController {

    private final TransactionService transactionService;

    @GetMapping
    @PageResponse
    @ApiResponse(message = "Transactions retrieved successfully")
    @SecuredEndpoint("ADMIN_TRANSACTION_VIEW")
    @Operation(summary = "Search transactions", description = "Query payment transactions with filters, sorting and pagination")
    // all filter parameters are optional; only page/size are required for
    // pagination.  Using individual @RequestParam annotations allows the UI
    // to render a flat form instead of embedding a complex object.
    public Object searchTransactions(
            @RequestParam(required = false) String id,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String txnRef,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir) {

        PaymentTransactionQueryRequest request = new PaymentTransactionQueryRequest();
        if (id != null && !id.isEmpty()) {
            try { request.setId(Long.parseLong(id)); } catch (NumberFormatException e) {}
        }
        if (userId != null && !userId.isEmpty()) {
            try { request.setUserId(Long.parseLong(userId)); } catch (NumberFormatException e) {}
        }
        request.setTxnRef(txnRef);
        request.setStatus(status);
        request.setStartDate(startDate);
        request.setEndDate(endDate);
        request.setSortBy(sortBy);
        request.setSortDir(sortDir);

        return transactionService.searchTransactions(request, page, size);
    }

    // NOTE: individual retrieval is no longer required; clients can simply
    // supply `id` as a query parameter to the general search endpoint.  we
    // keep this method for backwards compatibility but it merely delegates.
    @GetMapping("/{id}")
    @ApiResponse(message = "Transaction retrieved successfully")
    @SecuredEndpoint("ADMIN_TRANSACTION_VIEW")
    @Operation(summary = "Get transaction by ID (legacy)", description = "Retrieve a specific payment transaction by its ID")
    public Object getTransactionById(
            @Parameter(description = "Transaction ID") @PathVariable Long id) {
        PaymentTransactionQueryRequest req = new PaymentTransactionQueryRequest();
        req.setId(id);
        // delegate to search so pagination, sorting logic is reused
        return transactionService.searchTransactions(req, 0, 1).getContent().stream().findFirst().orElse(null);
    }
}
