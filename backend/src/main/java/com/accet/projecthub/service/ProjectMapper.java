package com.accet.projecthub.service;

import com.accet.projecthub.dto.ProjectDto;
import com.accet.projecthub.dto.ProjectFileDto;
import com.accet.projecthub.dto.TeamMemberDto;
import com.accet.projecthub.entity.Project;
import com.accet.projecthub.repository.BookmarkRepository;
import com.accet.projecthub.repository.ProjectCommentRepository;
import com.accet.projecthub.repository.ProjectFileRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class ProjectMapper {

    private final ProjectCommentRepository commentRepository;
    private final ProjectFileRepository fileRepository;
    private final BookmarkRepository bookmarkRepository;

    public ProjectMapper(ProjectCommentRepository commentRepository, ProjectFileRepository fileRepository,
                         BookmarkRepository bookmarkRepository) {
        this.commentRepository = commentRepository;
        this.fileRepository = fileRepository;
        this.bookmarkRepository = bookmarkRepository;
    }

    public ProjectDto toDto(Project project, Set<Long> likedIds, Set<Long> bookmarkedIds) {
        long bookmarkCount = bookmarkRepository.countByProjectId(project.getId());
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
                .achievement(project.getAchievement())
                .deployLink(project.getDeployLink())
                .department(project.getDepartment())
                .category(project.getCategory())
                .year(project.getYear())
                .academicYear(project.getAcademicYear())
                .image(project.getImage())
                .status(project.getStatus().name())
                .likes(project.getLikesCount())
                .views(project.getViewsCount())
                .bookmarks(bookmarkCount)
                .downloads(project.getDownloadsCount())
                .popularity((long) project.getViewsCount() + project.getLikesCount()
                    + bookmarkCount + project.getDownloadsCount())
                .submittedAt(project.getSubmittedAt() == null
                        ? null : project.getSubmittedAt().toString())
                .updatedAt(project.getUpdatedAt() == null
                    ? null : project.getUpdatedAt().toString())
                .commentCount(commentRepository.countByProjectId(project.getId()))
                .submittedById(project.getSubmittedBy().getId())
                .submittedBy(project.getSubmittedBy().getName())
                .technologies(new ArrayList<>(project.getTechnologies()))
                .teamMembers(members)
                .files(fileRepository.findByProjectIdOrderByIdAsc(project.getId()).stream()
                    .map(file -> ProjectFileDto.builder()
                        .id(file.getId())
                        .fileType(file.getFileType())
                        .fileName(file.getFileName())
                        .contentType(file.getContentType())
                        .fileSize(file.getFileSize())
                        .downloadUrl("/api/projects/" + project.getId() + "/files/" + file.getId())
                        .build())
                    .toList())
                .likedByMe(likedIds != null && likedIds.contains(project.getId()))
                .bookmarkedByMe(bookmarkedIds != null && bookmarkedIds.contains(project.getId()))
                .build();
    }

    public ProjectDto toDto(Project project) {
        return toDto(project, null, null);
    }
}
