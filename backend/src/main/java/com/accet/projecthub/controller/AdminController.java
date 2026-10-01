package com.accet.projecthub.controller;

import com.accet.projecthub.dto.AdminStatsDto;
import com.accet.projecthub.dto.MessageResponse;
import com.accet.projecthub.dto.ProjectDto;
import com.accet.projecthub.dto.RequestChangesRequest;
import com.accet.projecthub.dto.SimilarProjectDto;
import com.accet.projecthub.dto.StatusUpdateRequest;
import com.accet.projecthub.security.CustomUserDetails;
import com.accet.projecthub.service.AdminService;
import com.accet.projecthub.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final ProjectService projectService;

    public AdminController(AdminService adminService, ProjectService projectService) {
        this.adminService = adminService;
        this.projectService = projectService;
    }

    /** status = PENDING | APPROVED | REJECTED | CHANGES_REQUESTED | ALL */
    @GetMapping("/projects")
    public ResponseEntity<List<ProjectDto>> listProjects(
            @RequestParam(defaultValue = "ALL") String status) {
        return ResponseEntity.ok(adminService.listByStatus(status));
    }

    @PatchMapping("/projects/{id}/status")
    public ResponseEntity<ProjectDto> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusUpdateRequest request) {
        return ResponseEntity.ok(adminService.updateStatus(id, request.getStatus()));
    }

    @PatchMapping("/projects/{id}/reject")
    public ResponseEntity<ProjectDto> rejectProject(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(adminService.rejectProject(
            id, principal.getId()));
    }

    @GetMapping("/projects/{id}/similar")
    public ResponseEntity<List<SimilarProjectDto>> similarProjects(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.findSimilarProjects(id));
    }

    @PatchMapping("/projects/{id}/request-changes")
    public ResponseEntity<ProjectDto> requestChanges(
            @PathVariable Long id,
            @Valid @RequestBody RequestChangesRequest request) {
        return ResponseEntity.ok(adminService.requestChanges(id, request.getReason()));
    }

    @DeleteMapping("/projects/{id}")
    public ResponseEntity<MessageResponse> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal) {
        projectService.delete(id, principal.getId(), true);
        return ResponseEntity.ok(new MessageResponse("Project deleted successfully"));
    }

    @GetMapping("/stats")
    public ResponseEntity<AdminStatsDto> stats() {
        return ResponseEntity.ok(adminService.getStats());
    }
}
