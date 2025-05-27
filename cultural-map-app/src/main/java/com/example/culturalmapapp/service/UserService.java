package com.example.culturalmapapp.service;

import com.example.culturalmapapp.dto.ChangePasswordRequest;
import com.example.culturalmapapp.dto.UpdateUserProfileRequest;
import com.example.culturalmapapp.dto.UserRegistrationRequest;
import com.example.culturalmapapp.exception.ResourceNotFoundException;
import com.example.culturalmapapp.exception.UserAlreadyExistsException;
import com.example.culturalmapapp.model.User;
import com.example.culturalmapapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Transactional
    public User registerUser(UserRegistrationRequest registrationRequest) {
        if (userRepository.findByUsername(registrationRequest.getUsername()).isPresent()) {
            throw new UserAlreadyExistsException("Username already exists: " + registrationRequest.getUsername());
        }
        if (userRepository.findByEmail(registrationRequest.getEmail()).isPresent()) {
            throw new UserAlreadyExistsException("Email already exists: " + registrationRequest.getEmail());
        }

        User newUser = new User();
        newUser.setUsername(registrationRequest.getUsername());
        newUser.setPassword(passwordEncoder.encode(registrationRequest.getPassword()));
        newUser.setEmail(registrationRequest.getEmail());

        String role = registrationRequest.getRole();
        if (!StringUtils.hasText(role)) {
            role = "CONSUMER"; // Default role
        } else {
            role = role.toUpperCase();
        }
        // Basic role validation (can be expanded)
        if (!role.equals("CONSUMER") && !role.equals("PRODUCER") && !role.equals("ADMIN")) {
            throw new IllegalArgumentException("Invalid role specified. Must be CONSUMER, PRODUCER, or ADMIN.");
        }
        newUser.setRole("ROLE_" + role); // Spring Security expects roles to start with ROLE_

        return userRepository.save(newUser);
    }

    @Transactional(readOnly = true)
    public User getCurrentUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
    }

    @Transactional
    public User updateUserProfile(String username, UpdateUserProfileRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));

        // Check if the new email is already taken by another user
        Optional<User> userByNewEmail = userRepository.findByEmail(request.getEmail());
        if (userByNewEmail.isPresent() && !userByNewEmail.get().getId().equals(user.getId())) {
            throw new UserAlreadyExistsException("Email is already in use by another account.");
        }

        user.setEmail(request.getEmail());
        // Add other updatable fields here if any
        return userRepository.save(user);
    }

    @Transactional
    public void changePassword(String username, ChangePasswordRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Incorrect current password.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }
}
