package com.gasto.application.expense.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record SyncRequest(
        @NotEmpty(message = "Expenses list must not be empty")
        @Valid
        List<ExpenseRequest> expenses
) {}
