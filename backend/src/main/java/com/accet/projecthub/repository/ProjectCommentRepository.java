package com.accet.projecthub.repository;

import com.accet.projecthub.entity.ProjectComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectCommentRepository extends JpaRepository<ProjectComment, Long> {
    List<ProjectComment> findByProjectIdOrderByCreatedAtAsc(Long projectId);
    List<ProjectComment> findByAuthorIdOrderByCreatedAtDesc(Long authorId);
    long countByProjectId(Long projectId);
}
