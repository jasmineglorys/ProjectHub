package com.accet.projecthub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {
    private Long id;
    private String name;
    private String email;
    private String rollNo;
    private String department;
    private Integer year;
    private String avatar;
    private String role;
    private List<Long> likedProjects;
    private List<Long> bookmarks;
}
