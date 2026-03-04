package com.mhsolution.grading.controller;

import com.mhsolution.grading.dto.*;
import com.mhsolution.grading.entity.Assignment;
import com.mhsolution.grading.entity.AssignmentStatus;
import com.mhsolution.grading.entity.Role;
import com.mhsolution.grading.entity.User;
import com.mhsolution.grading.service.AssignmentService;
import com.mhsolution.grading.service.SolutionService;
import com.mhsolution.grading.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserService userService;
    private final AssignmentService assignmentService;
    private final SolutionService solutionService;

    public AdminController(UserService userService, 
                           AssignmentService assignmentService, 
                           SolutionService solutionService) {
        this.userService = userService;
        this.assignmentService = assignmentService;
        this.solutionService = solutionService;
    }

    // --- Dashboard ---
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        List<Assignment> assignments = assignmentService.findAll();
        model.addAttribute("assignments", assignments);
        
        // Stats
        long ongoingCount = assignments.stream().filter(a -> a.getStatus() == AssignmentStatus.ONGOING).count();
        long expiredCount = assignments.stream().filter(a -> a.getStatus() == AssignmentStatus.EXPIRED).count();
        model.addAttribute("ongoingCount", ongoingCount);
        model.addAttribute("expiredCount", expiredCount);
        
        return "admin/dashboard";
    }

    // --- Assignment (Task) Management ---
    @GetMapping("/assignments/new")
    public String newAssignmentForm(Model model) {
        model.addAttribute("assignmentRequest", new AssignmentRequest());
        model.addAttribute("statuses", AssignmentStatus.values());
        return "admin/assignment-form";
    }

    @PostMapping("/assignments/new")
    public String createAssignment(@Valid @ModelAttribute("assignmentRequest") AssignmentRequest request,
                                   BindingResult result, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("statuses", AssignmentStatus.values());
            return "admin/assignment-form";
        }
        assignmentService.create(request);
        ra.addFlashAttribute("success", "Assignment created successfully");
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/assignments/{id}/edit")
    public String editAssignmentForm(@PathVariable Long id, Model model) {
        Assignment a = assignmentService.findById(id);
        AssignmentRequest request = new AssignmentRequest();
        request.setTitle(a.getTitle());
        request.setDescription(a.getDescription());
        request.setDeadline(a.getDeadline());
        request.setStatus(a.getStatus());
        
        model.addAttribute("assignmentRequest", request);
        model.addAttribute("statuses", AssignmentStatus.values());
        model.addAttribute("assignmentId", id);
        return "admin/assignment-form";
    }

    @PostMapping("/assignments/{id}/edit")
    public String updateAssignment(@PathVariable Long id, 
                                   @Valid @ModelAttribute("assignmentRequest") AssignmentRequest request,
                                   BindingResult result, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("statuses", AssignmentStatus.values());
            model.addAttribute("assignmentId", id);
            return "admin/assignment-form";
        }
        assignmentService.update(id, request);
        ra.addFlashAttribute("success", "Assignment updated");
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/assignments/{id}/delete")
    public String deleteAssignment(@PathVariable Long id, RedirectAttributes ra) {
        assignmentService.delete(id);
        ra.addFlashAttribute("success", "Assignment deleted");
        return "redirect:/admin/dashboard";
    }

    // --- Assignment Details & Solutions ---
    @GetMapping("/assignments/{id}")
    public String viewAssignment(@PathVariable Long id, Model model) {
        model.addAttribute("assignment", assignmentService.findById(id));
        model.addAttribute("solutions", solutionService.findByAssignment(id));
        model.addAttribute("graders", userService.findByRole(Role.ROLE_GRADER));
        return "admin/assignment-details";
    }

    @PostMapping("/solutions/assign")
    public String assignGrader(@ModelAttribute GraderAssignmentRequest request, RedirectAttributes ra) {
        solutionService.assignGrader(request.getSolutionId(), request.getGraderId());
        ra.addFlashAttribute("success", "Solution assigned to grader");
        return "redirect:/admin/assignments/" + solutionService.findById(request.getSolutionId()).getAssignment().getId();
    }

    // --- User Management (Restricted) ---
    @GetMapping("/users")
    public String listUsers(Model model) {
        model.addAttribute("users", userService.findAll());
        return "admin/users";
    }

    @GetMapping("/users/new")
    public String newUserForm(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest());
        model.addAttribute("roles", List.of("APPLICANT", "GRADER")); // Restricted: cannot create ADMIN
        return "admin/user-form";
    }

    @PostMapping("/users/new")
    public String createUser(@Valid @ModelAttribute("registerRequest") RegisterRequest request,
                             BindingResult result, Principal principal, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("roles", List.of("APPLICANT", "GRADER"));
            return "admin/user-form";
        }
        try {
            userService.createUser(request, principal.getName());
            ra.addFlashAttribute("success", "User created successfully");
            return "redirect:/admin/users";
        } catch (IllegalArgumentException e) {
            result.rejectValue("username", "error.user", e.getMessage());
            model.addAttribute("roles", List.of("APPLICANT", "GRADER"));
            return "admin/user-form";
        }
    }

    @GetMapping("/users/{id}/edit")
    public String editUserForm(@PathVariable Long id, Model model) {
        User user = userService.findById(id);
        UpdateUserRequest request = new UpdateUserRequest();
        request.setUsername(user.getUsername());
        request.setEmail(user.getEmail());
        request.setRole(user.getRole());
        request.setEnabled(user.isEnabled());
        
        model.addAttribute("updateRequest", request);
        model.addAttribute("userId", id);
        
        // Admins cannot change their role; other users cannot be assigned ADMIN role
        if (user.getRole() == Role.ROLE_ADMIN) {
            model.addAttribute("roles", List.of(Role.ROLE_ADMIN));
        } else {
            model.addAttribute("roles", List.of(Role.ROLE_APPLICANT, Role.ROLE_GRADER));
        }
        
        return "admin/edit-user";
    }

    @PostMapping("/users/{id}/edit")
    public String updateUser(@PathVariable Long id, 
                             @Valid @ModelAttribute("updateRequest") UpdateUserRequest request,
                             BindingResult result, Principal principal, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("userId", id);
            
            // Re-populate roles correctly on error
            User user = userService.findById(id);
            if (user.getRole() == Role.ROLE_ADMIN) {
                model.addAttribute("roles", List.of(Role.ROLE_ADMIN));
            } else {
                model.addAttribute("roles", List.of(Role.ROLE_APPLICANT, Role.ROLE_GRADER));
            }
            
            return "admin/edit-user";
        }
        try {
            userService.update(id, request, principal.getName());
            ra.addFlashAttribute("success", "User updated successfully");
            return "redirect:/admin/users";
        } catch (IllegalArgumentException e) {
            result.rejectValue("username", "error.user", e.getMessage());
            model.addAttribute("userId", id);
            
            // Re-populate roles correctly on error
            User user = userService.findById(id);
            if (user.getRole() == Role.ROLE_ADMIN) {
                model.addAttribute("roles", List.of(Role.ROLE_ADMIN));
            } else {
                model.addAttribute("roles", List.of(Role.ROLE_APPLICANT, Role.ROLE_GRADER));
            }
            
            return "admin/edit-user";
        }
    }

    @PostMapping("/users/{id}/toggle")
    public String toggleUser(@PathVariable Long id, @RequestParam boolean enabled, 
                             Principal principal, RedirectAttributes ra) {
        try {
            userService.setEnabled(id, enabled, principal.getName());
            ra.addFlashAttribute("success", "User status updated");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable Long id, Principal principal, RedirectAttributes ra) {
        try {
            userService.delete(id, principal.getName());
            ra.addFlashAttribute("success", "User deleted successfully");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }
}
