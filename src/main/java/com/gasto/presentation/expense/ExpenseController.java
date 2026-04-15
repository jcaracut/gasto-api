package com.gasto.presentation.expense;

import com.gasto.application.expense.ExpenseService;
import com.gasto.application.expense.dto.*;
import com.gasto.presentation.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;

    @GetMapping
    public ApiResponse<List<ExpenseResponse>> getExpenses(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(expenseService.getExpenses(currentUserId(principal), from, to));
    }

    @GetMapping("/{id}")
    public ApiResponse<ExpenseResponse> getExpense(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable UUID id) {
        return ApiResponse.ok(expenseService.getExpense(currentUserId(principal), id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ExpenseResponse> createExpense(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody ExpenseRequest request) {
        return ApiResponse.ok(expenseService.createExpense(currentUserId(principal), request), "Expense created");
    }

    @PutMapping("/{id}")
    public ApiResponse<ExpenseResponse> updateExpense(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable UUID id,
            @Valid @RequestBody ExpenseRequest request) {
        return ApiResponse.ok(expenseService.updateExpense(currentUserId(principal), id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteExpense(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable UUID id) {
        expenseService.deleteExpense(currentUserId(principal), id);
    }

    @PostMapping("/sync")
    public ApiResponse<List<ExpenseResponse>> syncExpenses(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody SyncRequest request) {
        return ApiResponse.ok(
                expenseService.syncExpenses(currentUserId(principal), request),
                "Sync completed");
    }

    @GetMapping("/summary")
    public ApiResponse<List<ExpenseSummaryResponse>> getMonthlySummary(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam(defaultValue = "#{T(java.time.LocalDate).now().getYear()}") int year,
            @RequestParam(defaultValue = "#{T(java.time.LocalDate).now().getMonthValue()}") @Min(1) @Max(12) int month) {
        return ApiResponse.ok(expenseService.getMonthlySummary(currentUserId(principal), year, month));
    }

    private UUID currentUserId(UserDetails principal) {
        return UUID.fromString(principal.getUsername());
    }
}
