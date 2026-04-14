package com.gasto.infrastructure.persistence.adapter;

import com.gasto.domain.user.User;
import com.gasto.domain.user.UserRepository;
import com.gasto.infrastructure.persistence.jpa.JpaUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepository {

    private final JpaUserRepository jpa;

    @Override public User save(User user)                       { return jpa.save(user); }
    @Override public Optional<User> findById(UUID id)          { return jpa.findById(id); }
    @Override public Optional<User> findByEmail(String email)  { return jpa.findByEmail(email); }
    @Override public boolean existsByEmail(String email)       { return jpa.existsByEmail(email); }
}
