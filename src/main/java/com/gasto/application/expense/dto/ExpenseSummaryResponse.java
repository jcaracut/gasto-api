package com.gasto.application.expense.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ExpenseSummaryResponse(
        UUID categoryId,
        String categoryName,
        String categoryIcon,
        BigDecimal total,
        long count
) {}
