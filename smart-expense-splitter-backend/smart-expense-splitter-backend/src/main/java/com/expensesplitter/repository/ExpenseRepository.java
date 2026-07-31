package com.expensesplitter.repository;

import com.expensesplitter.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

// JpaSpecificationExecutor adds findAll(Specification, Pageable) — lets us build
// the WHERE clause dynamically at runtime based on which filters were actually
// passed in, instead of writing a separate query method for every combination
// of filters (paidBy alone, paidBy+splitType, splitType+dateRange, etc.)
public interface ExpenseRepository extends JpaRepository<Expense, Long>,
        JpaSpecificationExecutor<Expense> {

    List<Expense> findByGroup_IdOrderByCreatedAtDesc(Long groupId);

    // Object[] here is [Long userId, BigDecimal totalPaid] per row.
    // GROUP BY collapses all of a user's expenses in this group into one sum.
    @Query("SELECT e.paidBy.id, SUM(e.amount) FROM Expense e WHERE e.group.id = :groupId GROUP BY e.paidBy.id")
    List<Object[]> sumPaidByUserInGroup(@Param("groupId") Long groupId);
}
