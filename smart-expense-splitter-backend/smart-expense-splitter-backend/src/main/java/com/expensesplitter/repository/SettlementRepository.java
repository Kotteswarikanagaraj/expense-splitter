package com.expensesplitter.repository;

import com.expensesplitter.entity.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    List<Settlement> findByGroup_IdOrderByCreatedAtDesc(Long groupId);

    // All settled settlements for a group — used to adjust net balances
    // (a settled payment moves the group's balances toward zero).
    List<Settlement> findByGroup_IdAndSettledTrue(Long groupId);

    // Since pending suggestions are derived/recalculable, we wipe the old batch
    // before inserting a freshly computed one on every GET /settlements call.
    // @Modifying is required for any @Query that isn't a SELECT (Spring Data JPA
    // won't let a bulk update/delete run without it, as a safety guard).
    @Modifying
    @Query("DELETE FROM Settlement s WHERE s.group.id = :groupId AND s.settled = false")
    void deletePendingByGroupId(@Param("groupId") Long groupId);
}
