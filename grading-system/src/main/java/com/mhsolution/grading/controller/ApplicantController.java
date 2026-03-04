package com.mhsolution.grading.controller;

import com.mhsolution.grading.dto.SolutionRequest;
import com.mhsolution.grading.entity.Assignment;
import com.mhsolution.grading.entity.Solution;
import com.mhsolution.grading.service.AssignmentService;
import com.mhsolution.grading.service.SolutionService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/applicant")
public class ApplicantController {

    private final AssignmentService assignmentService;
    private final SolutionService solutionService;

    public ApplicantController(AssignmentService assignmentService, SolutionService solutionService) {
        this.assignmentService = assignmentService;
        this.solutionService = solutionService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("activeAssignments", assignmentService.findActiveAssignments());
        return "applicant/dashboard";
    }

    @GetMapping("/assignments/{id}")
    public String viewAssignment(@PathVariable Long id, Principal principal, Model model) {
        Assignment assignment = assignmentService.findById(id);
        List<Solution> mySolutions = solutionService.findMySolutionsForAssignment(id, principal.getName());
        
        model.addAttribute("assignment", assignment);
        model.addAttribute("mySolutions", mySolutions);
        
        if (mySolutions.isEmpty() && assignment.isEffectivelyOngoing()) {
            SolutionRequest request = new SolutionRequest();
            request.setAssignmentId(id);
            model.addAttribute("solutionRequest", request);
        }
        
        return "applicant/assignment-view";
    }

    @PostMapping("/upload")
    public String uploadSolution(@ModelAttribute SolutionRequest request, 
                                 Principal principal, RedirectAttributes ra) {
        try {
            solutionService.upload(request, principal.getName());
            ra.addFlashAttribute("success", "Solution submitted successfully!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/applicant/assignments/" + request.getAssignmentId();
    }

    @GetMapping("/download/{id}")
    @ResponseBody
    public ResponseEntity<Resource> downloadFile(@PathVariable Long id, Principal principal, 
                                                 org.springframework.security.core.Authentication authentication) {
        boolean isPrivileged = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_GRADER"));
        Solution solution = solutionService.getSolutionForDownload(id, principal.getName(), isPrivileged);
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
