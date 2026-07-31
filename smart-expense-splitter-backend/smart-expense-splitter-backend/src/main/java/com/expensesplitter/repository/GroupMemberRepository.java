package com.expensesplitter.repository;

import com.expensesplitter.entity.GroupMember;
import com.expensesplitter.entity.GroupMemberId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GroupMemberRepository extends JpaRepository<GroupMember, GroupMemberId> {

    boolean existsByGroup_IdAndUser_Id(Long groupId, Long userId);

    List<GroupMember> findByGroup_Id(Long groupId);

    // "List my groups" needs GroupMember rows for the current user, each carrying
    // its parent Group. JOIN FETCH avoids the classic N+1 (one query per group
    // to fetch group details) by pulling Group in the same query.
    @Query("SELECT gm FROM GroupMember gm JOIN FETCH gm.group WHERE gm.user.id = :userId")
    List<GroupMember> findMyGroups(@Param("userId") Long userId);
}
