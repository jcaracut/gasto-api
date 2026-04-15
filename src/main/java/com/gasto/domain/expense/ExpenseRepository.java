package com.gasto.domain.expense;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExpenseRepository {
    Expense save(Expense expense);
    Optional<Expense> findByIdAndUserId(UUID id, UUID userId);
    List<Expense> findByUserIdAndDateRange(UUID userId, LocalDate from, LocalDate to);
    List<ExpenseSummary> findMonthlySummaryByUserId(UUID userId, int year, int month);
    boolean existsByIdAndUserId(UUID id, UUID userId);
}
