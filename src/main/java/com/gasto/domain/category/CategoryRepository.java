package com.gasto.domain.category;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository {
    List<Category> findAll();
    Optional<Category> findById(UUID id);
    boolean existsById(UUID id);
}
