package com.mhsolution.grading.service.impl;

import com.mhsolution.grading.dto.AssignmentRequest;
import com.mhsolution.grading.entity.Assignment;
import com.mhsolution.grading.entity.AssignmentStatus;
import com.mhsolution.grading.exception.ResourceNotFoundException;
import com.mhsolution.grading.repository.AssignmentRepository;
import com.mhsolution.grading.service.AssignmentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class AssignmentServiceImpl implements AssignmentService {

    private final AssignmentRepository assignmentRepository;

    public AssignmentServiceImpl(AssignmentRepository assignmentRepository) {
        this.assignmentRepository = assignmentRepository;
    }

    @Override
    public Assignment create(AssignmentRequest request) {
        Assignment assignment = new Assignment();
        mapRequestToEntity(request, assignment);
        return assignmentRepository.save(assignment);
    }

    @Override
    public Assignment update(Long id, AssignmentRequest request) {
        Assignment assignment = findById(id);
        mapRequestToEntity(request, assignment);
        return assignmentRepository.save(assignment);
    }

    @Override
    @Transactional(readOnly = true)
    public Assignment findById(Long id) {
        return assignmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Assignment> findAll() {
        return assignmentRepository.findAllByOrderByCreatedAtDesc();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Assignment> findActiveAssignments() {
        return assignmentRepository.findByStatusAndDeadlineAfter(AssignmentStatus.ONGOING, LocalDateTime.now());
    }

    @Override
    public void delete(Long id) {
        Assignment assignment = findById(id);
        assignmentRepository.delete(assignment);
    }

    @Override
    public void updateStatus(Long id, AssignmentStatus status) {
        Assignment assignment = findById(id);
        assignment.setStatus(status);
        assignmentRepository.save(assignment);
    }

    private void mapRequestToEntity(AssignmentRequest request, Assignment assignment) {
        assignment.setTitle(request.getTitle());
        assignment.setDescription(request.getDescription());
        assignment.setDeadline(request.getDeadline());
        assignment.setStatus(request.getStatus());
    }
}
