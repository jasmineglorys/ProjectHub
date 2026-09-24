package com.accet.projecthub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ToggleResponse {
    private Long projectId;
    private boolean active;
    private Integer likes;
    private String message;
}
