package com.expensesplitter.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Used only for EXACT and PERCENTAGE split types.
 * - EXACT: 'value' is the exact rupee/dollar amount this user owes.
 * - PERCENTAGE: 'value' is the percentage (0-100) this user owes.
 * Ignored/omitted entirely for EQUAL splits, since equal shares need no per-user input.
 */
@Getter
@Setter
public class ShareInput {

    @NotNull(message = "userId is required in each share")
    private Long userId;

    @NotNull(message = "value is required in each share")
    private BigDecimal value;
}
