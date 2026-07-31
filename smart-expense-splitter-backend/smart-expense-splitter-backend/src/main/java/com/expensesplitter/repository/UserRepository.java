package com.expensesplitter.repository;

import com.expensesplitter.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // Spring Data JPA derives the SQL from the method name at startup —
    // no @Query needed for simple lookups like this.
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
