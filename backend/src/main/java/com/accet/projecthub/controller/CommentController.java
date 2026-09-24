package com.accet.projecthub.controller;

import com.accet.projecthub.dto.CommentDto;
import com.accet.projecthub.dto.CommentRequest;
import com.accet.projecthub.security.CustomUserDetails;
import com.accet.projecthub.service.CommentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping
    public ResponseEntity<List<CommentDto>> list(@PathVariable Long projectId) {
        return ResponseEntity.ok(commentService.getForProject(projectId));
    }

    @PostMapping
    public ResponseEntity<CommentDto> add(@PathVariable Long projectId,
                                          @Valid @RequestBody CommentRequest request,
                                          @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(commentService.add(projectId, request, principal.getId()));
    }
}
