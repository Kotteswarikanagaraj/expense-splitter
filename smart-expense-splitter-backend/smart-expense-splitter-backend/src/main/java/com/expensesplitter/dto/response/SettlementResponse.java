package com.expensesplitter.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class SettlementResponse {
    private Long id;
    private Long groupId;
    private Long fromUserId;
    private String fromUserName;
    private Long toUserId;
    private String toUserName;
    private BigDecimal amount;
    private boolean settled;
    private LocalDateTime createdAt;
    private LocalDateTime settledAt;
}
