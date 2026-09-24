package com.accet.projecthub.controller;

import com.accet.projecthub.dto.ProfileActivityDto;
import com.accet.projecthub.security.CustomUserDetails;
import com.accet.projecthub.service.ProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/activity")
    public ResponseEntity<ProfileActivityDto> activity(
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(profileService.getActivity(principal.getId()));
    }
}
