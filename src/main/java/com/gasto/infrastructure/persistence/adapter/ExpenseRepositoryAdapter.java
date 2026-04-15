package com.gasto.infrastructure.persistence.adapter;

import com.gasto.domain.expense.Expense;
import com.gasto.domain.expense.ExpenseRepository;
import com.gasto.domain.expense.ExpenseSummary;
import com.gasto.infrastructure.persistence.jpa.JpaExpenseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ExpenseRepositoryAdapter implements ExpenseRepository {

    private final JpaExpenseRepository jpa;

    @Override
    public Expense save(Expense expense) {
        return jpa.save(expense);
    }

    @Override
    public Optional<Expense> findByIdAndUserId(UUID id, UUID userId) {
        return jpa.findByIdAndUserId(id, userId);
    }

    @Override
    public List<Expense> findByUserIdAndDateRange(UUID userId, LocalDate from, LocalDate to) {
        return jpa.findByUserIdAndDateRange(userId, from, to);
    }

    @Override
    public List<ExpenseSummary> findMonthlySummaryByUserId(UUID userId, int year, int month) {
        return jpa.findMonthlySummaryByUserId(userId, year, month);
    }

    @Override
    public boolean existsByIdAndUserId(UUID id, UUID userId) {
        return jpa.existsByIdAndUserId(id, userId);
    }
}
