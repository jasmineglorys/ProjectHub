package com.accet.projecthub.service;

import com.accet.projecthub.dto.DepartmentStatDto;
import com.accet.projecthub.entity.ProjectStatus;
import com.accet.projecthub.repository.ProjectRepository;
import com.accet.projecthub.util.Constants;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DepartmentService {

    private final ProjectRepository projectRepository;

    public DepartmentService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    @Transactional(readOnly = true)
    public List<DepartmentStatDto> getDepartmentStats() {
        Map<String, Long> counts = new HashMap<>();
        for (Object[] row : projectRepository.countGroupedByDepartment(ProjectStatus.APPROVED)) {
            counts.put((String) row[0], ((Number) row[1]).longValue());
        }

        List<DepartmentStatDto> stats = new ArrayList<>();
        for (String dept : Constants.DEPARTMENTS) {
            stats.add(DepartmentStatDto.builder()
                    .department(dept)
                    .projectCount(counts.getOrDefault(dept, 0L))
                    .build());
        }
        return stats;
    }
}
