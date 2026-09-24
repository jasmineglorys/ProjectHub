package com.accet.projecthub.repository;

import com.accet.projecthub.entity.Project;
import com.accet.projecthub.entity.ProjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long>,
        JpaSpecificationExecutor<Project> {

    List<Project> findBySubmittedByIdOrderByIdDesc(Long userId);

    List<Project> findByStatusOrderByIdDesc(ProjectStatus status);

    long countByStatus(ProjectStatus status);

    @Query("SELECT p.department, COUNT(p) FROM Project p WHERE p.status = :status GROUP BY p.department")
    List<Object[]> countGroupedByDepartment(@Param("status") ProjectStatus status);

    @Query("SELECT p.category, COUNT(p) FROM Project p WHERE p.status = :status GROUP BY p.category")
    List<Object[]> countGroupedByCategory(@Param("status") ProjectStatus status);

    @Query("SELECT COALESCE(SUM(p.likesCount), 0) FROM Project p WHERE p.submittedBy.id = :userId")
    long sumLikesByUser(@Param("userId") Long userId);

    @Query("SELECT p FROM Project p WHERE p.id IN :ids ORDER BY p.id DESC")
    List<Project> findAllByIdIn(@Param("ids") List<Long> ids);
}
