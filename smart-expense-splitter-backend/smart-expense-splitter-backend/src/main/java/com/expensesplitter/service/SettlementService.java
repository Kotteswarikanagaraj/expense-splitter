package com.expensesplitter.service;

import com.expensesplitter.dto.response.SettlementResponse;
import com.expensesplitter.entity.Group;
import com.expensesplitter.entity.GroupMember;
import com.expensesplitter.entity.Settlement;
import com.expensesplitter.entity.User;
import com.expensesplitter.exception.BadRequestException;
import com.expensesplitter.exception.ResourceNotFoundException;
import com.expensesplitter.repository.ExpenseRepository;
import com.expensesplitter.repository.ExpenseShareRepository;
import com.expensesplitter.repository.GroupMemberRepository;
import com.expensesplitter.repository.SettlementRepository;
import com.expensesplitter.security.SecurityUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SettlementService {

    private final SettlementRepository settlementRepository;
    private final ExpenseRepository expenseRepository;
    private final ExpenseShareRepository expenseShareRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupService groupService;
    private final UserService userService;
    private final GroupNotificationService notificationService;

    /**
     * Recomputes net balances from scratch and returns the minimal set of
     * "who should pay whom, how much" transactions to clear all debts.
     * Old pending (unsettled) suggestions for this group are discarded first,
     * since they're fully derived from current data — nothing is lost, because
     * settled settlements are preserved and factored back into the balances.
     */
    @Transactional
    public List<SettlementResponse> getSimplifiedSettlements(Long groupId) {
        Group group = groupService.getGroupOrThrow(groupId);
        assertRequesterIsMember(groupId);

        Map<Long, BigDecimal> balances = computeNetBalances(groupId);

        settlementRepository.deletePendingByGroupId(groupId);
        List<Settlement> newSettlements = simplifyDebts(group, balances);
        List<Settlement> saved = settlementRepository.saveAll(newSettlements);

        return saved.stream().map(this::toResponse).toList();
    }

    @Transactional
    public SettlementResponse markAsSettled(Long groupId, Long settlementId) {
        groupService.getGroupOrThrow(groupId);
        assertRequesterIsMember(groupId);

        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new ResourceNotFoundException("Settlement not found with id: " + settlementId));

        if (!settlement.getGroup().getId().equals(groupId)) {
            throw new BadRequestException("Settlement " + settlementId + " does not belong to group " + groupId);
        }
        if (settlement.isSettled()) {
            throw new BadRequestException("Settlement " + settlementId + " is already marked as paid");
        }

        settlement.setSettled(true);
        settlement.setSettledAt(LocalDateTime.now());
        Settlement saved = settlementRepository.save(settlement);
        SettlementResponse response = toResponse(saved);

        // Balances have shifted, so the frontend should refetch both the
        // settlement list and the balances view for this group.
        notificationService.broadcast(groupId, com.expensesplitter.dto.ws.WsEventType.SETTLEMENT_UPDATED, response);

        return response;
    }

    /**
     * Raw balances (no simplification) — how much each member is currently
     * owed or owes, before collapsing it into a minimal payment list.
     */
    @Transactional
    public List<com.expensesplitter.dto.response.BalanceResponse> getBalances(Long groupId) {
        groupService.getGroupOrThrow(groupId);
        assertRequesterIsMember(groupId);

        Map<Long, BigDecimal> balances = computeNetBalances(groupId);

        return balances.entrySet().stream()
                .map(e -> com.expensesplitter.dto.response.BalanceResponse.builder()
                        .userId(e.getKey())
                        .userName(userService.getUserEntityById(e.getKey()).getName())
                        .balance(e.getValue().setScale(2, java.math.RoundingMode.HALF_UP))
                        .build())
                .sorted(Comparator.comparing(com.expensesplitter.dto.response.BalanceResponse::getBalance).reversed())
                .toList();
    }

    // ---------------- balance calculation ----------------

    /**
     * balance[user] = (total the user PAID across group expenses)
     *               - (total the user OWES across group expense shares)
     *               + (settled settlements where user was the payer, i.e. debt they already cleared)
     *               - (settled settlements where user was the receiver, i.e. credit they already collected)
     *
     * Positive => this person is owed money overall. Negative => this person owes money overall.
     */
    private Map<Long, BigDecimal> computeNetBalances(Long groupId) {
        Map<Long, BigDecimal> balances = new HashMap<>();

        // Seed every current group member at 0 so someone who neither paid nor
        // owes anything still shows up (and so we never NPE on a missing key).
        for (GroupMember gm : groupMemberRepository.findByGroup_Id(groupId)) {
            balances.put(gm.getUser().getId(), BigDecimal.ZERO.setScale(2));
        }

        for (Object[] row : expenseRepository.sumPaidByUserInGroup(groupId)) {
            Long userId = (Long) row[0];
            BigDecimal paid = (BigDecimal) row[1];
            balances.merge(userId, paid, BigDecimal::add);
        }

        for (Object[] row : expenseShareRepository.sumOwedByUserInGroup(groupId)) {
            Long userId = (Long) row[0];
            BigDecimal owed = (BigDecimal) row[1];
            balances.merge(userId, owed.negate(), BigDecimal::add);
        }

        for (Settlement s : settlementRepository.findByGroup_IdAndSettledTrue(groupId)) {
            balances.merge(s.getFromUser().getId(), s.getAmount(), BigDecimal::add);
            balances.merge(s.getToUser().getId(), s.getAmount().negate(), BigDecimal::add);
        }

        return balances;
    }

    // ---------------- greedy debt simplification ----------------

    /**
     * Two-pointer greedy match: largest debtor against largest creditor, repeatedly.
     * Each iteration fully clears at least one side, so this terminates in at most
     * N-1 transactions for N people with a nonzero balance. O(N log N) overall,
     * dominated by the initial sort (the matching sweep itself is O(N)).
     */
    private List<Settlement> simplifyDebts(Group group, Map<Long, BigDecimal> balances) {
        List<Balance> creditors = new ArrayList<>();
        List<Balance> debtors = new ArrayList<>();

        for (Map.Entry<Long, BigDecimal> entry : balances.entrySet()) {
            BigDecimal amount = entry.getValue().setScale(2, java.math.RoundingMode.HALF_UP);
            int cmp = amount.compareTo(BigDecimal.ZERO);
            if (cmp > 0) {
                creditors.add(new Balance(entry.getKey(), amount));
            } else if (cmp < 0) {
                debtors.add(new Balance(entry.getKey(), amount.abs()));
            }
            // cmp == 0 -> already settled, nothing to do
        }

        // Biggest amounts first on both sides — this is what guarantees we clear
        // at least one person per step instead of dribbling out lots of tiny payments.
        creditors.sort(Comparator.comparing(Balance::getAmount).reversed());
        debtors.sort(Comparator.comparing(Balance::getAmount).reversed());

        List<Settlement> result = new ArrayList<>();
        int i = 0; // pointer into debtors
        int j = 0; // pointer into creditors

        while (i < debtors.size() && j < creditors.size()) {
            Balance debtor = debtors.get(i);
            Balance creditor = creditors.get(j);

            BigDecimal settleAmount = debtor.getAmount().min(creditor.getAmount());

            if (settleAmount.compareTo(BigDecimal.ZERO) > 0) {
                result.add(Settlement.builder()
                        .group(group)
                        .fromUser(userService.getUserEntityById(debtor.getUserId()))
                        .toUser(userService.getUserEntityById(creditor.getUserId()))
                        .amount(settleAmount)
                        .settled(false)
                        .build());
            }

            debtor.setAmount(debtor.getAmount().subtract(settleAmount));
            creditor.setAmount(creditor.getAmount().subtract(settleAmount));

            if (debtor.getAmount().compareTo(BigDecimal.ZERO) == 0) i++;
            if (creditor.getAmount().compareTo(BigDecimal.ZERO) == 0) j++;
        }

        return result;
    }

    private void assertRequesterIsMember(Long groupId) {
        User requester = userService.getUserEntityByEmail(SecurityUtil.getCurrentUserEmail());
        groupService.assertIsMember(groupId, requester.getId());
    }

    private SettlementResponse toResponse(Settlement s) {
        return SettlementResponse.builder()
                .id(s.getId())
                .groupId(s.getGroup().getId())
                .fromUserId(s.getFromUser().getId())
                .fromUserName(s.getFromUser().getName())
                .toUserId(s.getToUser().getId())
                .toUserName(s.getToUser().getName())
                .amount(s.getAmount())
                .settled(s.isSettled())
                .createdAt(s.getCreatedAt())
                .settledAt(s.getSettledAt())
                .build();
    }

    // Small mutable holder used only inside the greedy algorithm — deliberately
    // private/package-local, never exposed outside this class.
    @Getter
    @Setter
    @AllArgsConstructor
    private static class Balance {
        private Long userId;
        private BigDecimal amount;
    }
}
