package com.example.culturalmapapp.controller;

import com.example.culturalmapapp.dto.ChangePasswordRequest;
import com.example.culturalmapapp.dto.UpdateUserProfileRequest;
import com.example.culturalmapapp.dto.UserProfileResponse;
import com.example.culturalmapapp.model.User;
import com.example.culturalmapapp.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Arrays;
import java.util.HashSet;
import java.util.stream.Collectors;

@Tag(name = "User Profile", description = "Endpoints for managing the current user's profile")
@RestController
@RequestMapping("/api/profile")
@SecurityRequirement(name = "bearerAuth") // Apply JWT security to all endpoints in this controller
public class UserProfileController {

    @Autowired
    private UserService userService;

    @Operation(summary = "Get current user's profile",
                 description = "Retrieves the profile information of the currently authenticated user.",
                 responses = {
                     @ApiResponse(responseCode = "200", description = "Profile retrieved successfully",
                                  content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserProfileResponse.class))),
                     @ApiResponse(responseCode = "401", description = "User not authenticated")
                 })
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserProfileResponse> getCurrentUserProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userService.getCurrentUserByUsername(username);
        
        UserProfileResponse profileResponse = new UserProfileResponse();
        profileResponse.setId(user.getId());
        profileResponse.setUsername(user.getUsername());
        profileResponse.setEmail(user.getEmail());
        profileResponse.setRoles(
            Arrays.stream(user.getRole().split(","))
                  .collect(Collectors.toSet())
        );
        return ResponseEntity.ok(profileResponse);
    }

    @Operation(summary = "Update current user's profile",
                 description = "Allows the currently authenticated user to update their profile information (e.g., email).",
                 responses = {
                     @ApiResponse(responseCode = "200", description = "Profile updated successfully",
                                  content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserProfileResponse.class))),
                     @ApiResponse(responseCode = "400", description = "Invalid input data or email already exists"),
                     @ApiResponse(responseCode = "401", description = "User not authenticated"),
                     @ApiResponse(responseCode = "404", description = "User not found (should not happen if authenticated)")
                 })
    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updateUserProfile(@Valid @RequestBody UpdateUserProfileRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        try {
            User updatedUser = userService.updateUserProfile(username, request);
            UserProfileResponse profileResponse = new UserProfileResponse();
            profileResponse.setId(updatedUser.getId());
            profileResponse.setUsername(updatedUser.getUsername());
            profileResponse.setEmail(updatedUser.getEmail());
            profileResponse.setRoles(
                Arrays.stream(updatedUser.getRole().split(","))
                      .collect(Collectors.toSet())
            );
            return ResponseEntity.ok(profileResponse);
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "Change current user's password",
                 description = "Allows the currently authenticated user to change their password.",
                 responses = {
                     @ApiResponse(responseCode = "200", description = "Password changed successfully"),
                     @ApiResponse(responseCode = "400", description = "Invalid input data (e.g., incorrect current password, new password policy violation)"),
                     @ApiResponse(responseCode = "401", description = "User not authenticated"),
                     @ApiResponse(responseCode = "404", description = "User not found (should not happen if authenticated)")
                 })
    @PostMapping("/me/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        try {
            userService.changePassword(username, request);
            return ResponseEntity.ok("Password changed successfully.");
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) { // For incorrect current password
             return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("An unexpected error occurred.");
        }
    }
}
