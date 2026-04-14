package com.gasto.infrastructure.kafka;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ExpenseEvent(
        String eventType,
        UUID expenseId,
        UUID userId,
        UUID categoryId,
        String categoryName,
        BigDecimal amount,
        LocalDate expenseDate,
        OffsetDateTime occurredAt
) {
    public static final String EVENT_CREATED = "EXPENSE_CREATED";
    public static final String EVENT_UPDATED = "EXPENSE_UPDATED";
}
