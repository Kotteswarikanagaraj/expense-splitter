package com.expensesplitter.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Every error response from the API (validation failure, not-found, auth failure,
 * unexpected exception) is shaped like this, so the React frontend has exactly ONE
 * error format to parse regardless of what went wrong.
 */
@Getter
@Builder
@AllArgsConstructor
public class ApiError {
    private LocalDateTime timestamp;
    private int status;
    private String error;      // e.g. "Not Found", "Bad Request"
    private String message;    // human readable detail
    private String path;       // request URI that failed
    private List<String> validationErrors; // populated only for 400 validation failures
}
