package com.accet.projecthub.service;

import com.accet.projecthub.dto.AdminStatsDto;
import com.accet.projecthub.dto.ProjectDto;
import com.accet.projecthub.entity.Project;
import com.accet.projecthub.entity.ProjectStatus;
import com.accet.projecthub.exception.BadRequestException;
import com.accet.projecthub.exception.ResourceNotFoundException;
import com.accet.projecthub.repository.ProjectRepository;
import com.accet.projecthub.repository.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    @Transactional
    public ProjectDto updateStatus(Long projectId, String status) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project not found with id " + projectId));

        ProjectStatus nextStatus = parseStatus(status);
        project.setStatus(nextStatus);
        Project saved = projectRepository.save(project);
        notificationService.notifyProjectOwner(
            saved,
            "STATUS",
            "Your project '" + saved.getTitle() + "' was "
                + nextStatus.name().toLowerCase() + ".",
            null);
        return mapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public AdminStatsDto getStats() {
        long total = projectRepository.count();
        long pending = projectRepository.countByStatus(ProjectStatus.PENDING);
        long approved = projectRepository.countByStatus(ProjectStatus.APPROVED);
        long rejected = projectRepository.countByStatus(ProjectStatus.REJECTED);

        Map<String, Long> byDept = new LinkedHashMap<>();
        for (Object[] row : projectRepository.countGroupedByDepartment(ProjectStatus.APPROVED)) {
            byDept.put((String) row[0], ((Number) row[1]).longValue());
        }

        Map<String, Long> byCategory = new LinkedHashMap<>();
        for (Object[] row : projectRepository.countGroupedByCategory(ProjectStatus.APPROVED)) {
            byCategory.put((String) row[0], ((Number) row[1]).longValue());
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
                .totalStudents(userRepository.count())
                .byDepartment(byDept)
                .byCategory(byCategory)
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
}
