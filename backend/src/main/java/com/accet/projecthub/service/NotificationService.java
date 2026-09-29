package com.accet.projecthub.service;

import com.accet.projecthub.dto.NotificationDto;
import com.accet.projecthub.entity.Notification;
import com.accet.projecthub.entity.Project;
import com.accet.projecthub.entity.User;
import com.accet.projecthub.exception.ResourceNotFoundException;
import com.accet.projecthub.repository.NotificationRepository;
import com.accet.projecthub.repository.ProjectCommentRepository;
import com.accet.projecthub.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final ProjectCommentRepository commentRepository;

    public NotificationService(NotificationRepository notificationRepository,
                               UserRepository userRepository,
                               ProjectCommentRepository commentRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.commentRepository = commentRepository;
    }

    @Transactional
    public void notifyProjectOwner(Project project, String type, String message, Long actorId) {
        if (project.getSubmittedBy().getId().equals(actorId)) return;
        notificationRepository.save(Notification.builder()
                .type(type)
                .message(message)
                .recipient(project.getSubmittedBy())
                .project(project)
                .build());
    }

    @Transactional
    public void notifyNewProject(Project project) {
        Long ownerId = project.getSubmittedBy().getId();
        List<Notification> notifications = userRepository.findAll().stream()
                .filter(user -> !user.getId().equals(ownerId))
                .map(user -> Notification.builder()
                        .type("NEW_PROJECT")
                        .message("A new project has been published.")
                        .recipient(user)
                        .project(project)
                        .build())
                .collect(Collectors.toList());
        if (!notifications.isEmpty()) {
            notificationRepository.saveAll(notifications);
        }
    }

    @Transactional
    public void notifyProjectComment(Project project, String authorName, Long authorId) {
        Long ownerId = project.getSubmittedBy().getId();
        Map<Long, User> recipients = new LinkedHashMap<>();
        if (!ownerId.equals(authorId)) {
            recipients.put(ownerId, project.getSubmittedBy());
        }

        commentRepository.findByProjectIdOrderByCreatedAtAsc(project.getId()).stream()
                .map(comment -> comment.getAuthor())
                .filter(user -> !user.getId().equals(authorId))
                .forEach(user -> recipients.putIfAbsent(user.getId(), user));

        List<Notification> notifications = recipients.values().stream()
                .map(recipient -> Notification.builder()
                        .type("COMMENT")
                        .message(recipient.getId().equals(ownerId)
                                ? authorName + " commented on your project '" + project.getTitle() + "'."
                                : authorName + " commented on '" + project.getTitle()
                                        + "', a project you commented on.")
                        .recipient(recipient)
                        .project(project)
                        .build())
                .collect(Collectors.toList());
        if (!notifications.isEmpty()) {
            notificationRepository.saveAll(notifications);
        }
    }

    @Transactional(readOnly = true)
    public List<NotificationDto> getForUser(Long userId) {
        return notificationRepository.findTop50ByRecipientIdOrderByCreatedAtDesc(userId)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public void markRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        if (!notification.getRecipient().getId().equals(userId)) {
            throw new ResourceNotFoundException("Notification not found");
        }
        notification.setReadAt(java.time.LocalDateTime.now());
    }

    private NotificationDto toDto(Notification notification) {
        Project project = notification.getProject();
        return NotificationDto.builder()
                .id(notification.getId())
                .type(notification.getType())
                .message(notification.getMessage())
                .projectId(project == null ? null : project.getId())
                .projectTitle(project == null ? null : project.getTitle())
                .projectDescription(project == null ? null : project.getDescription())
                .projectDepartment(project == null ? null : project.getDepartment())
                .projectCategory(project == null ? null : project.getCategory())
                .read(notification.getReadAt() != null)
                .createdAt(notification.getCreatedAt().toString())
                .build();
    }
}
