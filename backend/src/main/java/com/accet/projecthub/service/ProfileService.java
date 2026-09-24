package com.accet.projecthub.service;

import com.accet.projecthub.dto.CommentDto;
import com.accet.projecthub.dto.ProfileActivityDto;
import com.accet.projecthub.dto.ProjectDto;
import com.accet.projecthub.entity.Project;
import com.accet.projecthub.entity.ProjectComment;
import com.accet.projecthub.entity.ProjectStatus;
import com.accet.projecthub.repository.ProjectCommentRepository;
import com.accet.projecthub.repository.ProjectLikeRepository;
import com.accet.projecthub.repository.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
public class ProfileService {

    private final ProjectRepository projectRepository;
    private final ProjectLikeRepository likeRepository;
    private final ProjectCommentRepository commentRepository;
    private final ProjectMapper projectMapper;
    private final CommentService commentService;

    public ProfileService(ProjectRepository projectRepository,
                          ProjectLikeRepository likeRepository,
                          ProjectCommentRepository commentRepository,
                          ProjectMapper projectMapper,
                          CommentService commentService) {
        this.projectRepository = projectRepository;
        this.likeRepository = likeRepository;
        this.commentRepository = commentRepository;
        this.projectMapper = projectMapper;
        this.commentService = commentService;
    }

    @Transactional(readOnly = true)
    public ProfileActivityDto getActivity(Long userId) {
        List<Project> mine = projectRepository.findBySubmittedByIdOrderByIdDesc(userId);
        Set<Long> likedIds = Set.copyOf(likeRepository.findProjectIdsByUserId(userId));
        List<Project> liked = likeRepository.findProjectIdsByUserId(userId).isEmpty()
                ? List.of()
                : projectRepository.findAllByIdIn(List.copyOf(likedIds));

        return ProfileActivityDto.builder()
                .pending(mapProjects(mine, project -> project.getStatus() == ProjectStatus.PENDING, null, null))
                .approved(mapProjects(mine, project -> project.getStatus() == ProjectStatus.APPROVED, null, null))
                .rejected(mapProjects(mine, project -> project.getStatus() == ProjectStatus.REJECTED, null, null))
                .liked(liked.stream()
                        .map(project -> projectMapper.toDto(project, likedIds, null))
                        .collect(Collectors.toList()))
                .comments(commentRepository.findByAuthorIdOrderByCreatedAtDesc(userId)
                        .stream().map(commentService::toDto).collect(Collectors.toList()))
                .build();
    }

    private List<ProjectDto> mapProjects(List<Project> projects,
                                         Predicate<Project> filter,
                                         Set<Long> likedIds,
                                         Set<Long> bookmarkedIds) {
        return projects.stream()
                .filter(filter)
                .map(project -> projectMapper.toDto(project, likedIds, bookmarkedIds))
                .collect(Collectors.toList());
    }
}
