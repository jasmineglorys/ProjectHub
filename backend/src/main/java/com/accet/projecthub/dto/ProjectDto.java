package com.accet.projecthub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectDto {
    private Long id;
    private String title;
    private String description;
    private String deployLink;
    private String department;
    private String category;
    private Integer year;
    private String academicYear;
    private String image;
    private String status;
    private boolean winner;
    private Integer likes;
    private Integer views;
    private String submittedAt;
    private String updatedAt;
    private Long commentCount;
    private Long submittedById;
    private String submittedBy;
    private List<String> technologies;
    private List<TeamMemberDto> teamMembers;
    private List<ProjectFileDto> files;
    private Boolean likedByMe;
    private Boolean bookmarkedByMe;
}
