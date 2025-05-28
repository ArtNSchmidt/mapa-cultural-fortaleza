package com.example.culturalmapapp.controller;

import com.example.culturalmapapp.dto.LoginRequest;
import com.example.culturalmapapp.dto.LoginResponse;
import com.example.culturalmapapp.dto.UserRegistrationRequest;
import com.example.culturalmapapp.model.User;
import com.example.culturalmapapp.service.JwtTokenProvider;
import com.example.culturalmapapp.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Authentication", description = "Endpoints for user registration and login")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Operation(summary = "Register a new user",
                 description = "Creates a new user account. Default role is CONSUMER if not specified.",
                 responses = {
                     @ApiResponse(responseCode = "201", description = "User registered successfully"),
                     @ApiResponse(responseCode = "400", description = "Invalid input or user/email already exists")
                 })
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody UserRegistrationRequest registrationRequest) {
        // The @Valid annotation will now handle the checks previously done manually.
        // Manual checks for null/empty can be removed if @NotBlank is comprehensive enough.
        // The initial manual check is removed as @NotBlank covers it.
        
        try {
            User newUser = userService.registerUser(registrationRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body("User registered successfully: " + newUser.getUsername());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @Operation(summary = "Authenticate user and generate JWT",
                 description = "Logs in an existing user and returns a JWT token upon successful authentication.",
                 responses = {
                     @ApiResponse(responseCode = "200", description = "Login successful, JWT token returned",
                                  content = @Content(mediaType = "application/json", schema = @Schema(implementation = LoginResponse.class))),
                     @ApiResponse(responseCode = "400", description = "Invalid input (e.g., missing username/password)"),
                     @ApiResponse(responseCode = "401", description = "Invalid credentials")
                 })
    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        // @Valid is added here as well
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtTokenProvider.generateToken(authentication);
        return ResponseEntity.ok(new LoginResponse(jwt));
    }

    @Operation(summary = "Get current authenticated user's details",
                 description = "Returns information about the currently logged-in user. Requires authentication.",
                 responses = {
                     @ApiResponse(responseCode = "200", description = "User details retrieved"),
                     @ApiResponse(responseCode = "401", description = "User not authenticated")
                 })
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.ok("Currently logged in as: " + authentication.getName() + " with roles: " + authentication.getAuthorities());
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No user authenticated.");
    }
}
