package com.expensesplitter.specification;

import com.expensesplitter.entity.Expense;
import com.expensesplitter.entity.SplitType;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A Specification<Expense> is essentially a lambda that builds one WHERE-clause
 * predicate using the JPA Criteria API. The trick that makes filtering flexible:
 * Specification.where(null) is a no-op, and .and(null) is also a no-op — so we
 * can chain every possible filter unconditionally, and any filter the caller
 * didn't provide simply contributes nothing to the final query. This avoids
 * writing 2^N repository methods for every combination of optional filters.
 */
public class ExpenseSpecifications {

    private ExpenseSpecifications() {
    }

    public static Specification<Expense> belongsToGroup(Long groupId) {
        return (root, query, cb) -> cb.equal(root.get("group").get("id"), groupId);
    }

    public static Specification<Expense> paidByUser(Long userId) {
        if (userId == null) return null;
        return (root, query, cb) -> cb.equal(root.get("paidBy").get("id"), userId);
    }

    public static Specification<Expense> hasSplitType(SplitType splitType) {
        if (splitType == null) return null;
        return (root, query, cb) -> cb.equal(root.get("splitType"), splitType);
    }

    public static Specification<Expense> amountAtLeast(BigDecimal min) {
        if (min == null) return null;
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("amount"), min);
    }

    public static Specification<Expense> amountAtMost(BigDecimal max) {
        if (max == null) return null;
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("amount"), max);
    }

    public static Specification<Expense> createdAfter(LocalDateTime from) {
        if (from == null) return null;
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from);
    }

    public static Specification<Expense> createdBefore(LocalDateTime to) {
        if (to == null) return null;
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), to);
    }

    public static Specification<Expense> descriptionContains(String keyword) {
        if (keyword == null || keyword.isBlank()) return null;
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("description")), "%" + keyword.toLowerCase() + "%");
    }
}
