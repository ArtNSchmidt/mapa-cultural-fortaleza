package com.example.culturalmapapp.dto;

import lombok.Data;
import java.util.Set;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "Response DTO for user profile information.")
public class UserProfileResponse {

    @Schema(description = "Unique ID of the user.", example = "1")
    private Long id;

    @Schema(description = "Username of the user.", example = "john_doe")
    private String username;

    @Schema(description = "Email address of the user.", example = "john.doe@example.com")
    private String email;

    @Schema(description = "Set of roles assigned to the user.", example = "[\"ROLE_CONSUMER\", \"ROLE_PRODUCER\"]")
    private Set<String> roles; 
}
