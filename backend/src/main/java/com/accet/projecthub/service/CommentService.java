package com.accet.projecthub.service;

import com.accet.projecthub.dto.CommentDto;
import com.accet.projecthub.dto.CommentRequest;
import com.accet.projecthub.entity.Project;
import com.accet.projecthub.entity.ProjectComment;
import com.accet.projecthub.entity.ProjectStatus;
import com.accet.projecthub.entity.User;
import com.accet.projecthub.exception.ResourceNotFoundException;
import com.accet.projecthub.repository.ProjectCommentRepository;
import com.accet.projecthub.repository.ProjectRepository;
import com.accet.projecthub.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CommentService {

    private final ProjectCommentRepository commentRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public CommentService(ProjectCommentRepository commentRepository,
                          ProjectRepository projectRepository,
                          UserRepository userRepository,
                          NotificationService notificationService) {
        this.commentRepository = commentRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<CommentDto> getForProject(Long projectId) {
        Project project = findApprovedProject(projectId);
        return commentRepository.findByProjectIdOrderByCreatedAtAsc(project.getId())
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public CommentDto add(Long projectId, CommentRequest request, Long userId) {
        Project project = findApprovedProject(projectId);
        User author = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        ProjectComment comment = ProjectComment.builder()
                .content(request.getContent().trim())
                .project(project)
                .author(author)
                .build();
        ProjectComment saved = commentRepository.save(comment);
        notificationService.notifyProjectOwner(
                project,
                "COMMENT",
                author.getName() + " commented on your project",
                userId);
        return toDto(saved);
    }

    private Project findApprovedProject(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        if (project.getStatus() != ProjectStatus.APPROVED) {
            throw new ResourceNotFoundException("Project not found");
        }
        return project;
    }

    public CommentDto toDto(ProjectComment comment) {
        return CommentDto.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .authorId(comment.getAuthor().getId())
                .authorName(comment.getAuthor().getName())
                .projectId(comment.getProject().getId())
                .projectTitle(comment.getProject().getTitle())
                .createdAt(comment.getCreatedAt().toString())
                .build();
    }
}
