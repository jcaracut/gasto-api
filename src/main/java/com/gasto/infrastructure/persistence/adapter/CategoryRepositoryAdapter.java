package com.gasto.infrastructure.persistence.adapter;

import com.gasto.domain.category.Category;
import com.gasto.domain.category.CategoryRepository;
import com.gasto.infrastructure.persistence.jpa.JpaCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class CategoryRepositoryAdapter implements CategoryRepository {

    private final JpaCategoryRepository jpa;

    @Override public List<Category> findAll()                       { return jpa.findAll(); }
    @Override public Optional<Category> findById(UUID id)          { return jpa.findById(id); }
    @Override public boolean existsById(UUID id)                   { return jpa.existsById(id); }
}
