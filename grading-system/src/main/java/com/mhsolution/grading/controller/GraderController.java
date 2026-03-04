package com.mhsolution.grading.controller;

import com.mhsolution.grading.dto.GradeRequest;
import com.mhsolution.grading.entity.Assignment;
import com.mhsolution.grading.entity.Solution;
import com.mhsolution.grading.service.AssignmentService;
import com.mhsolution.grading.service.SolutionService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/grader")
@PreAuthorize("hasAnyRole('GRADER', 'ADMIN')")
public class GraderController {

    private final AssignmentService assignmentService;
    private final SolutionService solutionService;

    public GraderController(AssignmentService assignmentService, SolutionService solutionService) {
        this.assignmentService = assignmentService;
        this.solutionService = solutionService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Principal principal, Model model) {
        List<Assignment> activeAssignments = assignmentService.findActiveAssignments();
        List<Solution> assignedSolutions = solutionService.findSolutionsForGrader(principal.getName());
        
        long pendingCount = assignedSolutions.stream().filter(s -> s.getScore() == null).count();
        
        model.addAttribute("activeAssignments", activeAssignments);
        model.addAttribute("assignedSolutions", assignedSolutions);
        model.addAttribute("pendingCount", pendingCount);
        return "grader/dashboard";
    }

    @GetMapping("/assignments/{id}")
    public String viewAssignment(@PathVariable Long id, Principal principal, Model model) {
        model.addAttribute("assignment", assignmentService.findById(id));
        model.addAttribute("assignedSolutions", solutionService.findSolutionsForGraderByAssignment(id, principal.getName()));
        return "grader/assignment-view";
    }

    @GetMapping("/grade/{id}")
    public String gradeForm(@PathVariable Long id, Principal principal, Model model) {
        Solution solution = solutionService.findById(id);
        
        // Security check: only assigned grader or admin can grade
        // (Simple check for now, service layer also verifies)
        
        GradeRequest request = new GradeRequest();
        request.setSolutionId(id); 
        request.setScore(solution.getScore());
        request.setComment(solution.getGraderComment());

        model.addAttribute("gradeRequest", request);
        model.addAttribute("solution", solution);
        return "grader/grade";
    }

    @PostMapping("/grade")
    public String submitGrade(@ModelAttribute GradeRequest request, 
                              Principal principal, RedirectAttributes ra) {
        try {
            solutionService.grade(request, principal.getName());
            ra.addFlashAttribute("success", "Grade submitted!");
            Solution s = solutionService.findById(request.getSolutionId());
            return "redirect:/grader/assignments/" + s.getAssignment().getId();
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/grader/dashboard";
        }
    }

    @GetMapping("/download/{id}")
    @ResponseBody
    public ResponseEntity<Resource> downloadFile(@PathVariable Long id, Principal principal) {
        // Admins and Graders assigned to the solution can download
        Solution solution = solutionService.getSolutionForDownload(id, principal.getName(), true);
        try {
            Path path = Paths.get(solution.getFilePath());
            Resource resource = new UrlResource(path.toUri());

            if (resource.exists()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType("application/octet-stream"))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + solution.getOriginalFilename() + "\"")
                        .body(resource);
            }
        } catch (MalformedURLException ignored) {}
        return ResponseEntity.notFound().build();
    }
}
