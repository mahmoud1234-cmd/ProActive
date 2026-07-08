package com.proactive.demo.dto.module;

import com.proactive.demo.model.ProjectModule;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ModuleResponse {

    private Long id;
    private String name;
    private String description;
    private Integer progressPct;       // Valeur manuelle
    private Integer effectiveProgress; // Calculé (prend les enfants en compte)
    private Integer sortOrder;
    private Long parentId;
    private Long projectId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ModuleResponse> children;
    private int childCount;

    public static ModuleResponse from(ProjectModule m) {
        ModuleResponse r = new ModuleResponse();
        r.setId(m.getId());
        r.setName(m.getName());
        r.setDescription(m.getDescription());
        r.setProgressPct(m.getProgressPct());
        r.setEffectiveProgress(m.getEffectiveProgress());
        r.setSortOrder(m.getSortOrder());
        r.setParentId(m.getParent() != null ? m.getParent().getId() : null);
        r.setProjectId(m.getProject().getId());
        r.setCreatedAt(m.getCreatedAt());
        r.setUpdatedAt(m.getUpdatedAt());
        r.setChildCount(m.getChildren() != null ? m.getChildren().size() : 0);

        // Récursif pour les enfants
        if (m.getChildren() != null && !m.getChildren().isEmpty()) {
            r.setChildren(m.getChildren().stream().map(ModuleResponse::from).toList());
        }
        return r;
    }
}
