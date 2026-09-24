package com.accet.projecthub.controller;

import com.accet.projecthub.dto.NotificationDto;
import com.accet.projecthub.security.CustomUserDetails;
import com.accet.projecthub.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<List<NotificationDto>> list(
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(notificationService.getForUser(principal.getId()));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markRead(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal) {
        notificationService.markRead(id, principal.getId());
        return ResponseEntity.noContent().build();
    }
}
