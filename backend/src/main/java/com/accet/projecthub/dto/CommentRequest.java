package com.accet.projecthub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CommentRequest {
    @NotBlank(message = "Comment cannot be empty")
    @Size(max = 1000, message = "Comment must be at most 1000 characters")
    private String content;
}
