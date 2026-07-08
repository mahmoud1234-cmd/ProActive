package com.proactive.demo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "project_modules")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectModule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** Avancement manuel (0-100) — ignoré si le module a des sous-modules */
    @Column(name = "progress_pct")
    private Integer progressPct = 0;

    /** Ordre d'affichage dans la liste */
    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    /** Projet auquel appartient ce module */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    /** Module parent (null = module racine) */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "parent_id")
    private ProjectModule parent;

    /** Sous-modules */
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("sortOrder ASC")
    private List<ProjectModule> children = new ArrayList<>();

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

    /**
     * Calcule l'avancement effectif :
     * - Si le module a des sous-modules → moyenne de leurs avancements effectifs
     * - Sinon → son propre progressPct
     */
    public int getEffectiveProgress() {
        if (children == null || children.isEmpty()) {
            return progressPct != null ? progressPct : 0;
        }
        double avg = children.stream()
                .mapToInt(ProjectModule::getEffectiveProgress)
                .average()
                .orElse(0.0);
        return (int) Math.round(avg);
    }
}
