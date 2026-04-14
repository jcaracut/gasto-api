package com.gasto.presentation.expense;

import com.gasto.application.expense.ExpenseService;
import com.gasto.application.expense.dto.ExpenseRequest;
import com.gasto.application.expense.dto.ExpenseResponse;
import com.gasto.domain.exception.ResourceNotFoundException;
import com.gasto.infrastructure.security.JwtAuthFilter;
import com.gasto.infrastructure.security.JwtUtil;
import com.gasto.infrastructure.security.SecurityConfig;
import com.gasto.infrastructure.security.UserDetailsServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ExpenseController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class})
class ExpenseControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean ExpenseService expenseService;
    @MockitoBean UserDetailsServiceImpl userDetailsService;
    @MockitoBean JwtUtil jwtUtil;

    private UUID userId;
    private UUID expenseId;
    private UUID categoryId;
    private ExpenseResponse sampleResponse;

    @BeforeEach
    void setUp() {
        userId     = UUID.fromString("00000000-0000-0000-0000-000000000001");
        expenseId  = UUID.randomUUID();
        categoryId = UUID.randomUUID();

        sampleResponse = new ExpenseResponse(expenseId, categoryId, "Food & Dining", "utensils",
                new BigDecimal("50.00"), "Lunch", LocalDate.now(),
                OffsetDateTime.now(), OffsetDateTime.now());

        // Ensure @WithMockUser resolves: userDetailsService returns matching principal
        when(userDetailsService.loadUserByUsername(anyString())).thenAnswer(inv ->
                new User(userId.toString(), "n/a", Collections.emptyList()));
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000001")
    void getExpenses_returns200() throws Exception {
        when(expenseService.getExpenses(eq(userId), any(), any()))
                .thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/v1/expenses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(expenseId.toString()));
    }

    @Test
    void getExpenses_returns401_withoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/expenses"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000001")
    void createExpense_returns201() throws Exception {
        when(expenseService.createExpense(eq(userId), any(ExpenseRequest.class)))
                .thenReturn(sampleResponse);

        ExpenseRequest request = new ExpenseRequest(null, categoryId,
                new BigDecimal("50.00"), "Lunch", LocalDate.now());

        mockMvc.perform(post("/api/v1/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.amount").value(50.0));
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000001")
    void createExpense_returns400_withNegativeAmount() throws Exception {
        ExpenseRequest request = new ExpenseRequest(null, categoryId,
                new BigDecimal("-10.00"), "Bad", LocalDate.now());

        mockMvc.perform(post("/api/v1/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000001")
    void deleteExpense_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/expenses/" + expenseId))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000001")
    void getExpense_returns404_whenNotFound() throws Exception {
        when(expenseService.getExpense(eq(userId), eq(expenseId)))
                .thenThrow(new ResourceNotFoundException("Expense not found"));

        mockMvc.perform(get("/api/v1/expenses/" + expenseId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }
}
