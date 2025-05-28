package com.example.culturalmapapp.dto;

import lombok.Data;
import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "Response DTO for a cultural activity.")
public class ActivityResponse {

    @Schema(description = "Unique ID of the activity.", example = "1")
    private Long id;

    @Schema(description = "Name of the cultural activity.", example = "Summer Music Festival")
    private String name;

    @Schema(description = "Detailed description of the cultural activity.", example = "An annual festival featuring various local and international artists.")
    private String description;

    @Schema(description = "Date and time of the activity.", example = "2024-07-20T18:00:00")
    private LocalDateTime dateTime;

    @Schema(description = "Latitude of the activity's location.", example = "34.052235")
    private Double latitude;

    @Schema(description = "Longitude of the activity's location.", example = "-118.243683")
    private Double longitude;

    @Schema(description = "Category of the activity.", example = "Music")
    private String category;

    @Schema(description = "Username of the user who produced/created the activity.", example = "producer_user")
    private String producerUsername; 
}
