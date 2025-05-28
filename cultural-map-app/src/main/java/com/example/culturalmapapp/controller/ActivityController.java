package com.example.culturalmapapp.controller;

import com.example.culturalmapapp.dto.ActivityRequest;
import com.example.culturalmapapp.dto.ActivityResponse;
import com.example.culturalmapapp.service.ActivityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid; // Already present but good to confirm
// import java.util.List; // No longer returning List for paginated endpoints

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;


@Tag(name = "Cultural Activities", description = "Endpoints for managing cultural activities")
@RestController
@RequestMapping("/api/activities")
public class ActivityController {

    @Autowired
    private ActivityService activityService;

    @Operation(summary = "Create a new cultural activity",
                 description = "Allows PRODUCER or ADMIN users to create a new cultural activity. The producer is automatically assigned based on the authenticated user.",
                 security = @SecurityRequirement(name = "bearerAuth"),
                 responses = {
                     @ApiResponse(responseCode = "201", description = "Activity created successfully",
                                  content = @Content(mediaType = "application/json", schema = @Schema(implementation = ActivityResponse.class))),
                     @ApiResponse(responseCode = "400", description = "Invalid input data"),
                     @ApiResponse(responseCode = "401", description = "User not authenticated"),
                     @ApiResponse(responseCode = "403", description = "User not authorized (not a PRODUCER or ADMIN)")
                 })
    @PostMapping
    @PreAuthorize("hasRole('ROLE_PRODUCER')") // ADMIN inherits PRODUCER role via RoleHierarchy
    public ResponseEntity<ActivityResponse> createActivity(@Valid @RequestBody ActivityRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        ActivityResponse response = activityService.createActivity(request, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get an activity by its ID",
                 description = "Retrieves a specific cultural activity by its unique ID. Publicly accessible.",
                 responses = {
                     @ApiResponse(responseCode = "200", description = "Activity found",
                                  content = @Content(mediaType = "application/json", schema = @Schema(implementation = ActivityResponse.class))),
                     @ApiResponse(responseCode = "404", description = "Activity not found")
                 })
    @GetMapping("/{id}")
    public ResponseEntity<ActivityResponse> getActivityById(
            @Parameter(description = "ID of the activity to retrieve", required = true) @PathVariable Long id) {
        ActivityResponse response = activityService.getActivityById(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get all cultural activities (paginated)",
                 description = "Retrieves a paginated list of all cultural activities. Publicly accessible. Supports pagination and sorting via Pageable parameters (e.g., ?page=0&size=10&sort=name,asc).",
                 responses = {
                     @ApiResponse(responseCode = "200", description = "List of activities retrieved",
                                  content = @Content(mediaType = "application/json", schema = @Schema(implementation = Page.class))) // Note: Actual content is Page<ActivityResponse>
                 })
    @GetMapping
    public ResponseEntity<Page<ActivityResponse>> getAllActivities(
            @Parameter(description = "Pagination and sorting information") Pageable pageable) {
        Page<ActivityResponse> responses = activityService.getAllActivities(pageable);
        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "Update an existing cultural activity",
                 description = "Allows the original PRODUCER or an ADMIN to update an existing cultural activity.",
                 security = @SecurityRequirement(name = "bearerAuth"),
                 responses = {
                     @ApiResponse(responseCode = "200", description = "Activity updated successfully",
                                  content = @Content(mediaType = "application/json", schema = @Schema(implementation = ActivityResponse.class))),
                     @ApiResponse(responseCode = "400", description = "Invalid input data"),
                     @ApiResponse(responseCode = "401", description = "User not authenticated"),
                     @ApiResponse(responseCode = "403", description = "User not authorized to update this activity"),
                     @ApiResponse(responseCode = "404", description = "Activity not found")
                 })
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_PRODUCER')") // ADMIN inherits PRODUCER role
    public ResponseEntity<?> updateActivity(
            @Parameter(description = "ID of the activity to update", required = true) @PathVariable Long id,
            @Valid @RequestBody ActivityRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        try {
            ActivityResponse response = activityService.updateActivity(id, request, username);
            return ResponseEntity.ok(response);
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @Operation(summary = "Delete a cultural activity",
                 description = "Allows the original PRODUCER or an ADMIN to delete a cultural activity.",
                 security = @SecurityRequirement(name = "bearerAuth"),
                 responses = {
                     @ApiResponse(responseCode = "204", description = "Activity deleted successfully"),
                     @ApiResponse(responseCode = "401", description = "User not authenticated"),
                     @ApiResponse(responseCode = "403", description = "User not authorized to delete this activity"),
                     @ApiResponse(responseCode = "404", description = "Activity not found")
                 })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_PRODUCER')") // ADMIN inherits PRODUCER role
    public ResponseEntity<?> deleteActivity(
            @Parameter(description = "ID of the activity to delete", required = true) @PathVariable Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        try {
            activityService.deleteActivity(id, username);
            return ResponseEntity.noContent().build();
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @Operation(summary = "Search activities by category (paginated)",
                 description = "Retrieves a paginated list of cultural activities filtered by a specific category. Publicly accessible.",
                 responses = {
                     @ApiResponse(responseCode = "200", description = "List of activities retrieved",
                                  content = @Content(mediaType = "application/json", schema = @Schema(implementation = Page.class)))
                 })
    @GetMapping("/search")
    public ResponseEntity<Page<ActivityResponse>> searchActivitiesByCategory(
            @Parameter(description = "Category to search for", required = true) @RequestParam String category,
            @Parameter(description = "Pagination and sorting information") Pageable pageable) {
        Page<ActivityResponse> responses = activityService.getActivitiesByCategory(category, pageable);
        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "Find activities near a location (paginated)",
                 description = "Retrieves a paginated list of cultural activities within a specified radius (in kilometers) of a given latitude and longitude. Publicly accessible.",
                 responses = {
                     @ApiResponse(responseCode = "200", description = "List of activities retrieved",
                                  content = @Content(mediaType = "application/json", schema = @Schema(implementation = Page.class)))
                 })
    @GetMapping("/near")
    public ResponseEntity<Page<ActivityResponse>> getActivitiesNear(
            @Parameter(description = "Latitude of the center point", required = true) @RequestParam Double latitude,
            @Parameter(description = "Longitude of the center point", required = true) @RequestParam Double longitude,
            @Parameter(description = "Radius in kilometers", required = true) @RequestParam Double radius,
            @Parameter(description = "Pagination and sorting information") Pageable pageable) { 
        Page<ActivityResponse> responses = activityService.getActivitiesNear(latitude, longitude, radius, pageable);
        return ResponseEntity.ok(responses);
    }
}
