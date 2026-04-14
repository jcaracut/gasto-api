package com.gasto.infrastructure.persistence.jpa;

import com.gasto.domain.expense.Expense;
import com.gasto.domain.expense.ExpenseSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaExpenseRepository extends JpaRepository<Expense, UUID> {

    @Query("""
            SELECT e FROM Expense e
            JOIN FETCH e.category
            WHERE e.user.id = :userId
              AND e.expenseDate BETWEEN :from AND :to
            ORDER BY e.expenseDate DESC, e.createdAt DESC
            """)
    List<Expense> findByUserIdAndDateRange(
            @Param("userId") UUID userId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    Optional<Expense> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByIdAndUserId(UUID id, UUID userId);

    @Query("""
            SELECT new com.gasto.domain.expense.ExpenseSummary(
                e.category.id,
                e.category.name,
                e.category.icon,
                SUM(e.amount),
                COUNT(e)
            )
            FROM Expense e
            WHERE e.user.id = :userId
              AND e.deletedAt IS NULL
              AND YEAR(e.expenseDate) = :year
              AND MONTH(e.expenseDate) = :month
            GROUP BY e.category.id, e.category.name, e.category.icon
            ORDER BY SUM(e.amount) DESC
            """)
    List<ExpenseSummary> findMonthlySummaryByUserId(
            @Param("userId") UUID userId,
            @Param("year") int year,
            @Param("month") int month);
}
