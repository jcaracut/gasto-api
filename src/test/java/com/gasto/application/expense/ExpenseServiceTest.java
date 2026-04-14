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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

    @Mock ExpenseRepository expenseRepository;
    @Mock UserRepository userRepository;
    @Mock CategoryRepository categoryRepository;
    @Mock ExpenseCacheService cacheService;
    @Mock ExpenseEventProducer eventProducer;

    @InjectMocks ExpenseService expenseService;

    private User user;
    private Category category;
    private Expense expense;
    private UUID userId;
    private UUID categoryId;
    private UUID expenseId;

    @BeforeEach
    void setUp() {
        userId     = UUID.randomUUID();
        categoryId = UUID.randomUUID();
        expenseId  = UUID.randomUUID();

        user = User.builder().id(userId).email("test@test.com").fullName("Test User")
                .passwordHash("hash").build();

        category = Category.builder().id(categoryId).name("Food & Dining").icon("utensils").build();

        expense = Expense.builder()
                .id(expenseId).user(user).category(category)
                .amount(new BigDecimal("50.00"))
                .description("Lunch")
                .expenseDate(LocalDate.now())
                .build();
    }

    // ���� getExpenses ��������������������������������������������������������������������������������������������������������������������

    @Test
    void getExpenses_returnsFromRepo_whenCacheMiss() {
        when(cacheService.buildListKey(any(), any(), any())).thenReturn("key");
        when(cacheService.getExpenseList("key")).thenReturn(null);
        when(expenseRepository.findByUserIdAndDateRange(eq(userId), any(), any()))
                .thenReturn(List.of(expense));

        List<ExpenseResponse> result = expenseService.getExpenses(userId, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(expenseId);
        verify(cacheService).putExpenseList(eq("key"), any());
    }

    @Test
    void getExpenses_returnsFromCache_whenCacheHit() {
        ExpenseResponse cached = new ExpenseResponse(expenseId, categoryId, "Food", "utensils",
                BigDecimal.TEN, "Cached", LocalDate.now(), null, null);
        when(cacheService.buildListKey(any(), any(), any())).thenReturn("key");
        when(cacheService.getExpenseList("key")).thenReturn(List.of(cached));

        List<ExpenseResponse> result = expenseService.getExpenses(userId, null, null);

        assertThat(result).hasSize(1);
        verifyNoInteractions(expenseRepository);
    }

    // ���� createExpense ����������������������������������������������������������������������������������������������������������������

    @Test
    void createExpense_savesAndPublishesEvent() {
        ExpenseRequest request = new ExpenseRequest(null, categoryId,
                new BigDecimal("75.00"), "Dinner", LocalDate.now());

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(expenseRepository.save(any(Expense.class))).thenReturn(expense);

        ExpenseResponse response = expenseService.createExpense(userId, request);

        assertThat(response.id()).isEqualTo(expenseId);
        verify(expenseRepository).save(any(Expense.class));
        verify(cacheService).evictUserCache(userId);
        verify(eventProducer).publishExpenseCreated(expense);
    }

    @Test
    void createExpense_throwsNotFound_whenCategoryMissing() {
        ExpenseRequest request = new ExpenseRequest(null, categoryId,
                BigDecimal.TEN, "X", LocalDate.now());

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> expenseService.createExpense(userId, request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(expenseRepository, never()).save(any());
    }

    // ���� deleteExpense ����������������������������������������������������������������������������������������������������������������

    @Test
    void deleteExpense_softDeletes() {
        when(expenseRepository.findByIdAndUserId(expenseId, userId))
                .thenReturn(Optional.of(expense));
        when(expenseRepository.save(expense)).thenReturn(expense);

        expenseService.deleteExpense(userId, expenseId);

        assertThat(expense.isDeleted()).isTrue();
        verify(cacheService).evictUserCache(userId);
    }

    @Test
    void deleteExpense_throwsNotFound_whenExpenseDoesNotBelongToUser() {
        when(expenseRepository.findByIdAndUserId(expenseId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> expenseService.deleteExpense(userId, expenseId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ���� getMonthlySummary ��������������������������������������������������������������������������������������������������������

    @Test
    void getMonthlySummary_returnsSummary() {
        ExpenseSummary summary = new ExpenseSummary(categoryId, "Food & Dining", "utensils",
                new BigDecimal("150.00"), 3L);

        when(cacheService.buildSummaryKey(userId, 2026, 4)).thenReturn("summary-key");
        when(cacheService.getSummary("summary-key")).thenReturn(null);
        when(expenseRepository.findMonthlySummaryByUserId(userId, 2026, 4))
                .thenReturn(List.of(summary));

        List<ExpenseSummaryResponse> result = expenseService.getMonthlySummary(userId, 2026, 4);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).categoryName()).isEqualTo("Food & Dining");
        assertThat(result.get(0).total()).isEqualByComparingTo("150.00");
        verify(cacheService).putSummary(eq("summary-key"), any());
    }

    // ���� syncExpenses ������������������������������������������������������������������������������������������������������������������

    @Test
    void syncExpenses_createsNewExpenses_whenIdNotFound() {
        ExpenseRequest request = new ExpenseRequest(null, categoryId,
                BigDecimal.TEN, "New", LocalDate.now());
        SyncRequest syncRequest = new SyncRequest(List.of(request));

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(expenseRepository.save(any(Expense.class))).thenReturn(expense);

        List<ExpenseResponse> result = expenseService.syncExpenses(userId, syncRequest);

        assertThat(result).hasSize(1);
        verify(eventProducer).publishExpenseCreated(expense);
    }
}
