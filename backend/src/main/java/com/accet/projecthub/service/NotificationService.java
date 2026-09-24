package com.accet.projecthub.service;

import com.accet.projecthub.dto.NotificationDto;
import com.accet.projecthub.entity.Notification;
import com.accet.projecthub.entity.Project;
import com.accet.projecthub.entity.User;
import com.accet.projecthub.exception.ResourceNotFoundException;
import com.accet.projecthub.repository.NotificationRepository;
import com.accet.projecthub.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository,
                               UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
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
                .read(notification.getReadAt() != null)
                .createdAt(notification.getCreatedAt().toString())
                .build();
    }
}
