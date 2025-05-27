package com.example.culturalmapapp.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "Request DTO for updating the current user's profile information.")
public class UpdateUserProfileRequest {

    @Schema(description = "New email address for the user.", example = "john.new.doe@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Email should be valid")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;

    // Add other updatable fields here if any, e.g., first name, last name
    // For now, only email is considered updatable as per the initial requirements.
    // Username and roles should not be updatable through this DTO.
}
