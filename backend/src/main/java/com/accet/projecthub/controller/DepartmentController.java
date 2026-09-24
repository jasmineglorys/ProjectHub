package com.accet.projecthub.controller;

import com.accet.projecthub.dto.DepartmentStatDto;
import com.accet.projecthub.service.DepartmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/departments")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @GetMapping("/stats")
    public ResponseEntity<List<DepartmentStatDto>> stats() {
        return ResponseEntity.ok(departmentService.getDepartmentStats());
    }
}
