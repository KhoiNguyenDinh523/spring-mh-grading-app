package com.mhsolution.grading.controller;

import com.mhsolution.grading.dto.PasswordChangeRequest;
import com.mhsolution.grading.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    private final UserService userService;

    public ProfileController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/change-password")
    public String changePasswordForm(Model model) {
        model.addAttribute("passwordRequest", new PasswordChangeRequest());
        return "profile/change-password";
    }

    @PostMapping("/change-password")
    public String changePassword(@Valid @ModelAttribute("passwordRequest") PasswordChangeRequest request,
                                 BindingResult result,
                                 Principal principal,
                                 RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "profile/change-password";
        }

        try {
            userService.changePassword(principal.getName(), request);
            redirectAttributes.addFlashAttribute("success", "Password changed successfully!");
            // Redirect based on role to their dashboard
            return "redirect:/";
        } catch (IllegalArgumentException e) {
            result.rejectValue("oldPassword", "error.passwordRequest", e.getMessage());
            return "profile/change-password";
        }
    }
}
