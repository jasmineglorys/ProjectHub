package com.accet.projecthub.controller;

import com.accet.projecthub.util.Constants;
import com.accet.projecthub.service.ProjectService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/meta")
public class MetaController {

    private final ProjectService projectService;

    public MetaController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping("/departments")
    public ResponseEntity<List<String>> departments() {
        return ResponseEntity.ok(Constants.DEPARTMENTS);
    }

    @GetMapping("/categories")
    public ResponseEntity<List<String>> categories() {
        return ResponseEntity.ok(Constants.CATEGORIES);
    }

    @GetMapping("/technologies")
    public ResponseEntity<List<String>> technologies() {
        return ResponseEntity.ok(projectService.getAvailableTechnologies());
    }
}
