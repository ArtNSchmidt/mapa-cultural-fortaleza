package com.example.culturalmapapp.service;

import com.example.culturalmapapp.dto.ActivityRequest;
import com.example.culturalmapapp.dto.ActivityResponse;
import com.example.culturalmapapp.exception.ResourceNotFoundException;
import com.example.culturalmapapp.model.CulturalActivity;
import com.example.culturalmapapp.model.User;
import com.example.culturalmapapp.repository.CulturalActivityRepository;
import com.example.culturalmapapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ActivityService {

    @Autowired
    private CulturalActivityRepository activityRepository;

    @Autowired
    private UserRepository userRepository; // To fetch producer details

    public ActivityResponse createActivity(ActivityRequest request, String username) {
        User producer = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));

        CulturalActivity activity = new CulturalActivity();
        activity.setName(request.getName());
        activity.setDescription(request.getDescription());
        activity.setDateTime(request.getDateTime());
        activity.setLatitude(request.getLatitude());
        activity.setLongitude(request.getLongitude());
        activity.setCategory(request.getCategory());
        activity.setProducer(producer);

        CulturalActivity savedActivity = activityRepository.save(activity);
        return mapToActivityResponse(savedActivity);
    }

    public ActivityResponse getActivityById(Long id) {
        CulturalActivity activity = activityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Activity not found with id: " + id));
        return mapToActivityResponse(activity);
    }

    public Page<ActivityResponse> getAllActivities(Pageable pageable) {
        Page<CulturalActivity> activityPage = activityRepository.findAll(pageable);
        return activityPage.map(this::mapToActivityResponse);
    }

    public ActivityResponse updateActivity(Long id, ActivityRequest request, String username) {
        CulturalActivity activity = activityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Activity not found with id: " + id));
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));

        // Admin can update any activity, producers can only update their own
        if (!currentUser.getRole().equals("ROLE_ADMIN") && !activity.getProducer().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You are not authorized to update this activity.");
        }

        activity.setName(request.getName());
        activity.setDescription(request.getDescription());
        activity.setDateTime(request.getDateTime());
        activity.setLatitude(request.getLatitude());
        activity.setLongitude(request.getLongitude());
        activity.setCategory(request.getCategory());

        CulturalActivity updatedActivity = activityRepository.save(activity);
        return mapToActivityResponse(updatedActivity);
    }

    public void deleteActivity(Long id, String username) {
        CulturalActivity activity = activityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Activity not found with id: " + id));
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));

        // Admin can delete any activity, producers can only delete their own
        if (!currentUser.getRole().equals("ROLE_ADMIN") && !activity.getProducer().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You are not authorized to delete this activity.");
        }
        activityRepository.delete(activity);
    }

    public Page<ActivityResponse> getActivitiesByCategory(String category, Pageable pageable) {
        Page<CulturalActivity> activityPage = activityRepository.findByCategory(category, pageable);
        return activityPage.map(this::mapToActivityResponse);
    }

    public Page<ActivityResponse> getActivitiesNear(Double latitude, Double longitude, Double radiusKm, Pageable pageable) {
        // Basic bounding box approach for now.
        // Radius in degrees (approximate, as 1 degree lat/lon is not constant km)
        double latDegrees = radiusKm / 111.0; // Approx 111 km per degree latitude
        double lonDegrees = radiusKm / (111.0 * Math.cos(Math.toRadians(latitude))); // Adjust for longitude

        List<CulturalActivity> activitiesInBoundingBox = activityRepository.findByLocationBoundingBox(
                latitude - latDegrees, latitude + latDegrees,
                longitude - lonDegrees, longitude + lonDegrees
        );

        // Further filter by precise Haversine distance
        List<ActivityResponse> filteredActivities = activitiesInBoundingBox.stream()
                .filter(activity -> haversineDistance(latitude, longitude, activity.getLatitude(), activity.getLongitude()) <= radiusKm)
                .map(this::mapToActivityResponse)
                .collect(Collectors.toList());
        
        // Manual pagination for the in-memory filtered list
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filteredActivities.size());

        if (start > filteredActivities.size()) {
            return new PageImpl<>(List.of(), pageable, filteredActivities.size());
        }
        
        List<ActivityResponse> pageContent = filteredActivities.subList(start, end);
        return new PageImpl<>(pageContent, pageable, filteredActivities.size());
    }

    private ActivityResponse mapToActivityResponse(CulturalActivity activity) {
        ActivityResponse response = new ActivityResponse();
        response.setId(activity.getId());
        response.setName(activity.getName());
        response.setDescription(activity.getDescription());
        response.setDateTime(activity.getDateTime());
        response.setLatitude(activity.getLatitude());
        response.setLongitude(activity.getLongitude());
        response.setCategory(activity.getCategory());
        if (activity.getProducer() != null) {
            response.setProducerUsername(activity.getProducer().getUsername());
        }
        return response;
    }

    // Haversine distance calculation
    private static final double EARTH_RADIUS_KM = 6371.0;

    private double haversineDistance(Double userLat, Double userLon, Double activityLat, Double activityLon) {
        if (userLat == null || userLon == null || activityLat == null || activityLon == null) {
            return Double.MAX_VALUE; // Cannot calculate distance if any coordinate is missing
        }
        
        double lat1 = userLat;
        double lon1 = userLon;
        double lat2 = activityLat;
        double lon2 = activityLon;

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double radLat1 = Math.toRadians(lat1);
        double radLat2 = Math.toRadians(lat2);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.sin(dLon / 2) * Math.sin(dLon / 2) * Math.cos(radLat1) * Math.cos(radLat2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }
}
