package com.mhsolution.grading.entity;

/**
 * AssignmentStatus — the lifecycle of an assignment (task) created by admin.
 *
 * PENDING  → Created by admin but not yet visible to Applicants or Graders.
 * ONGOING  → Active: Applicants can submit Solutions, Graders can grade.
 * EXPIRED  → Past deadline or manually closed by admin. No new submissions.
 */
public enum AssignmentStatus {
    PENDING,
    ONGOING,
    EXPIRED
}
