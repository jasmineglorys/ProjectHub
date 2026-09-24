package com.accet.projecthub.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CommentDto {
    private Long id;
    private String content;
    private Long authorId;
    private String authorName;
    private Long projectId;
    private String projectTitle;
    private String createdAt;
}
