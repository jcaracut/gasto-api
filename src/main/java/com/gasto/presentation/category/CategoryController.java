package com.gasto.presentation.category;

import com.gasto.domain.category.Category;
import com.gasto.domain.category.CategoryRepository;
import com.gasto.presentation.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryRepository categoryRepository;

    @GetMapping
    public ApiResponse<List<Category>> getAll() {
        return ApiResponse.ok(categoryRepository.findAll());
    }
}
