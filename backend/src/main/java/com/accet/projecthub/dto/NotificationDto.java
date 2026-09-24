package com.accet.projecthub.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NotificationDto {
    private Long id;
    private String type;
    private String message;
    private Long projectId;
    private String projectTitle;
    private boolean read;
    private String createdAt;
}
