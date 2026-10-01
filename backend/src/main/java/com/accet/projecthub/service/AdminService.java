package com.accet.projecthub.service;

import com.accet.projecthub.dto.AdminStatsDto;
import com.accet.projecthub.dto.ProjectDto;
import com.accet.projecthub.dto.SimilarProjectDto;
import com.accet.projecthub.entity.Project;
import com.accet.projecthub.entity.ProjectStatus;
import com.accet.projecthub.entity.User;
import com.accet.projecthub.exception.BadRequestException;
import com.accet.projecthub.exception.ResourceNotFoundException;
import com.accet.projecthub.repository.ProjectRepository;
import com.accet.projecthub.repository.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectMapper mapper;
    private final NotificationService notificationService;

    public AdminService(ProjectRepository projectRepository,
                        UserRepository userRepository,
                        ProjectMapper mapper,
                        NotificationService notificationService) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.mapper = mapper;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<ProjectDto> listByStatus(String status) {
        List<Project> projects;
        if (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)) {
            projects = projectRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
        } else {
            projects = projectRepository.findByStatusOrderByIdDesc(parseStatus(status));
        }
        return projects.stream().map(mapper::toDto).collect(Collectors.toList());
    }

        @Transactional(readOnly = true)
        public List<SimilarProjectDto> findSimilarProjects(Long projectId) {
        Project source = projectRepository.findById(projectId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Project not found with id " + projectId));

        return projectRepository.findApprovedWithTechnologiesExcept(projectId, ProjectStatus.APPROVED).stream()
            .map(candidate -> SimilarProjectDto.builder()
                .id(candidate.getId())
                .title(candidate.getTitle())
                .department(candidate.getDepartment())
                .year(candidate.getYear())
                .status(candidate.getStatus().name())
                .matchingFields(ProjectSimilarityMatcher.matchingFields(source, candidate))
                .build())
            .filter(candidate -> !candidate.getMatchingFields().isEmpty())
            .sorted(Comparator.comparingInt(
                (SimilarProjectDto candidate) -> candidate.getMatchingFields().size())
                .reversed())
            .limit(5)
            .collect(Collectors.toList());
        }

    @Transactional
    public ProjectDto updateStatus(Long projectId, String status) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project not found with id " + projectId));

        ProjectStatus previousStatus = project.getStatus();
        ProjectStatus nextStatus = parseStatus(status);
        if (nextStatus == ProjectStatus.CHANGES_REQUESTED || nextStatus == ProjectStatus.REJECTED) {
            throw new BadRequestException("Use the dedicated action and provide a reason.");
        }
        project.setStatus(nextStatus);
        Project saved = projectRepository.save(project);
        notificationService.notifyProjectOwner(
            saved,
            "STATUS",
            "Your project '" + saved.getTitle() + "' was "
                + nextStatus.name().toLowerCase() + ".",
            null);
        if (previousStatus != ProjectStatus.APPROVED && nextStatus == ProjectStatus.APPROVED) {
            notificationService.notifyNewProject(saved);
        }
        return mapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public AdminStatsDto getStats() {
        long total = projectRepository.count();
        long pending = projectRepository.countByStatus(ProjectStatus.PENDING);
        long approved = projectRepository.countByStatus(ProjectStatus.APPROVED);
        long rejected = projectRepository.countByStatus(ProjectStatus.REJECTED);
        long changesRequested = projectRepository.countByStatus(ProjectStatus.CHANGES_REQUESTED);

        Map<String, Long> byDept = new LinkedHashMap<>();
        for (Object[] row : projectRepository.countGroupedByDepartment(ProjectStatus.APPROVED)) {
            byDept.put((String) row[0], ((Number) row[1]).longValue());
        }

        Map<String, Long> byCategory = new LinkedHashMap<>();
        for (Object[] row : projectRepository.countGroupedByCategory(ProjectStatus.APPROVED)) {
            byCategory.put((String) row[0], ((Number) row[1]).longValue());
        }

        Map<String, Map<String, Long>> departmentInsights = new LinkedHashMap<>();
        for (Object[] row : projectRepository.countGroupedByDepartmentAndCategory()) {
            String department = (String) row[0];
            String category = (String) row[1];
            long count = ((Number) row[2]).longValue();
            departmentInsights.computeIfAbsent(department, key -> new LinkedHashMap<>())
                    .put(category, count);
        }

        List<ProjectDto> recent = projectRepository
                .findAll(Sort.by(Sort.Direction.DESC, "id"))
                .stream()
                .limit(6)
                .map(mapper::toDto)
                .collect(Collectors.toList());

        return AdminStatsDto.builder()
                .totalProjects(total)
                .pending(pending)
                .approved(approved)
                .rejected(rejected)
                .changesRequested(changesRequested)
                .totalStudents(userRepository.count())
                .byDepartment(byDept)
                .byCategory(byCategory)
                .departmentInsights(departmentInsights)
                .recentActivity(recent)
                .build();
    }

    private ProjectStatus parseStatus(String status) {
        try {
            return ProjectStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException(
                    "Invalid status '" + status + "'. Use PENDING, APPROVED or REJECTED.");
        }
    }

    @Transactional
    public ProjectDto requestChanges(Long projectId, String reason) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project not found with id " + projectId));
        if (project.getStatus() != ProjectStatus.PENDING) {
            throw new BadRequestException("Changes can only be requested for pending projects.");
        }

        String trimmedReason = reason == null ? "" : reason.trim();
        if (trimmedReason.isEmpty()) {
            throw new BadRequestException("A reason is required when requesting changes.");
        }

        project.setStatus(ProjectStatus.CHANGES_REQUESTED);
        Project saved = projectRepository.save(project);
        notificationService.notifyProjectOwner(
                saved, "CHANGES_REQUESTED", trimmedReason, null);
        return mapper.toDto(saved);
    }

    @Transactional
    public ProjectDto rejectProject(Long projectId, Long adminId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project not found with id " + projectId));
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin user not found"));
        project.setStatus(ProjectStatus.REJECTED);
        project.setRejectedBy(admin);
        project.setRejectedAt(LocalDateTime.now());
        Project saved = projectRepository.save(project);
        notificationService.notifyProjectOwner(
                saved, "STATUS", "Your project was rejected.", null);
        return mapper.toDto(saved);
    }
}
