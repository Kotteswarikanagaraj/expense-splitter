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
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "expenses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @Column(nullable = false, length = 255)
    private String description;

    // BigDecimal, never double/float, for money — avoids binary floating point
    // rounding errors (classic interview question: "why not double for money?")
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paid_by", nullable = false)
    private User paidBy;

    @Enumerated(EnumType.STRING) // store "EQUAL"/"EXACT"/"PERCENTAGE", not 0/1/2 ordinal
    @Column(nullable = false, length = 20)
    private SplitType splitType;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    // OneToMany owned by ExpenseShare (mappedBy = "expense" -> ExpenseShare holds the FK).
    // cascade = ALL + orphanRemoval so that when we build shares for an expense and save
    // the expense, the shares are persisted automatically in the same transaction.
    @Builder.Default
    @OneToMany(mappedBy = "expense", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ExpenseShare> shares = new ArrayList<>();

    public void addShare(ExpenseShare share) {
        shares.add(share);
        share.setExpense(this);
    }
}
