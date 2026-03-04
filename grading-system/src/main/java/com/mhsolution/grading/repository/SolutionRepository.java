package com.mhsolution.grading.repository;

import com.mhsolution.grading.entity.Assignment;
import com.mhsolution.grading.entity.Solution;
import com.mhsolution.grading.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SolutionRepository extends JpaRepository<Solution, Long> {

    // All solutions submitted by one applicant (their history)
    List<Solution> findByUploader(User uploader);

    // All solutions for one assignment (admin/grader view)
    List<Solution> findByAssignment(Assignment assignment);

    // One applicant's solutions for a specific assignment (max 1, enforced by DB)
    // Used for IDOR-safe applicant access
    List<Solution> findByAssignmentAndUploader(Assignment assignment, User uploader);

    // IDOR-safe: get a specific solution only if it belongs to the uploader
    Optional<Solution> findByIdAndUploader(Long id, User uploader);

    // Check if an applicant has already submitted for this assignment
    boolean existsByAssignmentAndUploader(Assignment assignment, User uploader);

    // All solutions assigned to a specific grader
    List<Solution> findByAssignedGrader(User grader);

    // Solutions assigned to a grader for a specific assignment
    List<Solution> findByAssignmentAndAssignedGrader(Assignment assignment, User grader);
}
