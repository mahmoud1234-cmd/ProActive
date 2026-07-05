package com.proactive.demo.dto.project;

import com.proactive.demo.model.Project;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ProjectRequest {

    // Optionnel pour les mises à jour partielles (ex: drag & drop change juste le statut)
    @Size(min = 2, max = 150)
    private String name;

    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private Project.Status status;
    private Project.Priority priority;
    private Double budget;
    private Integer progressPct;
    private Long managerId;
}
