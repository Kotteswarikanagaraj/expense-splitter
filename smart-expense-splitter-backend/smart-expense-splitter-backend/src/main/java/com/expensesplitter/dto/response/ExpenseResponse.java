package com.expensesplitter.dto.response;

import com.expensesplitter.entity.SplitType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class ExpenseResponse {
    private Long id;
    private Long groupId;
    private String description;
    private BigDecimal amount;
    private Long paidBy;
    private String paidByName;
    private SplitType splitType;
    private LocalDateTime createdAt;
    private List<ExpenseShareResponse> shares;
}
