package com.accet.projecthub.repository;

import com.accet.projecthub.entity.ProjectFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectFileRepository extends JpaRepository<ProjectFile, Long> {
    List<ProjectFile> findByProjectIdOrderByIdAsc(Long projectId);
    Optional<ProjectFile> findByIdAndProjectId(Long id, Long projectId);
}