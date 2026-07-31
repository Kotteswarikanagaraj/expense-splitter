package com.expensesplitter.dto.request;

import com.expensesplitter.entity.SplitType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class CreateExpenseRequest {

    @NotNull(message = "groupId is required")
    private Long groupId;

    @NotBlank(message = "description is required")
    private String description;

    @NotNull(message = "amount is required")
    @DecimalMin(value = "0.01", message = "amount must be greater than 0")
    private BigDecimal amount;

    @NotNull(message = "splitType is required")
    private SplitType splitType;

    // Required only when splitType is EXACT or PERCENTAGE; null/ignored for EQUAL.
    @Valid
    private List<ShareInput> shares;
}