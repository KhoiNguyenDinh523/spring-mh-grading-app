package com.mhsolution.grading.repository;

import com.mhsolution.grading.entity.Assignment;
import com.mhsolution.grading.entity.AssignmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    // All assignments ordered newest first (admin view)
    List<Assignment> findAllByOrderByCreatedAtDesc();

    // Fetch by status (used for PENDING/ONGOING/EXPIRED filtering)
    List<Assignment> findByStatus(AssignmentStatus status);

    // Fetch ONGOING assignments whose deadline hasn't passed yet
    // Used by Applicant & Grader dashboards
    List<Assignment> findByStatusAndDeadlineAfter(AssignmentStatus status, LocalDateTime now);
}
