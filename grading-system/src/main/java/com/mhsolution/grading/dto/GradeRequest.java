package com.mhsolution.grading.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

/**
 * GradeRequest DTO — submitted by a Grader when evaluating an assignment.
 */
@Getter
@Setter
public class GradeRequest {

    @NotNull(message = "Solution ID is required")
    private Long solutionId;

    @NotBlank(message = "Comment is required")
    @Size(max = 2000, message = "Comment cannot exceed 2000 characters")
    private String comment;

    @NotNull(message = "Score is required")
    @Min(value = 0, message = "Score must be at least 0")
    @Max(value = 10, message = "Score cannot exceed 10")
    private Integer score;
}
