package com.expensesplitter.service;

import com.expensesplitter.dto.response.UserResponse;
import com.expensesplitter.entity.User;
import com.expensesplitter.exception.ResourceNotFoundException;
import com.expensesplitter.repository.UserRepository;
import com.expensesplitter.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public UserResponse getCurrentUser() {
        String email = SecurityUtil.getCurrentUserEmail();
        User user = getUserEntityByEmail(email);
        return toResponse(user);
    }

    public User getUserEntityByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public User getUserEntityById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    public UserResponse getById(Long id) {
        return toResponse(getUserEntityById(id));
    }

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .build();
    }
}
