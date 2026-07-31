package com.expensesplitter.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Records that fromUser owes -> should pay amount to toUser inside a group,
 * to settle up a debt. These rows have a lifecycle:
 *   1. Generated as a PENDING suggestion by the debt-simplification algorithm
 *      (settled = false, settledAt = null).
 *   2. Marked SETTLED once the payment actually happens in real life
 *      (settled = true, settledAt = now).
 * Pending suggestions are recalculated fresh every time /settlements is called
 * (old pending ones for the group are cleared first) since they're derived data,
 * not a source of truth. Settled rows are historical record and are never deleted.
 */
@Entity
@Table(name = "settlements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Settlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_user", nullable = false)
    private User fromUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_user", nullable = false)
    private User toUser;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Builder.Default
    @Column(nullable = false)
    private boolean settled = false;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    // Null until the settlement is actually marked paid
    private LocalDateTime settledAt;
}
