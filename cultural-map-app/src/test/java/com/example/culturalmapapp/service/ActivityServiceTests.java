package com.example.culturalmapapp.service;

import com.example.culturalmapapp.dto.ActivityRequest;
import com.example.culturalmapapp.dto.ActivityResponse;
import com.example.culturalmapapp.exception.ResourceNotFoundException;
import com.example.culturalmapapp.model.CulturalActivity;
import com.example.culturalmapapp.model.User;
import com.example.culturalmapapp.repository.CulturalActivityRepository;
import com.example.culturalmapapp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ActivityServiceTests {

    @Mock
    private CulturalActivityRepository activityRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ActivityService activityService;

    private User producerUser;
    private User anotherUser;
    private User adminUser;
    private CulturalActivity activity;
    private ActivityRequest activityRequest;

    @BeforeEach
    void setUp() {
        producerUser = new User(1L, "producer", "password", "producer@example.com", "ROLE_PRODUCER");
        anotherUser = new User(2L, "another", "password", "another@example.com", "ROLE_CONSUMER");
        adminUser = new User(3L, "admin", "password", "admin@example.com", "ROLE_ADMIN");

        activity = new CulturalActivity(1L, "Test Activity", "Description", LocalDateTime.now(), 40.7128, -74.0060, "Music", producerUser);

        activityRequest = new ActivityRequest();
        activityRequest.setName("Updated Activity");
        activityRequest.setDescription("Updated Description");
        activityRequest.setDateTime(LocalDateTime.now().plusDays(1));
        activityRequest.setLatitude(40.7500);
        activityRequest.setLongitude(-73.9800);
        activityRequest.setCategory("Art");
    }

    @Test
    void testCreateActivity_Success() {
        when(userRepository.findByUsername("producer")).thenReturn(Optional.of(producerUser));
        when(activityRepository.save(any(CulturalActivity.class))).thenReturn(activity);

        ActivityResponse response = activityService.createActivity(activityRequest, "producer");

        assertNotNull(response);
        assertEquals(activity.getName(), response.getName()); // Name should be from the 'activity' object that 'save' returns
        assertEquals("producer", response.getProducerUsername());
        verify(activityRepository, times(1)).save(any(CulturalActivity.class));
    }

    @Test
    void testGetActivityById_Found() {
        when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));
        ActivityResponse response = activityService.getActivityById(1L);
        assertNotNull(response);
        assertEquals("Test Activity", response.getName());
    }

    @Test
    void testGetActivityById_NotFound_ThrowsResourceNotFoundException() {
        when(activityRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> activityService.getActivityById(1L));
    }
    
    @Test
    void testGetAllActivities_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        List<CulturalActivity> activities = Collections.singletonList(activity);
        Page<CulturalActivity> activityPage = new PageImpl<>(activities, pageable, activities.size());

        when(activityRepository.findAll(pageable)).thenReturn(activityPage);

        Page<ActivityResponse> responsePage = activityService.getAllActivities(pageable);

        assertEquals(1, responsePage.getTotalElements());
        assertEquals("Test Activity", responsePage.getContent().get(0).getName());
        verify(activityRepository, times(1)).findAll(pageable);
    }


    @Test
    void testUpdateActivity_ByOwner_Success() {
        when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));
        when(userRepository.findByUsername("producer")).thenReturn(Optional.of(producerUser));
        when(activityRepository.save(any(CulturalActivity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ActivityResponse response = activityService.updateActivity(1L, activityRequest, "producer");

        assertNotNull(response);
        assertEquals("Updated Activity", response.getName());
        assertEquals(producerUser.getUsername(), response.getProducerUsername());
        verify(activityRepository, times(1)).save(any(CulturalActivity.class));
    }

    @Test
    void testUpdateActivity_ByAdmin_Success() {
        when(activityRepository.findById(1L)).thenReturn(Optional.of(activity)); // activity owned by producerUser
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(activityRepository.save(any(CulturalActivity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ActivityResponse response = activityService.updateActivity(1L, activityRequest, "admin");

        assertNotNull(response);
        assertEquals("Updated Activity", response.getName());
        // Producer username should still reflect the original producer
        assertEquals(producerUser.getUsername(), response.getProducerUsername()); 
        verify(activityRepository, times(1)).save(any(CulturalActivity.class));
    }

    @Test
    void testUpdateActivity_ByNonOwner_ThrowsAccessDeniedException() {
        when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));
        when(userRepository.findByUsername("another")).thenReturn(Optional.of(anotherUser));

        assertThrows(AccessDeniedException.class, () -> activityService.updateActivity(1L, activityRequest, "another"));
        verify(activityRepository, never()).save(any(CulturalActivity.class));
    }

    @Test
    void testUpdateActivity_NotFound_ThrowsResourceNotFoundException() {
        when(activityRepository.findById(1L)).thenReturn(Optional.empty());
        // Need to mock userRepository as well, as it's called before the save
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(producerUser));


        assertThrows(ResourceNotFoundException.class, () -> activityService.updateActivity(1L, activityRequest, "producer"));
        verify(activityRepository, never()).save(any(CulturalActivity.class));
    }
    

    @Test
    void testDeleteActivity_ByOwner_Success() {
        when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));
        when(userRepository.findByUsername("producer")).thenReturn(Optional.of(producerUser));
        doNothing().when(activityRepository).delete(activity);

        activityService.deleteActivity(1L, "producer");
        verify(activityRepository, times(1)).delete(activity);
    }

    @Test
    void testDeleteActivity_ByAdmin_Success() {
        when(activityRepository.findById(1L)).thenReturn(Optional.of(activity)); // activity owned by producerUser
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        doNothing().when(activityRepository).delete(activity);

        activityService.deleteActivity(1L, "admin");
        verify(activityRepository, times(1)).delete(activity);
    }
    
    @Test
    void testDeleteActivity_ByNonOwner_ThrowsAccessDeniedException() {
        when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));
        when(userRepository.findByUsername("another")).thenReturn(Optional.of(anotherUser));

        assertThrows(AccessDeniedException.class, () -> activityService.deleteActivity(1L, "another"));
        verify(activityRepository, never()).delete(any(CulturalActivity.class));
    }
    
    @Test
    void testDeleteActivity_NotFound_ThrowsResourceNotFoundException() {
        when(activityRepository.findById(1L)).thenReturn(Optional.empty());
        // Need to mock userRepository as well, as it's called before the delete
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(producerUser));

        assertThrows(ResourceNotFoundException.class, () -> activityService.deleteActivity(1L, "producer"));
        verify(activityRepository, never()).delete(any(CulturalActivity.class));
    }

    @Test
    void testGetActivitiesNear_HaversineFilteringAndPagination() {
        Pageable pageable = PageRequest.of(0, 10);
        double userLat = 40.7000;
        double userLon = -74.0000;
        double radiusKm = 10.0; // 10km

        // Activity1: within radius
        CulturalActivity activity1 = new CulturalActivity(1L, "Activity 1", "Desc1", LocalDateTime.now(), 40.7050, -74.0050, "Music", producerUser); // Approx 0.6km
        // Activity2: outside radius
        CulturalActivity activity2 = new CulturalActivity(2L, "Activity 2", "Desc2", LocalDateTime.now(), 40.8000, -74.1000, "Art", producerUser); // Approx >10km
        // Activity3: within radius
        CulturalActivity activity3 = new CulturalActivity(3L, "Activity 3", "Desc3", LocalDateTime.now(), 40.7010, -74.0010, "Theatre", producerUser); // Approx 0.1km
        
        // Mock repository to return these activities for the bounding box query
        // The bounding box would be wider, so all these might be returned by it
        List<CulturalActivity> activitiesInBoundingBox = List.of(activity1, activity2, activity3);
        when(activityRepository.findByLocationBoundingBox(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(activitiesInBoundingBox);

        Page<ActivityResponse> resultPage = activityService.getActivitiesNear(userLat, userLon, radiusKm, pageable);

        assertEquals(2, resultPage.getTotalElements()); // activity2 should be filtered out by Haversine
        assertEquals(2, resultPage.getContent().size());
        assertTrue(resultPage.getContent().stream().anyMatch(ar -> ar.getName().equals("Activity 1")));
        assertTrue(resultPage.getContent().stream().anyMatch(ar -> ar.getName().equals("Activity 3")));
        assertFalse(resultPage.getContent().stream().anyMatch(ar -> ar.getName().equals("Activity 2")));
    }
    
    @Test
    void testGetActivitiesNear_HaversineFiltering_NullCoordinates() {
        Pageable pageable = PageRequest.of(0, 10);
        double userLat = 40.7000;
        double userLon = -74.0000;
        double radiusKm = 10.0;

        CulturalActivity activityWithNullCoords = new CulturalActivity(1L, "Null Coords", "Desc", LocalDateTime.now(), null, null, "Test", producerUser);
        List<CulturalActivity> activitiesInBoundingBox = List.of(activityWithNullCoords);
        when(activityRepository.findByLocationBoundingBox(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(activitiesInBoundingBox);

        Page<ActivityResponse> resultPage = activityService.getActivitiesNear(userLat, userLon, radiusKm, pageable);
        assertTrue(resultPage.getContent().isEmpty()); // Activity with null coords should be filtered out
    }
}
