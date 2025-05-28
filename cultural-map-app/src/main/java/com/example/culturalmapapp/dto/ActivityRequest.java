package com.example.culturalmapapp.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "Request DTO for creating or updating a cultural activity.")
public class ActivityRequest {

    @Schema(description = "Name of the cultural activity.", example = "Summer Music Festival", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Activity name cannot be blank")
    @Size(max = 255, message = "Activity name must not exceed 255 characters")
    private String name;

    @Schema(description = "Detailed description of the cultural activity.", example = "An annual festival featuring various local and international artists.", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Description cannot be blank")
    private String description; 

    @Schema(description = "Date and time of the activity.", example = "2024-07-20T18:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Date and time cannot be null")
    // @FutureOrPresent(message = "Date and time must be in the present or future") 
    private LocalDateTime dateTime;

    @Schema(description = "Latitude of the activity's location.", example = "34.052235", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Latitude cannot be null")
    @Min(value = -90, message = "Latitude must be between -90 and 90")
    @Max(value = 90, message = "Latitude must be between -90 and 90")
    private Double latitude;

    @Schema(description = "Longitude of the activity's location.", example = "-118.243683", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Longitude cannot be null")
    @Min(value = -180, message = "Longitude must be between -180 and 180")
    @Max(value = 180, message = "Longitude must be between -180 and 180")
    private Double longitude;

    @Schema(description = "Category of the activity (e.g., Music, Art, Theatre).", example = "Music", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Category cannot be blank")
    @Size(max = 100, message = "Category must not exceed 100 characters")
    private String category;
    // producer_id will be handled from the authenticated user in the service layer
}
