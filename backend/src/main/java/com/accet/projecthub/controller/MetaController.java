package com.accet.projecthub.controller;

import com.accet.projecthub.util.Constants;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/meta")
public class MetaController {

    @GetMapping("/departments")
    public ResponseEntity<List<String>> departments() {
        return ResponseEntity.ok(Constants.DEPARTMENTS);
    }

    @GetMapping("/categories")
    public ResponseEntity<List<String>> categories() {
        return ResponseEntity.ok(Constants.CATEGORIES);
    }
}
