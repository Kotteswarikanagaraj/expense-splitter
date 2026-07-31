package com.expensesplitter.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class GroupMemberResponse {
    private Long userId;
    private String name;
    private String email;
    private LocalDateTime joinedAt;
}
