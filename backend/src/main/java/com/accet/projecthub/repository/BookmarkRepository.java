package com.accet.projecthub.repository;

import com.accet.projecthub.entity.Bookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    Optional<Bookmark> findByUserIdAndProjectId(Long userId, Long projectId);

    @Query("SELECT b.project.id FROM Bookmark b WHERE b.user.id = :userId ORDER BY b.id DESC")
    List<Long> findProjectIdsByUserId(@Param("userId") Long userId);

    long countByUserId(Long userId);

    void deleteByProjectId(Long projectId);
}
