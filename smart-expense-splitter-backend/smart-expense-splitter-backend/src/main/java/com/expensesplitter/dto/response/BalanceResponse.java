package com.expensesplitter.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Raw "who's owed what" per group member — distinct from SettlementResponse,
 * which is the simplified list of suggested payments. The frontend's balances
 * view uses this; the settlements view uses SettlementResponse. Positive
 * balance = this person is owed money; negative = this person owes money.
 */
@Getter
@Builder
@AllArgsConstructor
public class BalanceResponse {
    private Long userId;
    private String userName;
    private BigDecimal balance;
}
