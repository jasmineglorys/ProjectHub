package com.accet.projecthub.service;

import com.accet.projecthub.dto.PageResponse;
import com.accet.projecthub.dto.ProjectDto;
import com.accet.projecthub.dto.ProjectRequest;
import com.accet.projecthub.dto.TeamMemberDto;
import com.accet.projecthub.dto.ToggleResponse;
import com.accet.projecthub.entity.Bookmark;
import com.accet.projecthub.entity.Project;
import com.accet.projecthub.entity.ProjectLike;
import com.accet.projecthub.entity.ProjectStatus;
import com.accet.projecthub.entity.TeamMember;
import com.accet.projecthub.entity.User;
import com.accet.projecthub.exception.BadRequestException;
import com.accet.projecthub.exception.ResourceNotFoundException;
import com.accet.projecthub.exception.UnauthorizedActionException;
import com.accet.projecthub.repository.BookmarkRepository;
import com.accet.projecthub.repository.ProjectLikeRepository;
import com.accet.projecthub.repository.ProjectRepository;
import com.accet.projecthub.repository.ProjectSpecifications;
import com.accet.projecthub.repository.UserRepository;
import com.accet.projecthub.util.Constants;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectLikeRepository likeRepository;
    private final BookmarkRepository bookmarkRepository;
    private final ProjectMapper mapper;

    public ProjectService(ProjectRepository projectRepository,
                          UserRepository userRepository,
                          ProjectLikeRepository likeRepository,
                          BookmarkRepository bookmarkRepository,
                          ProjectMapper mapper) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.likeRepository = likeRepository;
        this.bookmarkRepository = bookmarkRepository;
        this.mapper = mapper;
    }

    // ---------------------------------------------------------------- read

    @Transactional(readOnly = true)
    public PageResponse<ProjectDto> browse(String search, String department, String category,
                                           String sort, int page, int size, Long currentUserId) {

        Sort sorting = switch (sort == null ? "popular" : sort) {
            case "newest" -> Sort.by(Sort.Direction.DESC, "year").and(Sort.by(Sort.Direction.DESC, "id"));
            case "views" -> Sort.by(Sort.Direction.DESC, "viewsCount");
            case "oldest" -> Sort.by(Sort.Direction.ASC, "id");
            default -> Sort.by(Sort.Direction.DESC, "likesCount");
        };

        Pageable pageable = PageRequest.of(Math.max(page, 0), size < 1 ? 12 : size, sorting);

        Specification<Project> spec = Specification
                .where(ProjectSpecifications.hasStatus(ProjectStatus.APPROVED))
                .and(ProjectSpecifications.hasDepartment(blankToNull(department)))
                .and(ProjectSpecifications.hasCategory(blankToNull(category)))
                .and(ProjectSpecifications.matchesSearch(blankToNull(search)));

        Page<Project> result = projectRepository.findAll(spec, pageable);

        Set<Long> liked = likedIds(currentUserId);
        Set<Long> saved = bookmarkedIds(currentUserId);

        List<ProjectDto> content = result.getContent().stream()
                .map(p -> mapper.toDto(p, liked, saved))
                .collect(Collectors.toList());

        return PageResponse.<ProjectDto>builder()
                .content(content)
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .last(result.isLast())
                .build();
    }

    @Transactional
    public ProjectDto getByIdAndCountView(Long id, Long currentUserId, boolean isAdmin) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id " + id));

        boolean isOwner = currentUserId != null
                && project.getSubmittedBy().getId().equals(currentUserId);

        if (project.getStatus() != ProjectStatus.APPROVED && !isOwner && !isAdmin) {
            throw new ResourceNotFoundException("Project not found with id " + id);
        }

        // Managed entity: Hibernate flushes this single UPDATE at commit.
        project.setViewsCount(project.getViewsCount() + 1);
        projectRepository.save(project);

        return mapper.toDto(project, likedIds(currentUserId), bookmarkedIds(currentUserId));
    }

    @Transactional(readOnly = true)
    public List<ProjectDto> getMyProjects(Long userId) {
        Set<Long> liked = likedIds(userId);
        Set<Long> saved = bookmarkedIds(userId);
        return projectRepository.findBySubmittedByIdOrderByIdDesc(userId).stream()
                .map(p -> mapper.toDto(p, liked, saved))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProjectDto> getBookmarkedProjects(Long userId) {
        List<Long> ids = bookmarkRepository.findProjectIdsByUserId(userId);
        if (ids.isEmpty()) {
            return new ArrayList<>();
        }
        Set<Long> liked = likedIds(userId);
        Set<Long> saved = new HashSet<>(ids);
        return projectRepository.findAllByIdIn(ids).stream()
                .map(p -> mapper.toDto(p, liked, saved))
                .collect(Collectors.toList());
    }

    // --------------------------------------------------------------- write

    @Transactional
    public ProjectDto create(ProjectRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        validateTaxonomy(request);

        Project project = Project.builder()
                .title(request.getTitle().trim())
                .description(request.getDescription().trim())
                .deployLink(blankToNull(request.getDeployLink()))
                .department(request.getDepartment())
                .category(request.getCategory())
                .year(request.getYear())
                .image(request.getImage() == null || request.getImage().isBlank()
                        ? Constants.DEFAULT_IMAGE : request.getImage())
                .status(ProjectStatus.PENDING)
                .likesCount(0)
                .viewsCount(0)
                // The owner comes from the JWT, never from the request body.
                .submittedBy(user)
                .technologies(cleanTechnologies(request.getTechnologies()))
                .teamMembers(new ArrayList<>())
                .build();

        applyTeamMembers(project, request.getTeamMembers());

        Project saved = projectRepository.save(project);
        return mapper.toDto(saved, likedIds(userId), bookmarkedIds(userId));
    }

    @Transactional
    public ProjectDto update(Long projectId, ProjectRequest request, Long userId, boolean isAdmin) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project not found with id " + projectId));

        if (!isAdmin && !project.getSubmittedBy().getId().equals(userId)) {
            throw new UnauthorizedActionException("You can only edit your own projects");
        }

        validateTaxonomy(request);

        project.setTitle(request.getTitle().trim());
        project.setDescription(request.getDescription().trim());
        project.setDeployLink(blankToNull(request.getDeployLink()));
        project.setDepartment(request.getDepartment());
        project.setCategory(request.getCategory());
        project.setYear(request.getYear());
        if (request.getImage() != null && !request.getImage().isBlank()) {
            project.setImage(request.getImage());
        }
        project.setTechnologies(cleanTechnologies(request.getTechnologies()));

        project.clearTeamMembers();
        applyTeamMembers(project, request.getTeamMembers());

        // An edited project goes back into the moderation queue.
        if (!isAdmin) {
            project.setStatus(ProjectStatus.PENDING);
        }

        Project saved = projectRepository.save(project);
        return mapper.toDto(saved, likedIds(userId), bookmarkedIds(userId));
    }

    @Transactional
    public void delete(Long projectId, Long userId, boolean isAdmin) {
        if (!isAdmin) {
            throw new UnauthorizedActionException("Only an admin can delete projects");
        }

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project not found with id " + projectId));

        if (!isAdmin && !project.getSubmittedBy().getId().equals(userId)) {
            throw new UnauthorizedActionException("You can only delete your own projects");
        }

        likeRepository.deleteByProjectId(projectId);
        bookmarkRepository.deleteByProjectId(projectId);
        projectRepository.delete(project);
    }

    // --------------------------------------------------- likes & bookmarks

    @Transactional
    public ToggleResponse toggleLike(Long projectId, Long userId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project not found with id " + projectId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return likeRepository.findByUserIdAndProjectId(userId, projectId)
                .map(existing -> {
                    likeRepository.delete(existing);
                    int newCount = Math.max(0, project.getLikesCount() - 1);
                    project.setLikesCount(newCount);
                    projectRepository.save(project);
                    return ToggleResponse.builder()
                            .projectId(projectId)
                            .active(false)
                            .likes(newCount)
                            .message("Like removed")
                            .build();
                })
                .orElseGet(() -> {
                    likeRepository.save(ProjectLike.builder()
                            .user(user).project(project).build());
                    int newCount = project.getLikesCount() + 1;
                    project.setLikesCount(newCount);
                    projectRepository.save(project);
                    return ToggleResponse.builder()
                            .projectId(projectId)
                            .active(true)
                            .likes(newCount)
                            .message("Project liked")
                            .build();
                });
    }

    @Transactional
    public ToggleResponse toggleBookmark(Long projectId, Long userId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project not found with id " + projectId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return bookmarkRepository.findByUserIdAndProjectId(userId, projectId)
                .map(existing -> {
                    bookmarkRepository.delete(existing);
                    return ToggleResponse.builder()
                            .projectId(projectId)
                            .active(false)
                            .likes(project.getLikesCount())
                            .message("Bookmark removed")
                            .build();
                })
                .orElseGet(() -> {
                    bookmarkRepository.save(Bookmark.builder()
                            .user(user).project(project).build());
                    return ToggleResponse.builder()
                            .projectId(projectId)
                            .active(true)
                            .likes(project.getLikesCount())
                            .message("Project saved")
                            .build();
                });
    }

    // ------------------------------------------------------------- helpers

    private void validateTaxonomy(ProjectRequest request) {
        if (!Constants.DEPARTMENTS.contains(request.getDepartment())) {
            throw new BadRequestException("Invalid department: " + request.getDepartment());
        }
        if (!Constants.CATEGORIES.contains(request.getCategory())) {
            throw new BadRequestException("Invalid category: " + request.getCategory());
        }
        if (("CSE".equals(request.getDepartment()) || "IT".equals(request.getDepartment()))
                && (request.getDeployLink() == null || request.getDeployLink().isBlank())) {
            throw new BadRequestException("Deploy link is required for CSE and IT projects");
        }
    }

    private Set<String> cleanTechnologies(List<String> raw) {
        Set<String> cleaned = raw.stream()
                .filter(t -> t != null && !t.isBlank())
                .map(String::trim)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (cleaned.isEmpty()) {
            throw new BadRequestException("At least one technology is required");
        }
        return cleaned;
    }

    private void applyTeamMembers(Project project, List<TeamMemberDto> members) {
        List<TeamMemberDto> valid = members.stream()
                .filter(m -> m.getName() != null && !m.getName().isBlank())
                .toList();

        if (valid.isEmpty()) {
            throw new BadRequestException("At least one team member with a name is required");
        }

        valid.forEach(m -> project.addTeamMember(TeamMember.builder()
                .name(m.getName().trim())
                .rollNo(m.getRollNo() == null ? null : m.getRollNo().trim().toUpperCase())
                .build()));
    }

    private Set<Long> likedIds(Long userId) {
        if (userId == null) return new HashSet<>();
        return new HashSet<>(likeRepository.findProjectIdsByUserId(userId));
    }

    private Set<Long> bookmarkedIds(Long userId) {
        if (userId == null) return new HashSet<>();
        return new HashSet<>(bookmarkRepository.findProjectIdsByUserId(userId));
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
