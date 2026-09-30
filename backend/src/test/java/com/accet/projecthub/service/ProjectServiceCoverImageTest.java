package com.accet.projecthub.service;

import com.accet.projecthub.dto.ProjectDto;
import com.accet.projecthub.dto.ProjectRequest;
import com.accet.projecthub.dto.TeamMemberDto;
import com.accet.projecthub.entity.Project;
import com.accet.projecthub.entity.ProjectStatus;
import com.accet.projecthub.entity.User;
import com.accet.projecthub.repository.BookmarkRepository;
import com.accet.projecthub.repository.ProjectFileRepository;
import com.accet.projecthub.repository.ProjectLikeRepository;
import com.accet.projecthub.repository.ProjectRepository;
import com.accet.projecthub.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectServiceCoverImageTest {

    private static final String COVER_IMAGE = "photo-1518770660439-4636190af475";

    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ProjectLikeRepository likeRepository;
    @Mock
    private BookmarkRepository bookmarkRepository;
    @Mock
    private ProjectFileRepository fileRepository;
    @Mock
    private ProjectMapper mapper;

    private ProjectService projectService;

    @BeforeEach
    void setUp() {
        projectService = new ProjectService(projectRepository, userRepository, likeRepository,
                bookmarkRepository, fileRepository, mapper, new ProjectCoverImageResolver());
    }

    @Test
    void createPersistsAndReturnsTheCoverImageSentByTheFrontend() {
        User owner = new User();
        owner.setId(12L);
        when(userRepository.findById(12L)).thenReturn(Optional.of(owner));
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(likeRepository.findProjectIdsByUserId(12L)).thenReturn(List.of());
        when(bookmarkRepository.findProjectIdsByUserId(12L)).thenReturn(List.of());
        when(mapper.toDto(any(Project.class), anySet(), anySet())).thenAnswer(invocation -> {
            Project saved = invocation.getArgument(0);
            return ProjectDto.builder().image(saved.getImage()).build();
        });

        ProjectRequest request = new ProjectRequest();
        request.setTitle("Earthquake Detection");
        request.setDescription("Detects seismic activity with embedded hardware sensors.");
        request.setDeployLink("https://demo.example");
        request.setDepartment("CSE");
        request.setCategory("Embedded Systems");
        request.setYear(2026);
        request.setAcademicYear("2023-2027");
        request.setTechnologies(List.of("Arduino", "Accelerometer", "Sensor"));
        request.setCoverImage(COVER_IMAGE);
        TeamMemberDto member = new TeamMemberDto();
        member.setName("Student");
        request.setTeamMembers(List.of(member));

        ProjectDto response = projectService.create(request, null, null, 12L);

        ArgumentCaptor<Project> savedProject = ArgumentCaptor.forClass(Project.class);
        verify(projectRepository).save(savedProject.capture());
        assertEquals(COVER_IMAGE, savedProject.getValue().getImage());
        assertEquals(COVER_IMAGE, response.getImage());
        assertEquals(COVER_IMAGE, response.getCoverImage());
    }

    @Test
    void updatePreservesExistingCoverWhenCategoryDoesNotChange() {
        Project project = existingProject("Embedded Systems", COVER_IMAGE);
        prepareUpdate(project);

        projectService.update(project.getId(), request("Embedded Systems", "photo-1677442136019-21780ecad995"),
                12L, false);

        assertEquals(COVER_IMAGE, project.getImage());
    }

    @Test
    void updateStoresSubmittedCoverWhenCategoryChanges() {
        Project project = existingProject("Embedded Systems", COVER_IMAGE);
        prepareUpdate(project);
        String webCover = "photo-1498050108023-c5249f4df085";

        projectService.update(project.getId(), request("Web Development", webCover), 12L, false);

        assertEquals(webCover, project.getImage());
    }

    private void prepareUpdate(Project project) {
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(likeRepository.findProjectIdsByUserId(12L)).thenReturn(List.of());
        when(bookmarkRepository.findProjectIdsByUserId(12L)).thenReturn(List.of());
        when(mapper.toDto(any(Project.class), anySet(), anySet())).thenAnswer(invocation -> {
            Project saved = invocation.getArgument(0);
            return ProjectDto.builder().image(saved.getImage()).build();
        });
    }

    private Project existingProject(String category, String image) {
        User owner = new User();
        owner.setId(12L);
        return Project.builder()
                .id(34L)
                .title("Existing project")
                .description("Existing project description for editing.")
                .department("CSE")
                .category(category)
                .year(2026)
                .academicYear("2023-2027")
                .image(image)
                .status(ProjectStatus.APPROVED)
                .likesCount(0)
                .viewsCount(0)
                .submittedBy(owner)
                .build();
    }

    private ProjectRequest request(String category, String coverImage) {
        ProjectRequest request = new ProjectRequest();
        request.setTitle("Updated project title");
        request.setDescription("Updated project description with enough detail.");
        request.setDeployLink("https://demo.example");
        request.setDepartment("CSE");
        request.setCategory(category);
        request.setYear(2026);
        request.setAcademicYear("2023-2027");
        request.setTechnologies(List.of("React", "Spring Boot"));
        request.setCoverImage(coverImage);
        TeamMemberDto member = new TeamMemberDto();
        member.setName("Student");
        request.setTeamMembers(List.of(member));
        return request;
    }
}
