package com.mhsolution.grading.service;

import com.mhsolution.grading.dto.GradeRequest;
import com.mhsolution.grading.dto.SolutionRequest;
import com.mhsolution.grading.entity.Assignment;
import com.mhsolution.grading.entity.Solution;
import com.mhsolution.grading.entity.User;

import java.io.IOException;
import java.util.List;

public interface SolutionService {

    Solution upload(SolutionRequest request, String username) throws IOException;

    Solution findById(Long id);

    /** Grader/Admin: See all solutions for an assignment */
    List<Solution> findByAssignment(Long assignmentId);

    /** Applicant: See own solutions for an assignment */
    List<Solution> findMySolutionsForAssignment(Long assignmentId, String username);

    /** Admin: Assign solution to a specific grader */
    void assignGrader(Long solutionId, Long graderId);

    /** Grader: See only assigned solutions */
    List<Solution> findSolutionsForGrader(String graderUsername);
    
    /** Grader: See solutions for specific assignment assigned to them */
    List<Solution> findSolutionsForGraderByAssignment(Long assignmentId, String graderUsername);

    void grade(GradeRequest request, String graderUsername);

    /** IDOR-safe fetch for download */
    Solution getSolutionForDownload(Long id, String username, boolean isPrivileged);

    void delete(Long id);
}
