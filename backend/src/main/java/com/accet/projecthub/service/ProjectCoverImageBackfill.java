package com.accet.projecthub.service;

import com.accet.projecthub.entity.Project;
import com.accet.projecthub.repository.ProjectRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Component
@Order(2)
public class ProjectCoverImageBackfill implements ApplicationRunner {

    private final ProjectRepository projectRepository;
    private final ProjectCoverImageResolver coverImageResolver;

    public ProjectCoverImageBackfill(ProjectRepository projectRepository,
                                     ProjectCoverImageResolver coverImageResolver) {
        this.projectRepository = projectRepository;
        this.coverImageResolver = coverImageResolver;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (Project project : projectRepository.findAll()) {
            String coverImage = coverImageResolver.resolve(project);
            if (!Objects.equals(project.getImage(), coverImage)) {
                project.setImage(coverImage);
            }
        }
    }
}