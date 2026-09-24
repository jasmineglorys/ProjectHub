package com.accet.projecthub.repository;

import com.accet.projecthub.entity.Project;
import com.accet.projecthub.entity.ProjectStatus;
import com.accet.projecthub.entity.TeamMember;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Dynamic, type-safe filters for the browse screen. Using the Criteria API avoids
 * the "(:param IS NULL OR ...)" JPQL pattern, which MySQL rejects for untyped nulls.
 */
public final class ProjectSpecifications {

    private ProjectSpecifications() {
    }

    public static Specification<Project> hasStatus(ProjectStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Project> hasDepartment(String department) {
        return (root, query, cb) ->
                department == null ? null : cb.equal(root.get("department"), department);
    }

    public static Specification<Project> hasCategory(String category) {
        return (root, query, cb) ->
                category == null ? null : cb.equal(root.get("category"), category);
    }

    /** Matches title, description, any technology, or any team member name. */
    public static Specification<Project> matchesSearch(String search) {
        return (root, query, cb) -> {
            if (search == null) {
                return null;
            }
            // Only join for the data query; the count query must stay flat.
            assert query != null;
            String pattern = "%" + search.toLowerCase() + "%";

            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.like(cb.lower(root.get("title")), pattern));
            predicates.add(cb.like(cb.lower(root.get("description")), pattern));

            Join<Project, String> techJoin = root.join("technologies", JoinType.LEFT);
            predicates.add(cb.like(cb.lower(techJoin), pattern));

            Join<Project, TeamMember> memberJoin = root.join("teamMembers", JoinType.LEFT);
            predicates.add(cb.like(cb.lower(memberJoin.get("name")), pattern));

            query.distinct(true);
            return cb.or(predicates.toArray(new Predicate[0]));
        };
    }
}
