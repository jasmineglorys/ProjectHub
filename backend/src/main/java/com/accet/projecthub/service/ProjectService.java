package com.accet.projecthub.service;

import com.accet.projecthub.dto.PageResponse;
import com.accet.projecthub.dto.ProjectDto;
import com.accet.projecthub.dto.ProjectRequest;
import com.accet.projecthub.dto.TeamMemberDto;
import com.accet.projecthub.dto.ToggleResponse;
import com.accet.projecthub.entity.Bookmark;
import com.accet.projecthub.entity.Project;
import com.accet.projecthub.entity.ProjectFile;
import com.accet.projecthub.entity.ProjectLike;
import com.accet.projecthub.entity.ProjectStatus;
import com.accet.projecthub.entity.TeamMember;
import com.accet.projecthub.entity.User;
import com.accet.projecthub.exception.BadRequestException;
import com.accet.projecthub.exception.ResourceNotFoundException;
import com.accet.projecthub.exception.UnauthorizedActionException;
import com.accet.projecthub.repository.BookmarkRepository;
import com.accet.projecthub.repository.ProjectLikeRepository;
import com.accet.projecthub.repository.ProjectFileRepository;
import com.accet.projecthub.repository.ProjectRepository;
import com.accet.projecthub.repository.ProjectSpecifications;
import com.accet.projecthub.repository.UserRepository;
import com.accet.projecthub.util.Constants;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.multipart.MultipartFile;
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
    private final ProjectFileRepository fileRepository;
    private final ProjectMapper mapper;

    public ProjectService(ProjectRepository projectRepository,
                          UserRepository userRepository,
                          ProjectLikeRepository likeRepository,
                          BookmarkRepository bookmarkRepository,
                          ProjectFileRepository fileRepository,
                          ProjectMapper mapper) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.likeRepository = likeRepository;
        this.bookmarkRepository = bookmarkRepository;
        this.fileRepository = fileRepository;
        this.mapper = mapper;
    }

    // ---------------------------------------------------------------- read

    @Transactional(readOnly = true)
    public PageResponse<ProjectDto> browse(String search, String department, String category,
                                           String academicYear, Integer year, String technology,
                                           boolean winning, String sort,
                                           int page, int size, Long currentUserId) {

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
                .and(ProjectSpecifications.hasAcademicYear(blankToNull(academicYear)))
                .and(ProjectSpecifications.hasProjectYear(year))
                .and(ProjectSpecifications.hasTechnology(blankToNull(technology)))
                .and(ProjectSpecifications.hasAchievement(winning))
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

    @Transactional(readOnly = true)
    public List<String> getAvailableTechnologies() {
        return projectRepository.findTechnologiesByStatus(ProjectStatus.APPROVED);
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
    public ProjectDto create(ProjectRequest request, List<MultipartFile> supportingFiles,
                             List<MultipartFile> mediaFiles, List<MultipartFile> certificateFiles,
                             MultipartFile coverImage, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        validateTaxonomy(request);

        String image = request.getImage();
        if (image == null || image.isBlank()) {
            image = Constants.resolveProjectImage(request.getCategory());
        }

        Project project = Project.builder()
                .title(request.getTitle().trim())
                .description(request.getDescription().trim())
                .achievement(blankToNull(request.getAchievement()))
                .deployLink(blankToNull(request.getDeployLink()))
                .department(request.getDepartment())
                .category(request.getCategory().trim())
                .year(request.getYear())
                .academicYear(blankToNull(request.getAcademicYear()))
                .image(image)
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
        saveCoverImage(saved, coverImage);
        saveFiles(saved, supportingFiles, "SUPPORTING");
        saveFiles(saved, mediaFiles, "MEDIA");
        if (blankToNull(request.getAchievement()) == null
                && certificateFiles != null && !certificateFiles.isEmpty()) {
            throw new BadRequestException("Enter achievement details before uploading certificates");
        }
        saveFiles(saved, certificateFiles, "CERTIFICATE");
        return mapper.toDto(saved, likedIds(userId), bookmarkedIds(userId));
    }

    @Transactional
    public ProjectFile getFile(Long projectId, Long fileId, Long currentUserId,
                               boolean isAdmin, boolean countDownload) {
        ProjectFile file = fileRepository.findByIdAndProjectId(fileId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found"));
        Project project = file.getProject();
        boolean isOwner = currentUserId != null
                && project.getSubmittedBy().getId().equals(currentUserId);
        if (project.getStatus() != ProjectStatus.APPROVED && !isOwner && !isAdmin) {
            throw new ResourceNotFoundException("File not found");
        }
        file.getData();
        if (countDownload) {
            project.setDownloadsCount(project.getDownloadsCount() + 1);
            projectRepository.save(project);
        }
        return file;
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
        project.setAchievement(blankToNull(request.getAchievement()));
        project.setDeployLink(blankToNull(request.getDeployLink()));
        project.setDepartment(request.getDepartment());
        project.setCategory(request.getCategory().trim());
        project.setYear(request.getYear());
        project.setAcademicYear(blankToNull(request.getAcademicYear()));

        String requestedImage = blankToNull(request.getImage());
        boolean categoryChanged = !request.getCategory().trim().equals(project.getCategory());
        if (requestedImage != null) {
            project.setImage(requestedImage);
        } else if (categoryChanged || project.getImage() == null || project.getImage().isBlank()) {
            project.setImage(Constants.resolveProjectImage(request.getCategory()));
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
        long currentCount = bookmarkRepository.countByProjectId(projectId);

        return bookmarkRepository.findByUserIdAndProjectId(userId, projectId)
                .map(existing -> {
                    bookmarkRepository.delete(existing);
                    return ToggleResponse.builder()
                            .projectId(projectId)
                            .active(false)
                            .likes(project.getLikesCount())
                            .bookmarks(Math.max(0, currentCount - 1))
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
                            .bookmarks(currentCount + 1)
                            .message("Project saved")
                            .build();
                });
    }

    // ------------------------------------------------------------- helpers

    private void validateTaxonomy(ProjectRequest request) {
        if (!Constants.DEPARTMENTS.contains(request.getDepartment())) {
            throw new BadRequestException("Invalid department: " + request.getDepartment());
        }
        List<String> departmentDomains = Constants.DEPARTMENT_DOMAINS.get(request.getDepartment());
        if (!request.isCustomCategory()
                && (departmentDomains == null || !departmentDomains.contains(request.getCategory()))) {
            throw new BadRequestException("Invalid project domain for department "
                    + request.getDepartment() + ": " + request.getCategory());
        }
        if (request.isCustomCategory() && request.getCategory().isBlank()) {
            throw new BadRequestException("Enter a project domain");
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

    private void saveFiles(Project project, List<MultipartFile> files, String fileType) {
        if (files == null) return;
        if (files.size() > 10) {
            throw new BadRequestException("You can upload at most 10 " + fileType.toLowerCase() + " files");
        }
        files.stream().filter(file -> file != null && !file.isEmpty()).forEach(file -> {
            if (file.getSize() > 50 * 1024 * 1024L) {
                throw new BadRequestException("Each uploaded file must be 50 MB or smaller");
            }
            try {
                ProjectFile stored = new ProjectFile();
                stored.setProject(project);
                stored.setFileType(fileType);
                stored.setFileName(file.getOriginalFilename() == null ? "uploaded-file" : file.getOriginalFilename());
                stored.setContentType(file.getContentType());
                stored.setFileSize(file.getSize());
                stored.setData(file.getBytes());
                fileRepository.save(stored);
            } catch (java.io.IOException exception) {
                throw new BadRequestException("Could not read uploaded file");
            }
        });
    }

    private void saveCoverImage(Project project, MultipartFile file) {
        if (file == null || file.isEmpty()) return;
        if (file.getSize() > 5 * 1024 * 1024L) {
            throw new BadRequestException("Cover image must be 5 MB or smaller");
        }
        String contentType = file.getContentType();
        if (!"image/jpeg".equals(contentType) && !"image/png".equals(contentType)) {
            throw new BadRequestException("Cover image must be a JPEG or PNG file");
        }
        try {
            byte[] data = file.getBytes();
            boolean validSignature = "image/png".equals(contentType)
                    ? data.length >= 8 && data[0] == (byte) 0x89 && data[1] == 0x50
                        && data[2] == 0x4e && data[3] == 0x47
                    : data.length >= 3 && data[0] == (byte) 0xff
                        && data[1] == (byte) 0xd8 && data[2] == (byte) 0xff;
            if (!validSignature) {
                throw new BadRequestException("Cover image content does not match its file type");
            }
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(data));
            if (image == null || image.getWidth() != 1200 || image.getHeight() != 630) {
                throw new BadRequestException("Cover image must be exactly 1200 x 630 pixels");
            }

            ProjectFile stored = new ProjectFile();
            stored.setProject(project);
            stored.setFileType("COVER");
            stored.setFileName("project-cover." + ("image/png".equals(contentType) ? "png" : "jpg"));
            stored.setContentType(contentType);
            stored.setFileSize(file.getSize());
            stored.setData(data);
            fileRepository.save(stored);
        } catch (java.io.IOException exception) {
            throw new BadRequestException("Could not read cover image");
        }
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
