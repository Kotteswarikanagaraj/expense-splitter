package com.expensesplitter.security;

import com.expensesplitter.entity.User;
import com.expensesplitter.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * Bridges our own User entity to Spring Security's UserDetails contract.
 * Spring Security doesn't know about our JPA User entity — it only knows how to
 * talk to UserDetailsService. This class is the adapter between the two worlds.
 * We use email as the "username" since that's how users log in.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("No user found with email: " + email));

        // No roles/authorities system in Phase 1 — every authenticated user has
        // the same access level. Collections.emptyList() keeps this explicit.
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())
                .authorities(Collections.emptyList())
                .build();
    }
}
