package com.accet.projecthub.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ProfileActivityDto {
    private List<ProjectDto> pending;
    private List<ProjectDto> approved;
    private List<ProjectDto> rejected;
    private List<ProjectDto> liked;
    private List<CommentDto> comments;
}
