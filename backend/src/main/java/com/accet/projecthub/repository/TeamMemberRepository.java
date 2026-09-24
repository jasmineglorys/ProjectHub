package com.accet.projecthub.repository;

import com.accet.projecthub.entity.TeamMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {

    List<TeamMember> findByProjectId(Long projectId);

    void deleteByProjectId(Long projectId);
}
