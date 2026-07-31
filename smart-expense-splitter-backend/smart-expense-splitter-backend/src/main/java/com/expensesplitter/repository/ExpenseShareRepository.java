package com.expensesplitter.repository;

import com.expensesplitter.entity.ExpenseShare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ExpenseShareRepository extends JpaRepository<ExpenseShare, Long> {

    List<ExpenseShare> findByExpense_Id(Long expenseId);

    // [Long userId, BigDecimal totalOwed] per row, across every expense in the group.
    // Note the join through es.expense.group.id — ExpenseShare doesn't have a direct
    // group reference, so we navigate expense -> group in the JPQL path expression.
    @Query("SELECT es.user.id, SUM(es.shareAmount) FROM ExpenseShare es " +
            "WHERE es.expense.group.id = :groupId GROUP BY es.user.id")
    List<Object[]> sumOwedByUserInGroup(@Param("groupId") Long groupId);
}
