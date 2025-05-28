package com.example.culturalmapapp.dto;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@RequiredArgsConstructor // Lombok will create a constructor with all final fields
@Schema(description = "Response DTO containing the JWT access token.")
public class LoginResponse {

    @Schema(description = "The JWT access token.", example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyMSIsImlhdCI6MTYxNjQwNjQwMH0.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c", requiredMode = Schema.RequiredMode.REQUIRED)
    private final String accessToken;

    @Schema(description = "Type of the token.", example = "Bearer", requiredMode = Schema.RequiredMode.REQUIRED)
    private String tokenType = "Bearer";
}
