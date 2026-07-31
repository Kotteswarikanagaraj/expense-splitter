package com.expensesplitter.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class GroupResponse {
    private Long id;
    private String name;
    private Long createdBy;
    private String createdByName;
    private LocalDateTime createdAt;
    private List<GroupMemberResponse> members;
}
