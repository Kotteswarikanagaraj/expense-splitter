package com.expensesplitter.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
@AllArgsConstructor
public class ExpenseShareResponse {
    private Long userId;
    private String userName;
    private BigDecimal shareAmount;
}
