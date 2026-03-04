package com.mhsolution.grading.repository;

import com.mhsolution.grading.entity.Role;
import com.mhsolution.grading.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * UserRepository — Spring Data JPA interface for `users` table.
 *
 * By extending JpaRepository<User, Long>, we automatically get:
 *   - save(), findById(), findAll(), delete(), count(), etc.
 *
 * We only define CUSTOM query methods here. Spring Data JPA generates
 * their SQL implementation from the method name at runtime.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    List<User> findByDeletedFalse();

    // SELECT * FROM users WHERE username = ?
    Optional<User> findByUsername(String username);

    // SELECT * FROM users WHERE email = ?
    Optional<User> findByEmail(String email);

    // Check existence before registration
    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    // Admin: filter users by role (exclude deleted)
    List<User> findByRoleAndDeletedFalse(Role role);
}
