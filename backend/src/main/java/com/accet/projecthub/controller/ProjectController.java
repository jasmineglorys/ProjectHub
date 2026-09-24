package com.accet.projecthub.controller;

import com.accet.projecthub.dto.MessageResponse;
import com.accet.projecthub.dto.PageResponse;
import com.accet.projecthub.dto.ProjectDto;
import com.accet.projecthub.dto.ProjectRequest;
import com.accet.projecthub.dto.ToggleResponse;
import com.accet.projecthub.security.CustomUserDetails;
import com.accet.projecthub.security.SecurityUtils;
import com.accet.projecthub.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    /** Public: approved projects with search, filters, sorting and paging. */
    @GetMapping
    public ResponseEntity<PageResponse<ProjectDto>> browse(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "popular") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {

        return ResponseEntity.ok(projectService.browse(
                search, department, category, sort, page, size, SecurityUtils.currentUserId()));
    }

    /** Authenticated: the logged-in student's own submissions (any status). */
    @GetMapping("/my")
    public ResponseEntity<List<ProjectDto>> myProjects(
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(projectService.getMyProjects(principal.getId()));
    }

    /** Authenticated: the logged-in student's saved projects. */
    @GetMapping("/bookmarked")
    public ResponseEntity<List<ProjectDto>> bookmarked(
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(projectService.getBookmarkedProjects(principal.getId()));
    }

    /** Public for approved projects; owner/admin can also read pending ones. */
    @GetMapping("/{id}")
    public ResponseEntity<ProjectDto> getOne(@PathVariable Long id) {
        return ResponseEntity.ok(projectService.getByIdAndCountView(
                id, SecurityUtils.currentUserId(), SecurityUtils.isAdmin()));
    }

    @PostMapping
    public ResponseEntity<ProjectDto> create(
            @Valid @RequestBody ProjectRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(projectService.create(request, principal.getId()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProjectDto> update(
            @PathVariable Long id,
            @Valid @RequestBody ProjectRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(projectService.update(
                id, request, principal.getId(), SecurityUtils.isAdmin()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal) {
        projectService.delete(id, principal.getId(), SecurityUtils.isAdmin());
        return ResponseEntity.ok(new MessageResponse("Project deleted successfully"));
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<ToggleResponse> toggleLike(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(projectService.toggleLike(id, principal.getId()));
    }

    @PostMapping("/{id}/bookmark")
    public ResponseEntity<ToggleResponse> toggleBookmark(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(projectService.toggleBookmark(id, principal.getId()));
    }
}
