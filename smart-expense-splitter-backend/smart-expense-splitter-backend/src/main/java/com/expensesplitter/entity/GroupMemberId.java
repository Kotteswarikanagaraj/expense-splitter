package com.expensesplitter.entity;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * Composite primary key for GroupMember (group_id + user_id).
 * Must implement Serializable and override equals()/hashCode() — JPA uses these
 * to look the entity up in the persistence context / L1 cache.
 * @EqualsAndHashCode is Lombok generating that for us.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class GroupMemberId implements Serializable {
    private Long groupId;
    private Long userId;
}
