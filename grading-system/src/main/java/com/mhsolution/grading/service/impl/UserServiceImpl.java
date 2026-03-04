package com.mhsolution.grading.service.impl;

import com.mhsolution.grading.dto.PasswordChangeRequest;
import com.mhsolution.grading.dto.RegisterRequest;
import com.mhsolution.grading.dto.UpdateUserRequest;
import com.mhsolution.grading.entity.Role;
import com.mhsolution.grading.entity.User;
import com.mhsolution.grading.exception.ResourceNotFoundException;
import com.mhsolution.grading.repository.UserRepository;
import com.mhsolution.grading.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public User createUser(RegisterRequest request, String currentAdminUsername) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        Role targetRole = Role.valueOf("ROLE_" + request.getRole().toUpperCase());
        
        // Restriction: Only Superadmin/Admin can create, but let's assume security layer handles that.
        // Business Rule: Admin cannot create another Admin.
        if (targetRole == Role.ROLE_ADMIN) {
            throw new IllegalArgumentException("Admins cannot create other Admin accounts");
        }

        User user = new User(
                request.getUsername(),
                passwordEncoder.encode(request.getPassword()),
                request.getEmail(),
                targetRole
        );
        return userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findAll() {
        return userRepository.findByDeletedFalse();
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findByRole(Role role) {
        return userRepository.findByRoleAndDeletedFalse(role);
    }

    @Override
    public User update(Long id, UpdateUserRequest request, String currentAdminUsername) {
        User targetUser = findById(id);
        User currentAdmin = findByUsername(currentAdminUsername);

        // Security Rule: Cannot edit another Admin
        if (targetUser.getRole() == Role.ROLE_ADMIN && !targetUser.getUsername().equals(currentAdminUsername)) {
            throw new IllegalArgumentException("You cannot edit another Admin's account");
        }

        // Approach A: Immutable Roles - Prevent changing roles after creation
        if (targetUser.getRole() != request.getRole()) {
            throw new IllegalArgumentException("User roles cannot be changed after account creation. Please create a new account for the new role.");
        }

        // Rule: Admin cannot disable themselves
        if (targetUser.getUsername().equals(currentAdminUsername) && !request.isEnabled()) {
            throw new IllegalArgumentException("You cannot disable your own admin account");
        }

        targetUser.setUsername(request.getUsername());
        targetUser.setEmail(request.getEmail());
        targetUser.setEnabled(request.isEnabled());

        if (StringUtils.hasText(request.getNewPassword())) {
            targetUser.setPassword(passwordEncoder.encode(request.getNewPassword()));
        }

        return userRepository.save(targetUser);
    }

    @Override
    public void delete(Long id, String currentAdminUsername) {
        User targetUser = findById(id);
        if (targetUser.getUsername().equals(currentAdminUsername)) {
            throw new IllegalArgumentException("You cannot delete yourself");
        }
        if (targetUser.getRole() == Role.ROLE_ADMIN) {
            throw new IllegalArgumentException("Cannot delete another Admin");
        }

        // Soft Delete / Anonymization Logic
        targetUser.setDeleted(true);
        targetUser.setEnabled(false);

        if (targetUser.getRole() == Role.ROLE_APPLICANT) {
            // Anonymize Applicant for data integrity
            String anonymousId = "deleted_user_" + targetUser.getId();
            targetUser.setUsername(anonymousId);
            targetUser.setEmail(anonymousId + "@deleted.mhsolution.com");
            // Note: solutions still linked to this ID will show "deleted_user_X"
        }
        // For Graders, we just keep their profile (disabled/deleted=true) 
        // they are already hidden from dropdowns by findByRoleAndDeletedFalse

        userRepository.save(targetUser);
    }

    @Override
    public void setEnabled(Long id, boolean enabled, String currentAdminUsername) {
        User targetUser = findById(id);
        if (targetUser.getUsername().equals(currentAdminUsername) && !enabled) {
            throw new IllegalArgumentException("You cannot disable yourself");
        }
        if (targetUser.getRole() == Role.ROLE_ADMIN) {
            throw new IllegalArgumentException("Cannot toggle status of another Admin");
        }
        targetUser.setEnabled(enabled);
        userRepository.save(targetUser);
    }

    @Override
    public void changePassword(String username, PasswordChangeRequest request) {
        User user = findByUsername(username);

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Incorrect old password");
        }

        if (!request.getNewPassword().equals(request.getConfirmNewPassword())) {
            throw new IllegalArgumentException("New passwords do not match");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }
}
