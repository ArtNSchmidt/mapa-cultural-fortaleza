package com.example.culturalmapapp.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "Request DTO for user login.")
public class LoginRequest {

    @Schema(description = "Username of the user.", example = "john_doe", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Username cannot be blank")
    private String username;

    @Schema(description = "Password of the user.", example = "Str0ngP@ssw0rd", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Password cannot be blank")
    private String password;
}
