package com.gasto.domain.expense;

import java.math.BigDecimal;
import java.util.UUID;

public record ExpenseSummary(
        UUID categoryId,
        String categoryName,
        String categoryIcon,
        BigDecimal total,
        long count
) {}
