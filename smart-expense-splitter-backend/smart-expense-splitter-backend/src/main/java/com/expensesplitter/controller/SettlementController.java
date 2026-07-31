package com.expensesplitter.controller;

import com.expensesplitter.dto.response.BalanceResponse;
import com.expensesplitter.dto.response.SettlementResponse;
import com.expensesplitter.service.SettlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/groups/{groupId}")
@RequiredArgsConstructor
public class SettlementController {

    private final SettlementService settlementService;

    // Raw per-user balances — "who's owed what" before simplification.
    @GetMapping("/balances")
    public List<BalanceResponse> getBalances(@PathVariable Long groupId) {
        return settlementService.getBalances(groupId);
    }

    // Recomputes balances from current expenses + settled history, and returns
    // the minimal set of pending "who pays whom" transactions.
    @GetMapping("/settlements")
    public List<SettlementResponse> getSimplifiedSettlements(@PathVariable Long groupId) {
        return settlementService.getSimplifiedSettlements(groupId);
    }

    @PostMapping("/settlements/{settlementId}/settle")
    public SettlementResponse settle(@PathVariable Long groupId, @PathVariable Long settlementId) {
        return settlementService.markAsSettled(groupId, settlementId);
    }
}
