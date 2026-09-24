package com.accet.projecthub.repository;

import com.accet.projecthub.entity.ProjectLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectLikeRepository extends JpaRepository<ProjectLike, Long> {

    Optional<ProjectLike> findByUserIdAndProjectId(Long userId, Long projectId);

    boolean existsByUserIdAndProjectId(Long userId, Long projectId);

    @Query("SELECT l.project.id FROM ProjectLike l WHERE l.user.id = :userId")
    List<Long> findProjectIdsByUserId(@Param("userId") Long userId);

    void deleteByProjectId(Long projectId);
}
