package com.expensesplitter.controller;

import com.expensesplitter.dto.response.UserResponse;
import com.expensesplitter.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // Every request here passes through JwtAuthenticationFilter first — if there's
    // no valid token, SecurityContext stays empty and Spring Security auto-rejects
    // with 401 before this method body ever runs.
    @GetMapping("/me")
    public UserResponse getCurrentUser() {
        return userService.getCurrentUser();
    }

    @GetMapping("/{id}")
    public UserResponse getUser(@PathVariable Long id) {
        return userService.getById(id);
    }
}
