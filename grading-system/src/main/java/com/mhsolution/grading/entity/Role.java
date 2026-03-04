package com.mhsolution.grading.entity;

/**
 * Enum representing the roles a User can have in the system.
 *
 * ROLE_APPLICANT  -> Can upload assignment files.
 * ROLE_GRADER     -> Can view/download assignments and submit evaluations.
 * ROLE_ADMIN      -> Full access: CRUD users, assign roles, all user operations.
 *
 * Spring Security convention: role names are prefixed with "ROLE_"
 */
public enum Role {
    ROLE_APPLICANT,
    ROLE_GRADER,
    ROLE_ADMIN
}
