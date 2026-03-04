package com.mhsolution.grading.security;

import com.mhsolution.grading.entity.User;
import com.mhsolution.grading.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * CustomUserDetailsService — Bridges Spring Security with our User entity.
 *
 * Spring Security's authentication mechanism calls loadUserByUsername()
 * to retrieve user details during login. We implement UserDetailsService
 * to load the user from our MySQL database via UserRepository.
 *
 * The returned UserDetails object contains:
 *   - username
 *   - hashed password (Spring Security compares it with the submitted password)
 *   - granted authorities (our Role enum becomes a GrantedAuthority)
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Look up user in DB; throw standard Spring Security exception if not found
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found: " + username));

        // Convert our Role enum to a Spring Security GrantedAuthority
        // SimpleGrantedAuthority wraps the role string (e.g., "ROLE_ADMIN")
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                user.isEnabled(),   // accountNonExpired
                true,               // accountNonLocked
                true,               // credentialsNonExpired
                true,               // enabled
                List.of(new SimpleGrantedAuthority(user.getRole().name()))
        );
    }
}
