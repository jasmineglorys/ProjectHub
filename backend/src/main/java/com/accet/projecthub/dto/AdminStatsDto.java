package com.accet.projecthub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminStatsDto {
    private long totalProjects;
    private long pending;
    private long approved;
    private long rejected;
    private long totalStudents;
    private Map<String, Long> byDepartment;
    private Map<String, Long> byCategory;
    private List<ProjectDto> recentActivity;
}
