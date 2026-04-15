package com.gasto.application.expense;

import com.gasto.application.expense.dto.*;
import com.gasto.domain.category.Category;
import com.gasto.domain.category.CategoryRepository;
import com.gasto.domain.exception.ResourceNotFoundException;
import com.gasto.domain.expense.Expense;
import com.gasto.domain.expense.ExpenseRepository;
import com.gasto.domain.expense.ExpenseSummary;
import com.gasto.domain.user.User;
import com.gasto.domain.user.UserRepository;
import com.gasto.infrastructure.kafka.ExpenseEventProducer;
import com.gasto.infrastructure.redis.ExpenseCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ExpenseCacheService cacheService;
    private final ExpenseEventProducer eventProducer;

    @Transactional(readOnly = true)
    public List<ExpenseResponse> getExpenses(UUID userId, LocalDate from, LocalDate to) {
        String cacheKey = cacheService.buildListKey(userId, from, to);
        List<ExpenseResponse> cached = cacheService.getExpenseList(cacheKey);
        if (cached != null) {
            log.debug("Returning cached expense list for user {}", userId);
            return cached;
        }

        LocalDate effectiveFrom = from != null ? from : LocalDate.now().withDayOfMonth(1);
        LocalDate effectiveTo = to != null ? to : LocalDate.now();

        List<ExpenseResponse> result = expenseRepository
                .findByUserIdAndDateRange(userId, effectiveFrom, effectiveTo)
                .stream()
                .filter(e -> !e.isDeleted())
                .map(ExpenseResponse::from)
                .toList();

        cacheService.putExpenseList(cacheKey, result);
        return result;
    }

    @Transactional(readOnly = true)
    public ExpenseResponse getExpense(UUID userId, UUID expenseId) {
        Expense expense = expenseRepository.findByIdAndUserId(expenseId, userId)
                .filter(e -> !e.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found"));
        return ExpenseResponse.from(expense);
    }

    @Transactional
    public ExpenseResponse createExpense(UUID userId, ExpenseRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + request.categoryId()));

        Expense expense = Expense.builder()
                .user(user)
                .category(category)
                .amount(request.amount())
                .description(request.description())
                .expenseDate(request.expenseDate())
                .build();

        expense = expenseRepository.save(expense);
        cacheService.evictUserCache(userId);
        eventProducer.publishExpenseCreated(expense);

        return ExpenseResponse.from(expense);
    }

    @Transactional
    public ExpenseResponse updateExpense(UUID userId, UUID expenseId, ExpenseRequest request) {
        Expense expense = expenseRepository.findByIdAndUserId(expenseId, userId)
                .filter(e -> !e.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found"));

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + request.categoryId()));

        expense.setCategory(category);
        expense.setAmount(request.amount());
        expense.setDescription(request.description());
        expense.setExpenseDate(request.expenseDate());

        expense = expenseRepository.save(expense);
        cacheService.evictUserCache(userId);
        eventProducer.publishExpenseUpdated(expense);

        return ExpenseResponse.from(expense);
    }

    @Transactional
    public void deleteExpense(UUID userId, UUID expenseId) {
        Expense expense = expenseRepository.findByIdAndUserId(expenseId, userId)
                .filter(e -> !e.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found"));

        expense.softDelete();
        expenseRepository.save(expense);
        cacheService.evictUserCache(userId);
    }

    @Transactional
    public List<ExpenseResponse> syncExpenses(UUID userId, SyncRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<ExpenseResponse> results = new ArrayList<>();

        for (ExpenseRequest dto : request.expenses()) {
            Category category = categoryRepository.findById(dto.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + dto.categoryId()));

            Expense expense;
            boolean isNew = false;

            if (dto.id() != null && expenseRepository.existsByIdAndUserId(dto.id(), userId)) {
                // update existing
                expense = expenseRepository.findByIdAndUserId(dto.id(), userId).get();
                expense.setCategory(category);
                expense.setAmount(dto.amount());
                expense.setDescription(dto.description());
                expense.setExpenseDate(dto.expenseDate());
                expense.setDeletedAt(null);
            } else {
                // create new; honour client-side UUID if provided to preserve idempotency
                expense = Expense.builder()
                        .id(dto.id())
                        .user(user)
                        .category(category)
                        .amount(dto.amount())
                        .description(dto.description())
                        .expenseDate(dto.expenseDate())
                        .build();
                isNew = true;
            }

            expense = expenseRepository.save(expense);
            results.add(ExpenseResponse.from(expense));

            if (isNew) {
                eventProducer.publishExpenseCreated(expense);
            } else {
                eventProducer.publishExpenseUpdated(expense);
            }
        }

        cacheService.evictUserCache(userId);
        return results;
    }

    @Transactional(readOnly = true)
    public List<ExpenseSummaryResponse> getMonthlySummary(UUID userId, int year, int month) {
        String cacheKey = cacheService.buildSummaryKey(userId, year, month);
        List<ExpenseSummaryResponse> cached = cacheService.getSummary(cacheKey);
        if (cached != null) {
            return cached;
        }

        List<ExpenseSummaryResponse> result = expenseRepository
                .findMonthlySummaryByUserId(userId, year, month)
                .stream()
                .map(s -> new ExpenseSummaryResponse(
                        s.categoryId(), s.categoryName(), s.categoryIcon(), s.total(), s.count()))
                .toList();

        cacheService.putSummary(cacheKey, result);
        return result;
    }
}
