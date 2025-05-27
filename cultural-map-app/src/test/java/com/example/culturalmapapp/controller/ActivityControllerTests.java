package com.example.culturalmapapp.controller;

import com.example.culturalmapapp.config.SecurityConfig;
import com.example.culturalmapapp.dto.ActivityRequest;
import com.example.culturalmapapp.dto.ActivityResponse;
import com.example.culturalmapapp.filter.JwtAuthenticationFilter;
import com.example.culturalmapapp.service.ActivityService;
import com.example.culturalmapapp.service.CustomUserDetailsService;
import com.example.culturalmapapp.service.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ActivityController.class)
@Import({SecurityConfig.class, CustomUserDetailsService.class, JwtTokenProvider.class, JwtAuthenticationFilter.class})
public class ActivityControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ActivityService activityService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider; // Required by JwtAuthenticationFilter

    @MockBean
    private CustomUserDetailsService customUserDetailsService; // Required by SecurityConfig

    @Autowired
    private ObjectMapper objectMapper;

    private ActivityRequest activityRequest;
    private ActivityResponse activityResponse;

    @BeforeEach
    void setUp(WebApplicationContext webApplicationContext) {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        activityRequest = new ActivityRequest();
        activityRequest.setName("Festival");
        activityRequest.setDescription("Annual music festival");
        activityRequest.setDateTime(LocalDateTime.now().plusMonths(1));
        activityRequest.setLatitude(34.0522);
        activityRequest.setLongitude(-118.2437);
        activityRequest.setCategory("Music");

        activityResponse = new ActivityResponse();
        activityResponse.setId(1L);
        activityResponse.setName("Festival");
        activityResponse.setProducerUsername("produceruser");
    }

    // --- POST /api/activities ---
    @Test
    @WithMockUser(username = "produceruser", roles = {"PRODUCER"})
    void testCreateActivity_AsProducer_ReturnsCreated() throws Exception {
        given(activityService.createActivity(any(ActivityRequest.class), eq("produceruser"))).willReturn(activityResponse);

        mockMvc.perform(post("/api/activities")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(activityRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Festival")))
                .andExpect(jsonPath("$.producerUsername", is("produceruser")));
    }

    @Test
    @WithMockUser(username = "consumeruser", roles = {"CONSUMER"})
    void testCreateActivity_AsConsumer_ReturnsForbidden() throws Exception {
        mockMvc.perform(post("/api/activities")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(activityRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    void testCreateActivity_Unauthenticated_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/activities")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(activityRequest)))
                .andExpect(status().isUnauthorized()); // Or 403 if filter chain processes differently before controller
    }
    
    @Test
    @WithMockUser(username = "produceruser", roles = {"PRODUCER"})
    void testCreateActivity_InvalidRequestBody_ReturnsBadRequest() throws Exception {
        ActivityRequest invalidRequest = new ActivityRequest(); // Missing name, etc.
        mockMvc.perform(post("/api/activities")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").exists());
    }

    // --- GET /api/activities/{id} ---
    @Test
    void testGetActivityById_Found_ReturnsOk() throws Exception {
        given(activityService.getActivityById(1L)).willReturn(activityResponse);
        mockMvc.perform(get("/api/activities/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Festival")));
    }

    // --- GET /api/activities ---
    @Test
    void testGetAllActivities_ReturnsOk() throws Exception {
        Pageable pageable = PageRequest.of(0, 10);
        Page<ActivityResponse> page = new PageImpl<>(Collections.singletonList(activityResponse), pageable, 1);
        given(activityService.getAllActivities(any(Pageable.class))).willReturn(page);

        mockMvc.perform(get("/api/activities?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name", is("Festival")));
    }
    
    // --- PUT /api/activities/{id} ---
    @Test
    @WithMockUser(username = "produceruser", roles = {"PRODUCER"})
    void testUpdateActivity_AsOwnerProducer_ReturnsOk() throws Exception {
        given(activityService.updateActivity(eq(1L), any(ActivityRequest.class), eq("produceruser"))).willReturn(activityResponse);

        mockMvc.perform(put("/api/activities/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(activityRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Festival")));
    }

    @Test
    @WithMockUser(username = "otherproducer", roles = {"PRODUCER"})
    void testUpdateActivity_AsNonOwnerProducer_ReturnsForbidden() throws Exception {
        given(activityService.updateActivity(eq(1L), any(ActivityRequest.class), eq("otherproducer")))
            .willThrow(new AccessDeniedException("Not owner"));

        mockMvc.perform(put("/api/activities/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(activityRequest)))
                .andExpect(status().isForbidden());
    }
    
    @Test
    @WithMockUser(username = "adminuser", roles = {"ADMIN"})
    void testUpdateActivity_AsAdmin_ReturnsOk() throws Exception {
        given(activityService.updateActivity(eq(1L), any(ActivityRequest.class), eq("adminuser"))).willReturn(activityResponse);

        mockMvc.perform(put("/api/activities/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(activityRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Festival")));
    }
    
    @Test
    @WithMockUser(username = "produceruser", roles = {"PRODUCER"})
    void testUpdateActivity_InvalidBody_ReturnsBadRequest() throws Exception {
         ActivityRequest invalidRequest = new ActivityRequest(); // Missing name
         mockMvc.perform(put("/api/activities/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    // --- DELETE /api/activities/{id} ---
    @Test
    @WithMockUser(username = "produceruser", roles = {"PRODUCER"})
    void testDeleteActivity_AsOwnerProducer_ReturnsNoContent() throws Exception {
        doNothing().when(activityService).deleteActivity(eq(1L), eq("produceruser"));
        mockMvc.perform(delete("/api/activities/1"))
                .andExpect(status().isNoContent());
    }
    
    @Test
    @WithMockUser(username = "otherproducer", roles = {"PRODUCER"})
    void testDeleteActivity_AsNonOwnerProducer_ReturnsForbidden() throws Exception {
        doThrow(new AccessDeniedException("Not owner")).when(activityService).deleteActivity(eq(1L), eq("otherproducer"));
        mockMvc.perform(delete("/api/activities/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "adminuser", roles = {"ADMIN"})
    void testDeleteActivity_AsAdmin_ReturnsNoContent() throws Exception {
        doNothing().when(activityService).deleteActivity(eq(1L), eq("adminuser"));
        mockMvc.perform(delete("/api/activities/1"))
                .andExpect(status().isNoContent());
    }

    // --- GET /api/activities/search ---
    @Test
    void testSearchActivitiesByCategory_ReturnsOk() throws Exception {
        Pageable pageable = PageRequest.of(0, 5);
        Page<ActivityResponse> page = new PageImpl<>(Collections.singletonList(activityResponse), pageable, 1);
        given(activityService.getActivitiesByCategory(eq("Music"), any(Pageable.class))).willReturn(page);

        mockMvc.perform(get("/api/activities/search?category=Music&page=0&size=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name", is("Festival")));
    }

    // --- GET /api/activities/near ---
    @Test
    void testGetActivitiesNear_ReturnsOk() throws Exception {
        Pageable pageable = PageRequest.of(0, 5);
        Page<ActivityResponse> page = new PageImpl<>(List.of(activityResponse), pageable, 1);
        given(activityService.getActivitiesNear(anyDouble(), anyDouble(), anyDouble(), any(Pageable.class))).willReturn(page);

        mockMvc.perform(get("/api/activities/near?latitude=34.05&longitude=-118.24&radius=10&page=0&size=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name", is("Festival")));
    }
}
