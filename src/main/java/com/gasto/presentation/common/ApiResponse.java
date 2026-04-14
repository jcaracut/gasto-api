package com.gasto.presentation.common;

import java.time.OffsetDateTime;
import java.util.Map;

public record ApiResponse<T>(
        boolean success,
        T data,
        String message,
        OffsetDateTime timestamp
) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null, OffsetDateTime.now());
    }

    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(true, data, message, OffsetDateTime.now());
    }

    public static ApiResponse<Map<String, String>> error(String message) {
        return new ApiResponse<>(false, Map.of("error", message), message, OffsetDateTime.now());
    }
}
