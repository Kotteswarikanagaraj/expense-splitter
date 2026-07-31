package com.expensesplitter.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Deliberately excludes 'password' — this is the whole point of DTOs: the entity
 * never leaves the service layer, so there's no risk of accidentally serializing
 * a password hash into a JSON response.
 */
@Getter
@Builder
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String name;
    private String email;
}
