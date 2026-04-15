package com.gasto.infrastructure.persistence.jpa;

import com.gasto.domain.category.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaCategoryRepository extends JpaRepository<Category, UUID> {}
