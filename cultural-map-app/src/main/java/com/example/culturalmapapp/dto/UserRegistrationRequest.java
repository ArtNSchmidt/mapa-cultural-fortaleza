package com.example.culturalmapapp.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "Request DTO for user registration.")
public class UserRegistrationRequest {

    @Schema(description = "Desired username for the new account.", example = "john_doe", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Username cannot be blank")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @Schema(description = "Desired password for the new account.", example = "Str0ngP@ssw0rd", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Password cannot be blank")
    @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
    private String password;

    @Schema(description = "Email address for the new account.", example = "john.doe@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Email should be valid")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;

    @Schema(description = "Role for the new user (e.g., CONSUMER, PRODUCER). Defaults to CONSUMER if not provided.", example = "CONSUMER")
    // Role can be optional, default is applied in service. If provided, it's validated there.
    // For stricter validation here, a custom annotation or @Pattern could be used.
    // For now, allowing it to be potentially blank here and handled by service logic.
    @Size(max = 50, message = "Role must not exceed 50 characters if provided")
    private String role; 
}
