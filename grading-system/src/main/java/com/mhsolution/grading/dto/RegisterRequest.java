package com.mhsolution.grading.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

/**
 * RegisterRequest DTO — Data Transfer Object for user registration form.
 *
 * DTOs separate the web layer (form data) from the domain layer (entities).
 * They carry only the data needed for a specific operation.
 *
 * Bean Validation annotations (@NotBlank, @Size, @Email) trigger automatic
 * validation when @Valid is added to the controller method parameter.
 */
@Getter
@Setter
public class RegisterRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username can only contain letters, numbers, and underscores")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @NotBlank(message = "Please confirm your password")
    private String confirmPassword;

    // Role selection: only APPLICANT or GRADER (ADMIN is created manually)
    @NotBlank(message = "Please select a role")
    private String role;
}
