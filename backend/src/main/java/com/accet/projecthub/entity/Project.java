package com.accet.projecthub.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "projects", indexes = {
        @Index(name = "idx_projects_status", columnList = "status"),
        @Index(name = "idx_projects_department", columnList = "department"),
        @Index(name = "idx_projects_category", columnList = "category")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 4000)
    private String description;

    @Column(name = "deploy_link", length = 500)
    private String deployLink;

    @Column(nullable = false, length = 40)
    private String department;

    @Column(nullable = false, length = 40)
    private String category;

    @Column(name = "project_year", nullable = false)
    private Integer year;

    @Column(length = 200)
    private String image;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProjectStatus status;

    @Column(name = "likes_count", nullable = false)
    private Integer likesCount;

    @Column(name = "views_count", nullable = false)
    private Integer viewsCount;

    @Column(name = "submitted_at", nullable = false)
    private LocalDate submittedAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submitted_by", nullable = false,
            foreignKey = @ForeignKey(name = "fk_projects_user"))
    private User submittedBy;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "project_technologies",
            joinColumns = @JoinColumn(name = "project_id",
                    foreignKey = @ForeignKey(name = "fk_tech_project"))
    )
    @Column(name = "technology", nullable = false, length = 60)
    @Builder.Default
    private Set<String> technologies = new LinkedHashSet<>();

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<TeamMember> teamMembers = new ArrayList<>();

    @PrePersist
    void onCreate() {
        if (status == null) status = ProjectStatus.PENDING;
        if (likesCount == null) likesCount = 0;
        if (viewsCount == null) viewsCount = 0;
        if (submittedAt == null) submittedAt = LocalDate.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void addTeamMember(TeamMember member) {
        member.setProject(this);
        this.teamMembers.add(member);
    }

    public void clearTeamMembers() {
        this.teamMembers.clear();
    }
}
