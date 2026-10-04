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
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String technology,
            @RequestParam(defaultValue = "false") boolean winning,
            @RequestParam(defaultValue = "popular") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {

        return ResponseEntity.ok(projectService.browse(
            search, department, category, academicYear, year, technology, winning, sort, page, size,
            SecurityUtils.currentUserId()));
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
            @Valid @RequestPart("project") ProjectRequest request,
            @RequestPart(value = "supportingFiles", required = false) List<MultipartFile> supportingFiles,
            @RequestPart(value = "mediaFiles", required = false) List<MultipartFile> mediaFiles,
            @RequestPart(value = "certificateFiles", required = false) List<MultipartFile> certificateFiles,
            @RequestPart(value = "coverImage", required = false) MultipartFile coverImage,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(projectService.create(request, supportingFiles, mediaFiles,
                    certificateFiles, coverImage, principal.getId()));
    }

    @PostMapping("/admin/import")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProjectDto> importProject(@Valid @RequestBody ProjectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(projectService.importProject(request));
    }

    @GetMapping("/{projectId}/files/{fileId}")
    public ResponseEntity<ByteArrayResource> downloadFile(
            @PathVariable Long projectId,
            @PathVariable Long fileId,
            @RequestParam(defaultValue = "false") boolean download) {
        var file = projectService.getFile(
            projectId, fileId, SecurityUtils.currentUserId(), SecurityUtils.isAdmin(), download);
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (file.getContentType() != null) {
            try {
                mediaType = MediaType.parseMediaType(file.getContentType());
            } catch (IllegalArgumentException ignored) {
                // Fall back to a generic download for unknown content types.
            }
        }
        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(file.getData().length)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + file.getFileName().replace("\"", "") + "\"")
                .body(new ByteArrayResource(file.getData()));
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
