package com.gasto.infrastructure.redis;

import com.gasto.application.expense.dto.ExpenseResponse;
import com.gasto.application.expense.dto.ExpenseSummaryResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExpenseCacheService {

    private static final String LIST_PREFIX    = "expenses::list::";
    private static final String SUMMARY_PREFIX = "expenses::summary::";

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.cache.expense-list-ttl-minutes:10}")
    private long listTtlMinutes;

    @Value("${app.cache.expense-summary-ttl-minutes:30}")
    private long summaryTtlMinutes;

    // ���� List cache ��������������������������������������������������������������������������������������������������������������������

    public String buildListKey(UUID userId, LocalDate from, LocalDate to) {
        return LIST_PREFIX + userId + "::" + from + "::" + to;
    }

    public List<ExpenseResponse> getExpenseList(String key) {
        try {
            Object raw = redisTemplate.opsForValue().get(key);
            if (raw == null) return null;
            return objectMapper.convertValue(raw, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("Redis read error for key {}: {}", key, e.getMessage());
            return null;
        }
    }

    public void putExpenseList(String key, List<ExpenseResponse> value) {
        try {
            redisTemplate.opsForValue().set(key, value, Duration.ofMinutes(listTtlMinutes));
        } catch (Exception e) {
            log.warn("Redis write error for key {}: {}", key, e.getMessage());
        }
    }

    // ���� Summary cache ����������������������������������������������������������������������������������������������������������������

    public String buildSummaryKey(UUID userId, int year, int month) {
        return SUMMARY_PREFIX + userId + "::" + year + "-" + String.format("%02d", month);
    }

    public List<ExpenseSummaryResponse> getSummary(String key) {
        try {
            Object raw = redisTemplate.opsForValue().get(key);
            if (raw == null) return null;
            return objectMapper.convertValue(raw, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("Redis read error for key {}: {}", key, e.getMessage());
            return null;
        }
    }

    public void putSummary(String key, List<ExpenseSummaryResponse> value) {
        try {
            redisTemplate.opsForValue().set(key, value, Duration.ofMinutes(summaryTtlMinutes));
        } catch (Exception e) {
            log.warn("Redis write error for key {}: {}", key, e.getMessage());
        }
    }

    // ���� Eviction ��������������������������������������������������������������������������������������������������������������������������

    public void evictUserCache(UUID userId) {
        try {
            var listKeys    = redisTemplate.keys(LIST_PREFIX + userId + "::*");
            var summaryKeys = redisTemplate.keys(SUMMARY_PREFIX + userId + "::*");
            if (listKeys != null && !listKeys.isEmpty())    redisTemplate.delete(listKeys);
            if (summaryKeys != null && !summaryKeys.isEmpty()) redisTemplate.delete(summaryKeys);
        } catch (Exception e) {
            log.warn("Redis eviction error for user {}: {}", userId, e.getMessage());
        }
    }
}
