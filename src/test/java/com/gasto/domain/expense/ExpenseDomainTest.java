package com.gasto.domain.expense;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ExpenseDomainTest {

    @Test
    void isDeleted_returnsFalse_whenDeletedAtIsNull() {
        Expense expense = Expense.builder()
                .amount(BigDecimal.TEN)
                .expenseDate(LocalDate.now())
                .build();

        assertThat(expense.isDeleted()).isFalse();
    }

    @Test
    void softDelete_setsDeletedAt() {
        Expense expense = Expense.builder()
                .amount(BigDecimal.TEN)
                .expenseDate(LocalDate.now())
                .build();

        expense.softDelete();

        assertThat(expense.isDeleted()).isTrue();
        assertThat(expense.getDeletedAt()).isNotNull();
    }

    @Test
    void softDelete_canBeCalledMultipleTimes_withoutError() {
        Expense expense = Expense.builder().amount(BigDecimal.ONE).expenseDate(LocalDate.now()).build();

        expense.softDelete();
        expense.softDelete();

        assertThat(expense.isDeleted()).isTrue();
    }
}
