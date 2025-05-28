package com.example.culturalmapapp.controller;

import com.example.culturalmapapp.config.SecurityConfig;
import com.example.culturalmapapp.dto.ChangePasswordRequest;
import com.example.culturalmapapp.dto.UpdateUserProfileRequest;
import com.example.culturalmapapp.dto.UserProfileResponse;
import com.example.culturalmapapp.exception.UserAlreadyExistsException;
import com.example.culturalmapapp.filter.JwtAuthenticationFilter;
import com.example.culturalmapapp.model.User;
import com.example.culturalmapapp.service.CustomUserDetailsService;
import com.example.culturalmapapp.service.JwtTokenProvider;
import com.example.culturalmapapp.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Set;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserProfileController.class)
@Import({SecurityConfig.class, CustomUserDetailsService.class, JwtTokenProvider.class, JwtAuthenticationFilter.class})
public class UserProfileControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider; // Required by JwtAuthenticationFilter

    @MockBean
    private CustomUserDetailsService customUserDetailsService; // Required by SecurityConfig

    @Autowired
    private ObjectMapper objectMapper;

    private User mockUser;
    private UserProfileResponse userProfileResponse;

    @BeforeEach
    void setUp(WebApplicationContext webApplicationContext) {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        mockUser = new User(1L, "testuser", "encodedPassword", "test@example.com", "ROLE_CONSUMER");

        userProfileResponse = new UserProfileResponse();
        userProfileResponse.setId(1L);
        userProfileResponse.setUsername("testuser");
        userProfileResponse.setEmail("test@example.com");
        userProfileResponse.setRoles(Set.of("ROLE_CONSUMER"));
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"CONSUMER"})
    void testGetCurrentUserProfile_Authenticated_ReturnsProfile() throws Exception {
        given(userService.getCurrentUserByUsername("testuser")).willReturn(mockUser);

        mockMvc.perform(get("/api/profile/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("testuser")))
                .andExpect(jsonPath("$.email", is("test@example.com")))
                .andExpect(jsonPath("$.roles[0]", is("ROLE_CONSUMER")));
    }

    @Test
    void testGetCurrentUserProfile_Unauthenticated_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/profile/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"CONSUMER"})
    void testUpdateUserProfile_ValidRequest_ReturnsOk() throws Exception {
        UpdateUserProfileRequest updateRequest = new UpdateUserProfileRequest();
        updateRequest.setEmail("newemail@example.com");

        User updatedUser = new User(1L, "testuser", "encodedPassword", "newemail@example.com", "ROLE_CONSUMER");
        given(userService.updateUserProfile(eq("testuser"), any(UpdateUserProfileRequest.class))).willReturn(updatedUser);

        mockMvc.perform(put("/api/profile/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is("newemail@example.com")));
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"CONSUMER"})
    void testUpdateUserProfile_InvalidEmail_ReturnsBadRequest() throws Exception {
        UpdateUserProfileRequest updateRequest = new UpdateUserProfileRequest();
        updateRequest.setEmail("invalidemail"); // Invalid format

        mockMvc.perform(put("/api/profile/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0]", is("email: Email should be valid")));
    }
    
    @Test
    @WithMockUser(username = "testuser", roles = {"CONSUMER"})
    void testUpdateUserProfile_EmailAlreadyExists_ReturnsBadRequest() throws Exception {
        UpdateUserProfileRequest updateRequest = new UpdateUserProfileRequest();
        updateRequest.setEmail("existing@example.com");

        given(userService.updateUserProfile(eq("testuser"), any(UpdateUserProfileRequest.class)))
            .willThrow(new UserAlreadyExistsException("Email is already in use by another account."));

        mockMvc.perform(put("/api/profile/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isConflict()) // GlobalExceptionHandler maps this to 409
                .andExpect(jsonPath("$.message", is("Email is already in use by another account.")));
    }


    @Test
    @WithMockUser(username = "testuser", roles = {"CONSUMER"})
    void testChangePassword_ValidRequest_ReturnsOk() throws Exception {
        ChangePasswordRequest changeRequest = new ChangePasswordRequest();
        changeRequest.setCurrentPassword("oldPassword123");
        changeRequest.setNewPassword("newPassword456");

        doNothing().when(userService).changePassword(eq("testuser"), any(ChangePasswordRequest.class));

        mockMvc.perform(post("/api/profile/me/change-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(changeRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Password changed successfully.")));
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"CONSUMER"})
    void testChangePassword_IncorrectCurrentPassword_ReturnsBadRequest() throws Exception {
        ChangePasswordRequest changeRequest = new ChangePasswordRequest();
        changeRequest.setCurrentPassword("wrongOldPassword");
        changeRequest.setNewPassword("newPassword456");

        doThrow(new IllegalArgumentException("Incorrect current password."))
            .when(userService).changePassword(eq("testuser"), any(ChangePasswordRequest.class));

        mockMvc.perform(post("/api/profile/me/change-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(changeRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Incorrect current password.")));
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"CONSUMER"})
    void testChangePassword_InvalidNewPassword_ReturnsBadRequest() throws Exception {
        ChangePasswordRequest changeRequest = new ChangePasswordRequest();
        changeRequest.setCurrentPassword("oldPassword123");
        changeRequest.setNewPassword("short"); // Invalid - @Size(min=6)

        mockMvc.perform(post("/api/profile/me/change-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(changeRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0]", is("newPassword: New password must be between 6 and 100 characters")));
    }
}
