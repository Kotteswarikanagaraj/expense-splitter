package com.expensesplitter.controller;

import com.expensesplitter.dto.request.CreateExpenseRequest;
import com.expensesplitter.dto.response.ExpenseResponse;
import com.expensesplitter.dto.response.PageResponse;
import com.expensesplitter.entity.SplitType;
import com.expensesplitter.service.ExpenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;

    @PostMapping
    public ResponseEntity<ExpenseResponse> addExpense(@Valid @RequestBody CreateExpenseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(expenseService.addExpense(request));
    }

    /**
     * All query params are optional. Examples:
     *   GET /api/expenses/group/1?page=0&size=10
     *   GET /api/expenses/group/1?paidBy=2&splitType=EQUAL
     *   GET /api/expenses/group/1?minAmount=100&maxAmount=1000&sortBy=amount&sortDir=asc
     *   GET /api/expenses/group/1?description=dinner
     */
    @GetMapping("/group/{groupId}")
    public PageResponse<ExpenseResponse> getExpensesForGroup(
            @PathVariable Long groupId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) Long paidBy,
            @RequestParam(required = false) SplitType splitType,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) String description
    ) {
        Sort sort = Sort.by(sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        return expenseService.getExpensesForGroup(
                groupId, pageable, paidBy, splitType, minAmount, maxAmount, from, to, description);
    }
}
