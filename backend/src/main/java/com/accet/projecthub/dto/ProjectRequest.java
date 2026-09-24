package com.accet.projecthub.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ProjectRequest {

    @NotBlank(message = "Title is required")
    @Size(min = 5, max = 200, message = "Title must be between 5 and 200 characters")
    private String title;

    @NotBlank(message = "Description is required")
    @Size(min = 20, max = 4000, message = "Description must be between 20 and 4000 characters")
    private String description;

    @Size(max = 500, message = "Deploy link must be at most 500 characters")
    private String deployLink;

    @NotBlank(message = "Department is required")
    private String department;

    @NotBlank(message = "Category is required")
    private String category;

    @NotNull(message = "Project year is required")
    @Min(value = 2000, message = "Project year must be 2000 or later")
    @Max(value = 2100, message = "Project year is not valid")
    private Integer year;

    @Size(max = 200)
    private String image;

    @Size(max = 10)
    private List<String> supportingFileNames = new ArrayList<>();

    @Size(max = 10)
    private List<String> mediaFileNames = new ArrayList<>();

    @NotEmpty(message = "At least one technology is required")
    private List<String> technologies = new ArrayList<>();

    @Valid
    @NotEmpty(message = "At least one team member is required")
    @Size(max = 5, message = "A team can have at most 5 members")
    private List<TeamMemberDto> teamMembers = new ArrayList<>();
}
