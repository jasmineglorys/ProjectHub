package com.accet.projecthub.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProjectFileDto {
    private Long id;
    private String fileType;
    private String fileName;
    private String contentType;
    private Long fileSize;
    private String downloadUrl;
}