package com.accet.projecthub.service;

import com.accet.projecthub.dto.ProjectDto;
import com.accet.projecthub.dto.TeamMemberDto;
import com.accet.projecthub.entity.Project;
import com.accet.projecthub.repository.ProjectCommentRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class ProjectMapper {

    private final ProjectCommentRepository commentRepository;

    public ProjectMapper(ProjectCommentRepository commentRepository) {
        this.commentRepository = commentRepository;
    }

    public ProjectDto toDto(Project project, Set<Long> likedIds, Set<Long> bookmarkedIds) {
        List<TeamMemberDto> members = new ArrayList<>();
        project.getTeamMembers().forEach(m ->
                members.add(TeamMemberDto.builder()
                        .name(m.getName())
                        .rollNo(m.getRollNo())
                        .build()));

        return ProjectDto.builder()
                .id(project.getId())
                .title(project.getTitle())
                .description(project.getDescription())
                .deployLink(project.getDeployLink())
                .department(project.getDepartment())
                .category(project.getCategory())
                .year(project.getYear())
                .image(project.getImage())
                .status(project.getStatus().name())
                .likes(project.getLikesCount())
                .views(project.getViewsCount())
                .submittedAt(project.getSubmittedAt() == null
                        ? null : project.getSubmittedAt().toString())
                .updatedAt(project.getUpdatedAt() == null
                    ? null : project.getUpdatedAt().toString())
                .commentCount(commentRepository.countByProjectId(project.getId()))
                .submittedById(project.getSubmittedBy().getId())
                .submittedBy(project.getSubmittedBy().getName())
                .technologies(new ArrayList<>(project.getTechnologies()))
                .teamMembers(members)
                .likedByMe(likedIds != null && likedIds.contains(project.getId()))
                .bookmarkedByMe(bookmarkedIds != null && bookmarkedIds.contains(project.getId()))
                .build();
    }

    public ProjectDto toDto(Project project) {
        return toDto(project, null, null);
    }
}
