package com.mhsolution.grading.config;

import com.mhsolution.grading.security.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * SecurityConfig — THE CORE Spring Security Configuration.
 *
 * This is the most important security file in the project. It defines:
 *
 *  1. Which URLs are publicly accessible vs protected
 *  2. How authentication (login/logout) works
 *  3. Which password encoder is used (BCrypt)
 *  4. Role-Based Access Control (RBAC) rules
 *
 * Annotations explained:
 *  @Configuration       : This class provides Spring Beans (methods annotated with @Bean)
 *  @EnableWebSecurity   : Activates Spring Security for web requests
 *  @EnableMethodSecurity: Allows @PreAuthorize/@PostAuthorize on individual methods
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;

    public SecurityConfig(CustomUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    /**
     * PasswordEncoder Bean — uses BCrypt hashing algorithm.
     *
     * BCrypt is intentionally SLOW (designed to resist brute-force attacks).
     * NEVER store plain-text passwords. Always encode before saving to DB.
     * strength=12 means 2^12 = 4096 iterations.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * AuthenticationProvider — wires together:
     *   - Our custom UserDetailsService (loads user from DB)
     *   - Our PasswordEncoder (BCrypt comparison)
     *
     * When a user logs in, Spring Security:
     *   1. Calls userDetailsService.loadUserByUsername(username)
     *   2. Compares the submitted password against the stored BCrypt hash
     *   3. On success: creates a SecurityContext with the authenticated principal
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * AuthenticationManager — required if you need to programmatically
     * authenticate users (e.g., during registration auto-login).
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * SecurityFilterChain — defines the HTTP security rules.
     *
     * This is the main method that controls who can access what.
     *
     * Rule evaluation order: rules are checked TOP-TO-BOTTOM.
     * The FIRST matching rule wins. Always put specific rules before general ones.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // ----------------------------------------------------------------
            // CSRF Protection
            // ----------------------------------------------------------------
            // CSRF (Cross-Site Request Forgery) protection is ENABLED by default.
            // Thymeleaf automatically includes CSRF tokens in forms.
            // Only disable CSRF for stateless REST APIs (not applicable here).
            .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))

            // ----------------------------------------------------------------
            // Authorization Rules (RBAC)
            // ----------------------------------------------------------------
            .authorizeHttpRequests(auth -> auth
                // Public pages: anyone can access
                .requestMatchers("/", "/login", "/css/**", "/js/**").permitAll()

                // Admin-only area: only users with ROLE_ADMIN
                .requestMatchers("/admin/**").hasRole("ADMIN")

                // Grader-only area
                .requestMatchers("/grader/**").hasAnyRole("GRADER", "ADMIN")

                // Applicant area
                .requestMatchers("/applicant/**").hasAnyRole("APPLICANT", "ADMIN")

                // All other requests require authentication (any logged-in user)
                .anyRequest().authenticated()
            )

            // ----------------------------------------------------------------
            // Form Login Configuration
            // ----------------------------------------------------------------
            .formLogin(form -> form
                .loginPage("/login")                    // Custom login page URL
                .loginProcessingUrl("/login")           // POST URL Spring Security handles
                .usernameParameter("username")          // HTML input field name
                .passwordParameter("password")          // HTML input field name
                .successHandler(authenticationSuccessHandler())  // Custom redirect by role
                .failureUrl("/login?error=true")        // Redirect on failure
                .permitAll()
            )

            // ----------------------------------------------------------------
            // Logout Configuration
            // ----------------------------------------------------------------
            .logout(logout -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout", "POST"))
                .logoutSuccessUrl("/login?logout=true")  // Redirect after logout
                .invalidateHttpSession(true)             // Destroy session
                .deleteCookies("JSESSIONID")             // Remove session cookie
                .permitAll()
            )

            // ----------------------------------------------------------------
            // Session Management
            // ----------------------------------------------------------------
            .sessionManagement(session -> session
                // Only one active session per user (prevents session sharing)
                .maximumSessions(1)
                .expiredUrl("/login?expired=true")
            )

            // Wire in our authentication provider
            .authenticationProvider(authenticationProvider());

        return http.build();
    }

    /**
     * Custom Authentication Success Handler.
     * Redirects users to different dashboards based on their role after login.
     *
     *   ROLE_ADMIN      → /admin/dashboard
     *   ROLE_GRADER     → /grader/dashboard
     *   ROLE_APPLICANT  → /applicant/dashboard
     */
    @Bean
    public org.springframework.security.web.authentication.AuthenticationSuccessHandler
            authenticationSuccessHandler() {
        return (request, response, authentication) -> {
            String role = authentication.getAuthorities().iterator().next().getAuthority();
            switch (role) {
                case "ROLE_ADMIN"     -> response.sendRedirect("/admin/dashboard");
                case "ROLE_GRADER"    -> response.sendRedirect("/grader/dashboard");
                case "ROLE_APPLICANT" -> response.sendRedirect("/applicant/dashboard");
                default               -> response.sendRedirect("/");
            }
        };
    }
}
