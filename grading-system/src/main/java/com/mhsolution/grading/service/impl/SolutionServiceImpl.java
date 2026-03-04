package com.mhsolution.grading.service.impl;

import com.mhsolution.grading.dto.GradeRequest;
import com.mhsolution.grading.dto.SolutionRequest;
import com.mhsolution.grading.entity.Assignment;
import com.mhsolution.grading.entity.Solution;
import com.mhsolution.grading.entity.User;
import com.mhsolution.grading.exception.ResourceNotFoundException;
import com.mhsolution.grading.repository.SolutionRepository;
import com.mhsolution.grading.service.AssignmentService;
import com.mhsolution.grading.service.SolutionService;
import com.mhsolution.grading.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class SolutionServiceImpl implements SolutionService {

    @Value("${app.upload.dir}")
    private String uploadDir;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf", "doc", "docx", "txt", "zip"
    );

    private final SolutionRepository solutionRepository;
    private final UserService userService;
    private final AssignmentService assignmentService;

    public SolutionServiceImpl(SolutionRepository solutionRepository, 
                               UserService userService,
                               AssignmentService assignmentService) {
        this.solutionRepository = solutionRepository;
        this.userService = userService;
        this.assignmentService = assignmentService;
    }

    @Override
    public Solution upload(SolutionRequest request, String username) throws IOException {
        User uploader = userService.findByUsername(username);
        Assignment assignment = assignmentService.findById(request.getAssignmentId());

        if (!assignment.isEffectivelyOngoing()) {
            throw new IllegalArgumentException("Assignment is not active or deadline passed");
        }

        if (solutionRepository.existsByAssignmentAndUploader(assignment, uploader)) {
            throw new IllegalArgumentException("You have already submitted a solution for this assignment");
        }

        if (request.getFile() == null || request.getFile().isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        String originalFilename = StringUtils.cleanPath(request.getFile().getOriginalFilename());
        String extension = originalFilename.substring(originalFilename.lastIndexOf('.') + 1);

        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new IllegalArgumentException("File extension not allowed");
        }

        String storedFilename = UUID.randomUUID() + "." + extension;
        Path targetPath = Paths.get(uploadDir).resolve(storedFilename);
        
        Files.createDirectories(targetPath.getParent());
        Files.copy(request.getFile().getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        Solution solution = new Solution();
        solution.setAssignment(assignment);
        solution.setUploader(uploader);
        solution.setOriginalFilename(originalFilename);
        solution.setStoredFilename(storedFilename);
        solution.setFilePath(targetPath.toString());
        solution.setFileExtension(extension);
        solution.setFileSize(request.getFile().getSize());
        solution.setApplicantComment(request.getApplicantComment());

        return solutionRepository.save(solution);
    }

    @Override
    @Transactional(readOnly = true)
    public Solution findById(Long id) {
        return solutionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solution not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Solution> findByAssignment(Long assignmentId) {
        Assignment assignment = assignmentService.findById(assignmentId);
        return solutionRepository.findByAssignment(assignment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Solution> findMySolutionsForAssignment(Long assignmentId, String username) {
        Assignment assignment = assignmentService.findById(assignmentId);
        User user = userService.findByUsername(username);
        return solutionRepository.findByAssignmentAndUploader(assignment, user);
    }

    @Override
    public void assignGrader(Long solutionId, Long graderId) {
        Solution solution = findById(solutionId);
        User grader = userService.findById(graderId);
        solution.setAssignedGrader(grader);
        solutionRepository.save(solution);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Solution> findSolutionsForGrader(String graderUsername) {
        User grader = userService.findByUsername(graderUsername);
        return solutionRepository.findByAssignedGrader(grader);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Solution> findSolutionsForGraderByAssignment(Long assignmentId, String graderUsername) {
        Assignment assignment = assignmentService.findById(assignmentId);
        User grader = userService.findByUsername(graderUsername);
        return solutionRepository.findByAssignmentAndAssignedGrader(assignment, grader);
    }

    @Override
    public void grade(GradeRequest request, String graderUsername) {
        // Fix for "The given id must not be null" error in whitelabel page:
        // Ensure request actually contains the ID.
        if (request.getSolutionId() == null) {
            throw new IllegalArgumentException("Solution ID is missing");
        }
        
        // Note: The controller/DTO renamed assignmentId to solutionId for clarity in the UI usually
        // but here we follow the request object mapping. 
        Solution solution = findById(request.getSolutionId()); 
        User grader = userService.findByUsername(graderUsername);

        solution.setScore(request.getScore());
        solution.setGraderComment(request.getComment());
        solution.setGradedBy(grader);
        solution.setGradedAt(LocalDateTime.now());

        solutionRepository.save(solution);
    }

    @Override
    @Transactional(readOnly = true)
    public Solution getSolutionForDownload(Long id, String username, boolean isPrivileged) {
        if (isPrivileged) {
            return findById(id);
        }
        User user = userService.findByUsername(username);
        return solutionRepository.findByIdAndUploader(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Solution not found or access denied"));
    }

    @Override
    public void delete(Long id) {
        Solution solution = findById(id);
        try {
            Files.deleteIfExists(Paths.get(solution.getFilePath()));
        } catch (IOException ignored) {}
        solutionRepository.delete(solution);
    }
}
