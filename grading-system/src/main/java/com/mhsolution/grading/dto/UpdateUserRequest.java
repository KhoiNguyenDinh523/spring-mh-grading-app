package com.mhsolution.grading.dto;

import com.mhsolution.grading.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * UpdateUserRequest DTO — Admin use only.
 * Used when an Admin edits a user's details or changes their role.
 */
@Getter
@Setter
public class UpdateUserRequest {

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Email is required")
    @Email
    private String email;

    @NotNull(message = "Role is required")
    private Role role;

    private boolean enabled;

    // Optional new password (leave blank to keep existing password)
    private String newPassword;
}
