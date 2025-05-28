package com.example.culturalmapapp.service;

import com.example.culturalmapapp.dto.ChangePasswordRequest;
import com.example.culturalmapapp.dto.UpdateUserProfileRequest;
import com.example.culturalmapapp.dto.UserRegistrationRequest;
import com.example.culturalmapapp.exception.UserAlreadyExistsException;
import com.example.culturalmapapp.model.User;
import com.example.culturalmapapp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTests {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private UserRegistrationRequest registrationRequest;
    private User existingUser;

    @BeforeEach
    void setUp() {
        registrationRequest = new UserRegistrationRequest();
        registrationRequest.setUsername("testuser");
        registrationRequest.setPassword("password123");
        registrationRequest.setEmail("test@example.com");
        registrationRequest.setRole("CONSUMER");

        existingUser = new User(1L, "testuser", "encodedPassword", "test@example.com", "ROLE_CONSUMER");
    }

    @Test
    void testRegisterUser_Success() {
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User registeredUser = userService.registerUser(registrationRequest);

        assertNotNull(registeredUser);
        assertEquals("testuser", registeredUser.getUsername());
        assertEquals("encodedPassword", registeredUser.getPassword());
        assertEquals("test@example.com", registeredUser.getEmail());
        assertEquals("ROLE_CONSUMER", registeredUser.getRole());

        verify(userRepository, times(1)).findByUsername("testuser");
        verify(userRepository, times(1)).findByEmail("test@example.com");
        verify(passwordEncoder, times(1)).encode("password123");
        verify(userRepository, times(1)).save(any(User.class));
    }
    
    @Test
    void testRegisterUser_DefaultRole_Success() {
        registrationRequest.setRole(null); // Test default role assignment
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User registeredUser = userService.registerUser(registrationRequest);
        assertEquals("ROLE_CONSUMER", registeredUser.getRole());
    }
    
    @Test
    void testRegisterUser_ProducerRole_Success() {
        registrationRequest.setRole("PRODUCER"); 
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User registeredUser = userService.registerUser(registrationRequest);
        assertEquals("ROLE_PRODUCER", registeredUser.getRole());
    }
    
    @Test
    void testRegisterUser_InvalidRole_ThrowsException() {
        registrationRequest.setRole("INVALID_ROLE"); 
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
       
        assertThrows(IllegalArgumentException.class, () -> userService.registerUser(registrationRequest));
    }


    @Test
    void testRegisterUser_UsernameExists_ThrowsUserAlreadyExistsException() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(existingUser));

        assertThrows(UserAlreadyExistsException.class, () -> userService.registerUser(registrationRequest));
        verify(userRepository, times(1)).findByUsername("testuser");
        verify(userRepository, never()).findByEmail(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testRegisterUser_EmailExists_ThrowsUserAlreadyExistsException() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(existingUser));

        assertThrows(UserAlreadyExistsException.class, () -> userService.registerUser(registrationRequest));
        verify(userRepository, times(1)).findByUsername("testuser");
        verify(userRepository, times(1)).findByEmail("test@example.com");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testUpdateUserProfile_Success() {
        UpdateUserProfileRequest updateRequest = new UpdateUserProfileRequest();
        updateRequest.setEmail("newemail@example.com");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(existingUser));
        when(userRepository.findByEmail("newemail@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(existingUser);

        User updatedUser = userService.updateUserProfile("testuser", updateRequest);

        assertNotNull(updatedUser);
        assertEquals("newemail@example.com", updatedUser.getEmail());
        verify(userRepository, times(1)).save(existingUser);
    }

    @Test
    void testUpdateUserProfile_EmailExistsForAnotherUser_ThrowsUserAlreadyExistsException() {
        UpdateUserProfileRequest updateRequest = new UpdateUserProfileRequest();
        updateRequest.setEmail("another@example.com");

        User anotherUserWithEmail = new User(2L, "anotheruser", "pass", "another@example.com", "ROLE_CONSUMER");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(existingUser));
        when(userRepository.findByEmail("another@example.com")).thenReturn(Optional.of(anotherUserWithEmail));

        assertThrows(UserAlreadyExistsException.class, () -> userService.updateUserProfile("testuser", updateRequest));
        verify(userRepository, never()).save(any(User.class));
    }
    
    @Test
    void testUpdateUserProfile_EmailUnchanged_Success() {
        UpdateUserProfileRequest updateRequest = new UpdateUserProfileRequest();
        updateRequest.setEmail(existingUser.getEmail()); // Email is the same

        when(userRepository.findByUsername(existingUser.getUsername())).thenReturn(Optional.of(existingUser));
        // findByEmail should be called, but it will find the current user's email
        when(userRepository.findByEmail(existingUser.getEmail())).thenReturn(Optional.of(existingUser)); 
        when(userRepository.save(any(User.class))).thenReturn(existingUser);

        User updatedUser = userService.updateUserProfile(existingUser.getUsername(), updateRequest);
        assertNotNull(updatedUser);
        assertEquals(existingUser.getEmail(), updatedUser.getEmail());
        verify(userRepository, times(1)).save(existingUser);
    }


    @Test
    void testChangePassword_Success() {
        ChangePasswordRequest changeRequest = new ChangePasswordRequest();
        changeRequest.setCurrentPassword("password123");
        changeRequest.setNewPassword("newPassword456");

        existingUser.setPassword("encodedOldPassword"); // Set the original encoded password

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("password123", "encodedOldPassword")).thenReturn(true);
        when(passwordEncoder.encode("newPassword456")).thenReturn("encodedNewPassword");
        when(userRepository.save(any(User.class))).thenReturn(existingUser);

        userService.changePassword("testuser", changeRequest);

        assertEquals("encodedNewPassword", existingUser.getPassword());
        verify(passwordEncoder, times(1)).encode("newPassword456");
        verify(userRepository, times(1)).save(existingUser);
    }

    @Test
    void testChangePassword_IncorrectCurrentPassword_ThrowsIllegalArgumentException() {
        ChangePasswordRequest changeRequest = new ChangePasswordRequest();
        changeRequest.setCurrentPassword("wrongPassword");
        changeRequest.setNewPassword("newPassword456");

        existingUser.setPassword("encodedOldPassword");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("wrongPassword", "encodedOldPassword")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> userService.changePassword("testuser", changeRequest));
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }
}
