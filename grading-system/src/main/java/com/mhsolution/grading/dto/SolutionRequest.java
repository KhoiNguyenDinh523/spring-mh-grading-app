package com.mhsolution.grading.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class SolutionRequest {
    
    private Long assignmentId;
    
    private String applicantComment;
    
    private MultipartFile file;
}
