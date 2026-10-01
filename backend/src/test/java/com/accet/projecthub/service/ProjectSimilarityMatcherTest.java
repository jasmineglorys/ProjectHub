package com.accet.projecthub.service;

import com.accet.projecthub.entity.Project;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectSimilarityMatcherTest {

    @Test
    void flagsSharedTitleDescriptionAndTechnologies() {
        Project first = project(
                "Smart Attendance System",
                "A campus attendance platform using face recognition to record student presence and provide reports to faculty.",
                Set.of("React", "Spring Boot", "MySQL"));
        Project second = project(
                "Smart Attendance Management System",
                "A campus attendance platform using face recognition to record student presence and provide reports to faculty.",
                Set.of("React", "Spring Boot", "MySQL"));

        assertThat(ProjectSimilarityMatcher.matchingFields(first, second))
                .contains("Title", "Description / abstract", "Technologies");
    }

    @Test
    void doesNotFlagOnlyOneCommonTechnology() {
        Project first = project("Library Inventory Portal", "A searchable library catalogue for students.",
                Set.of("React"));
        Project second = project("Solar Panel Monitor", "A dashboard that tracks panel energy output.",
                Set.of("React"));

        assertThat(ProjectSimilarityMatcher.matchingFields(first, second)).isEmpty();
    }

    private Project project(String title, String description, Set<String> technologies) {
        return Project.builder()
                .title(title)
                .description(description)
                .technologies(technologies)
                .build();
    }
}