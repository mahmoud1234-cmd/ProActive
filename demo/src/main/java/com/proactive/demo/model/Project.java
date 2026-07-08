package com.proactive.demo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "projects")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.PLANNING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Priority priority = Priority.MEDIUM;

    /** Type de projet */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ProjectType type = ProjectType.OTHER;

    /** Méthodologie de travail */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Methodology methodology = Methodology.AGILE;

    /** Outils utilisés (liste séparée par virgules : "Jira,Slack,GitHub") */
    @Column(columnDefinition = "TEXT")
    private String tools;

    /** Budget estimé */
    private Double budget;

    /** Pourcentage d'avancement (0-100) */
    @Column(name = "progress_pct")
    private Integer progressPct = 0;

    /** Manager principal du projet */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "manager_id")
    private User manager;

    /** Créateur du projet */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "created_by")
    private User createdBy;

    /** Membres du projet */
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<ProjectMember> members = new ArrayList<>();

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ── Enums ────────────────────────────────────────────────────

    public enum Status {
        PLANNING, ACTIVE, ON_HOLD, COMPLETED, CANCELLED
    }

    public enum Priority {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    public enum ProjectType {
        SOFTWARE,       // Développement logiciel
        INFRASTRUCTURE, // Infrastructure / DevOps
        MARKETING,      // Campagne marketing
        DESIGN,         // Design / UX
        RESEARCH,       // Recherche & Innovation
        TRAINING,       // Formation / Education
        CONSULTING,     // Consulting
        OTHER           // Autre
    }

    public enum Methodology {
        AGILE,     // Agile / Scrum
        KANBAN,    // Kanban
        WATERFALL, // Cycle en V
        LEAN,      // Lean
        DEVOPS,    // DevOps
        PRINCE2,   // PRINCE2
        PMI,       // PMI / PMBOK
        HYBRID,    // Hybride
        OTHER      // Autre
    }
}
