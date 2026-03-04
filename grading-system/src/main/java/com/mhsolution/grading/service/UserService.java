package com.mhsolution.grading.service;

import com.mhsolution.grading.dto.PasswordChangeRequest;
import com.mhsolution.grading.dto.RegisterRequest;
import com.mhsolution.grading.dto.UpdateUserRequest;
import com.mhsolution.grading.entity.Role;
import com.mhsolution.grading.entity.User;

import java.util.List;

/**
 * UserService interface — defines the contract for all user-related business logic.
 *
 * Why use an interface?
 *   - Enables dependency injection via interface type (loose coupling)
 *   - Makes it easy to swap implementations (e.g., for testing with mocks)
 *   - Follows the Dependency Inversion Principle (SOLID)
 *
 * The actual implementation is in UserServiceImpl.
 */
public interface UserService {

    /** Admin-only user creation (Replacement for public registration) */
    User createUser(RegisterRequest request, String currentAdminUsername);

    /** Find a user by their username (used internally). */
    User findByUsername(String username);

    /** Find a user by their ID. */
    User findById(Long id);

    /** Return all users (Admin only). */
    List<User> findAll();

    /** Return only specific roles (e.g. Graders for assignment dropdowns) */
    List<User> findByRole(Role role);

    /** Update a user's details (Admin only, restricted by rules). */
    User update(Long id, UpdateUserRequest request, String currentAdminUsername);

    /** Delete a user by ID (Admin only, restricted). */
    void delete(Long id, String currentAdminUsername);

    /** Enable or disable a user account (Admin only, restricted). */
    void setEnabled(Long id, boolean enabled, String currentAdminUsername);

    /** Change password for self */
    void changePassword(String username, PasswordChangeRequest request);
}
