package com.accet.projecthub.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StatusUpdateRequest {

    /** One of: PENDING, APPROVED. Use dedicated endpoints for rejection and change requests. */
    @NotBlank(message = "Status is required")
    private String status;
}
