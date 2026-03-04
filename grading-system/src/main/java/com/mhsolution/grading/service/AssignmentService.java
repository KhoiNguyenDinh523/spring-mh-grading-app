package com.mhsolution.grading.service;

import com.mhsolution.grading.dto.AssignmentRequest;
import com.mhsolution.grading.entity.Assignment;
import com.mhsolution.grading.entity.AssignmentStatus;

import java.util.List;

public interface AssignmentService {
    
    Assignment create(AssignmentRequest request);
    
    Assignment update(Long id, AssignmentRequest request);
    
    Assignment findById(Long id);
    
    List<Assignment> findAll();
    
    /** Returns ONGOING assignments that haven't expired yet */
    List<Assignment> findActiveAssignments();
    
    void delete(Long id);
    
    void updateStatus(Long id, AssignmentStatus status);
}
