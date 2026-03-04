package com.mhsolution.grading.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GraderAssignmentRequest {
    
    @NotNull(message = "Solution ID is required")
    private Long solutionId;
    
    @NotNull(message = "Grader ID is required")
    private Long graderId;
}
