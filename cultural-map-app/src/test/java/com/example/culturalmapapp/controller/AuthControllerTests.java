package com.example.culturalmapapp.controller;

import com.example.culturalmapapp.dto.LoginRequest;
import com.example.culturalmapapp.dto.LoginResponse;
import com.example.culturalmapapp.dto.UserRegistrationRequest;
import com.example.culturalmapapp.exception.UserAlreadyExistsException;
import com.example.culturalmapapp.model.User;
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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import com.example.culturalmapapp.config.SecurityConfig; // Import your security config
import com.example.culturalmapapp.service.CustomUserDetailsService; // Import if SecurityConfig depends on it directly for bean creation
import com.example.culturalmapapp.filter.JwtAuthenticationFilter; // Import if SecurityConfig depends on it

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.is;

@WebMvcTest(AuthController.class)
// Import SecurityConfig to apply security filters. 
// Also import other beans that SecurityConfig might depend on if they are not already scanned by WebMvcTest.
@Import({SecurityConfig.class, CustomUserDetailsService.class, JwtTokenProvider.class, JwtAuthenticationFilter.class})
public class AuthControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;
    
    // CustomUserDetailsService is already imported via @Import if needed by SecurityConfig for bean creation
    // No need to @MockBean it if its interactions are not directly tested here or if SecurityConfig uses a real one.
    // If SecurityConfig uses a real CustomUserDetailsService, and that service has dependencies, those might need mocking.
    // However, for controller tests, typically the full service chain is mocked.
    @MockBean
    private CustomUserDetailsService customUserDetailsService;


    @Autowired
    private ObjectMapper objectMapper;

    private UserRegistrationRequest registrationRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp(WebApplicationContext webApplicationContext) {
         // Apply Spring Security to MockMvc
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        registrationRequest = new UserRegistrationRequest();
        registrationRequest.setUsername("testuser");
        registrationRequest.setPassword("password123");
        registrationRequest.setEmail("test@example.com");
        registrationRequest.setRole("CONSUMER");

        loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password123");
    }

    @Test
    void testRegisterUser_ValidRequest_ReturnsCreated() throws Exception {
        User user = new User(1L, "testuser", "encodedPass", "test@example.com", "ROLE_CONSUMER");
        when(userService.registerUser(any(UserRegistrationRequest.class))).thenReturn(user);

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registrationRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message", is("User registered successfully: testuser")));
    }

    @Test
    void testRegisterUser_InvalidRequest_MissingUsername_ReturnsBadRequest() throws Exception {
        registrationRequest.setUsername(""); // Invalid - @NotBlank

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registrationRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0]", is("username: Username cannot be blank")));
    }
    
    @Test
    void testRegisterUser_InvalidRequest_ShortPassword_ReturnsBadRequest() throws Exception {
        registrationRequest.setPassword("123"); // Invalid - @Size(min=6)

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registrationRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0]", is("password: Password must be between 6 and 100 characters")));
    }


    @Test
    void testRegisterUser_UserAlreadyExists_ReturnsBadRequest() throws Exception {
        when(userService.registerUser(any(UserRegistrationRequest.class)))
                .thenThrow(new UserAlreadyExistsException("Username already exists: testuser"));

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registrationRequest)))
                .andExpect(status().isConflict()) // GlobalExceptionHandler maps this to 409 CONFLICT
                .andExpect(jsonPath("$.message", is("Username already exists: testuser")));
    }

    @Test
    void testAuthenticateUser_ValidCredentials_ReturnsToken() throws Exception {
        Authentication authentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(jwtTokenProvider.generateToken(authentication)).thenReturn("mock.jwt.token");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", is("mock.jwt.token")))
                .andExpect(jsonPath("$.tokenType", is("Bearer")));
    }

    @Test
    void testAuthenticateUser_InvalidCredentials_ReturnsUnauthorized() throws Exception {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized()); // Spring Security's default for BadCredentialsException
    }
    
    @Test
    void testAuthenticateUser_InvalidRequest_MissingUsername_ReturnsBadRequest() throws Exception {
        loginRequest.setUsername(""); // Invalid - @NotBlank

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0]", is("username: Username cannot be blank")));
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"CONSUMER"})
    void testGetCurrentUser_Authenticated_ReturnsUserInfo() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Currently logged in as: testuser with roles: [ROLE_CONSUMER]")));
    }

    @Test
    void testGetCurrentUser_Unauthenticated_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                 .andExpect(jsonPath("$.message", is("No user authenticated.")));
    }
}
