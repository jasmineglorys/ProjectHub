package com.accet.projecthub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RequestChangesRequest {

    @NotBlank(message = "A reason is required")
    @Size(max = 450, message = "The reason must be 450 characters or fewer")
    private String reason;
}