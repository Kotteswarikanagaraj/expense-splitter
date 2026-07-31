package com.expensesplitter.dto.ws;

/**
 * What kind of thing changed. The frontend switches on this to decide whether
 * to refetch the expense list, the balances, or both.
 */
public enum WsEventType {
    EXPENSE_ADDED,
    SETTLEMENT_UPDATED
}
