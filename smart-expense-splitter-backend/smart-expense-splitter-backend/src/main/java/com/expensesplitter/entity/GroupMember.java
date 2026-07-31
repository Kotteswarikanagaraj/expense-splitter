package com.expensesplitter.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Join entity representing the many-to-many relationship between User and Group,
 * with an extra attribute (joinedAt). A plain @ManyToMany can't carry that extra
 * column, so we model the join table as its own entity instead — this is the
 * standard JPA pattern whenever a join table needs metadata of its own.
 */
@Entity
@Table(name = "group_members")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GroupMember {

    @EmbeddedId
    private GroupMemberId id;

    // @MapsId tells JPA: "the groupId field inside the embedded id IS the FK
    // to Group.id" — so we don't duplicate the column, and we get a real
    // navigable relationship (member.getGroup().getName()) for free.
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("groupId")
    @JoinColumn(name = "group_id")
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime joinedAt;

    public GroupMember(Group group, User user) {
        this.group = group;
        this.user = user;
        this.id = new GroupMemberId(group.getId(), user.getId());
    }
}
