package com.expensesplitter.service;

import com.expensesplitter.dto.request.CreateExpenseRequest;
import com.expensesplitter.dto.request.ShareInput;
import com.expensesplitter.dto.response.ExpenseResponse;
import com.expensesplitter.dto.response.ExpenseShareResponse;
import com.expensesplitter.dto.response.PageResponse;
import com.expensesplitter.entity.*;
import com.expensesplitter.exception.BadRequestException;
import com.expensesplitter.repository.ExpenseRepository;
import com.expensesplitter.repository.GroupMemberRepository;
import com.expensesplitter.security.SecurityUtil;
import com.expensesplitter.specification.ExpenseSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupService groupService;
    private final UserService userService;
    private final GroupNotificationService notificationService;

    @Transactional
    public ExpenseResponse addExpense(CreateExpenseRequest request) {
        Group group = groupService.getGroupOrThrow(request.getGroupId());

        // The payer is ALWAYS the person making this request — never someone
        // picked from a dropdown. This closes off a whole class of bugs/abuse
        // where user A could log an expense claiming user B paid for it.
        User paidBy = userService.getUserEntityByEmail(SecurityUtil.getCurrentUserEmail());

        groupService.assertIsMember(group.getId(), paidBy.getId());

        List<GroupMember> allMembers = groupMemberRepository.findByGroup_Id(group.getId());
        if (allMembers.isEmpty()) {
            throw new BadRequestException("Group has no members to split the expense between");
        }

        Expense expense = Expense.builder()
                .group(group)
                .description(request.getDescription())
                .amount(request.getAmount())
                .paidBy(paidBy)
                .splitType(request.getSplitType())
                .build();

        List<ExpenseShare> shares = switch (request.getSplitType()) {
            case EQUAL -> buildEqualShares(expense, request.getAmount(), allMembers);
            case EXACT -> buildExactShares(expense, request.getAmount(), request.getShares(), allMembers);
            case PERCENTAGE -> buildPercentageShares(expense, request.getAmount(), request.getShares(), allMembers);
        };

        shares.forEach(expense::addShare);

        Expense saved = expenseRepository.save(expense);
        ExpenseResponse response = toResponse(saved);

        // Broadcast to the group topic. The frontend inspects response.shares
        // itself to decide who owes what and shows a "you owe X" notice to
        // each affected member — see useGroupSocket handling in GroupDetailPage.
        notificationService.broadcast(group.getId(), com.expensesplitter.dto.ws.WsEventType.EXPENSE_ADDED, response);

        return response;
    }

    @Transactional(readOnly = true)
    public PageResponse<ExpenseResponse> getExpensesForGroup(Long groupId,
                                                              Pageable pageable,
                                                              Long paidBy,
                                                              SplitType splitType,
                                                              BigDecimal minAmount,
                                                              BigDecimal maxAmount,
                                                              LocalDateTime from,
                                                              LocalDateTime to,
                                                              String description) {
        Group group = groupService.getGroupOrThrow(groupId);
        User requester = userService.getUserEntityByEmail(SecurityUtil.getCurrentUserEmail());
        groupService.assertIsMember(group.getId(), requester.getId());

        Specification<Expense> spec = Specification
                .where(ExpenseSpecifications.belongsToGroup(groupId))
                .and(ExpenseSpecifications.paidByUser(paidBy))
                .and(ExpenseSpecifications.hasSplitType(splitType))
                .and(ExpenseSpecifications.amountAtLeast(minAmount))
                .and(ExpenseSpecifications.amountAtMost(maxAmount))
                .and(ExpenseSpecifications.createdAfter(from))
                .and(ExpenseSpecifications.createdBefore(to))
                .and(ExpenseSpecifications.descriptionContains(description));

        Page<Expense> page = expenseRepository.findAll(spec, pageable);
        List<ExpenseResponse> content = page.getContent().stream().map(this::toResponse).toList();

        return PageResponse.from(page, content);
    }

    private List<ExpenseShare> buildEqualShares(Expense expense, BigDecimal amount, List<GroupMember> members) {
        int count = members.size();
        BigDecimal baseShare = amount.divide(BigDecimal.valueOf(count), 2, RoundingMode.FLOOR);
        BigDecimal totalAssigned = baseShare.multiply(BigDecimal.valueOf(count));
        BigDecimal remainder = amount.subtract(totalAssigned);

        BigDecimal cent = new BigDecimal("0.01");
        int centsToDistribute = remainder.divide(cent).intValue();

        return buildShareList(expense, members, (member, index) -> {
            BigDecimal share = baseShare;
            if (index < centsToDistribute) {
                share = share.add(cent);
            }
            return share;
        });
    }

    private List<ExpenseShare> buildExactShares(Expense expense, BigDecimal amount,
                                                 List<ShareInput> shareInputs, List<GroupMember> members) {
        if (shareInputs == null || shareInputs.isEmpty()) {
            throw new BadRequestException("shares are required for EXACT split");
        }
        validateAllMembersCovered(shareInputs, members);

        BigDecimal sum = shareInputs.stream().map(ShareInput::getValue).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (sum.setScale(2, RoundingMode.HALF_UP).compareTo(amount.setScale(2, RoundingMode.HALF_UP)) != 0) {
            throw new BadRequestException("Exact shares (" + sum + ") must sum to the expense amount (" + amount + ")");
        }

        return shareInputs.stream()
                .map(si -> ExpenseShare.builder()
                        .expense(expense)
                        .user(userService.getUserEntityById(si.getUserId()))
                        .shareAmount(si.getValue().setScale(2, RoundingMode.HALF_UP))
                        .build())
                .toList();
    }

    private List<ExpenseShare> buildPercentageShares(Expense expense, BigDecimal amount,
                                                       List<ShareInput> shareInputs, List<GroupMember> members) {
        if (shareInputs == null || shareInputs.isEmpty()) {
            throw new BadRequestException("shares are required for PERCENTAGE split");
        }
        validateAllMembersCovered(shareInputs, members);

        BigDecimal totalPct = shareInputs.stream().map(ShareInput::getValue).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalPct.compareTo(BigDecimal.valueOf(100)) != 0) {
            throw new BadRequestException("Percentages must sum to 100, got " + totalPct);
        }

        return shareInputs.stream()
                .map(si -> {
                    BigDecimal share = amount
                            .multiply(si.getValue())
                            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                    return ExpenseShare.builder()
                            .expense(expense)
                            .user(userService.getUserEntityById(si.getUserId()))
                            .shareAmount(share)
                            .build();
                })
                .toList();
    }

    private void validateAllMembersCovered(List<ShareInput> shareInputs, List<GroupMember> members) {
        List<Long> memberIds = members.stream().map(m -> m.getUser().getId()).toList();
        List<Long> shareUserIds = shareInputs.stream().map(ShareInput::getUserId).toList();

        for (Long id : shareUserIds) {
            if (!memberIds.contains(id)) {
                throw new BadRequestException("User " + id + " is not a member of this group");
            }
        }
        if (shareUserIds.stream().distinct().count() != shareUserIds.size()) {
            throw new BadRequestException("Duplicate userId in shares");
        }
    }

    @FunctionalInterface
    private interface ShareAmountFn {
        BigDecimal apply(GroupMember member, int index);
    }

    private List<ExpenseShare> buildShareList(Expense expense, List<GroupMember> members, ShareAmountFn fn) {
        return java.util.stream.IntStream.range(0, members.size())
                .mapToObj(i -> {
                    GroupMember member = members.get(i);
                    return ExpenseShare.builder()
                            .expense(expense)
                            .user(member.getUser())
                            .shareAmount(fn.apply(member, i))
                            .build();
                })
                .toList();
    }

    private ExpenseResponse toResponse(Expense expense) {
        List<ExpenseShareResponse> shareResponses = expense.getShares().stream()
                .map(s -> ExpenseShareResponse.builder()
                        .userId(s.getUser().getId())
                        .userName(s.getUser().getName())
                        .shareAmount(s.getShareAmount())
                        .build())
                .toList();

        return ExpenseResponse.builder()
                .id(expense.getId())
                .groupId(expense.getGroup().getId())
                .description(expense.getDescription())
                .amount(expense.getAmount())
                .paidBy(expense.getPaidBy().getId())
                .paidByName(expense.getPaidBy().getName())
                .splitType(expense.getSplitType())
                .createdAt(expense.getCreatedAt())
                .shares(shareResponses)
                .build();
    }
}