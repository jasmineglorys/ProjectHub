package com.accet.projecthub.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class SimilarProjectDto {
    private Long id;
    private String title;
    private String department;
    private Integer year;
    private String status;
    private List<String> matchingFields;
}