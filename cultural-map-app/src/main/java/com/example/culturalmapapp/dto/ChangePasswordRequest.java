package com.example.culturalmapapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "Request DTO for changing the current user's password.")
public class ChangePasswordRequest {

    @Schema(description = "The user's current password.", example = "OldP@ssw0rd1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Current password cannot be blank")
    private String currentPassword;

    @Schema(description = "The desired new password.", example = "NewS3cureP@ss!", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "New password cannot be blank")
    @Size(min = 6, max = 100, message = "New password must be between 6 and 100 characters")
    private String newPassword;
}
