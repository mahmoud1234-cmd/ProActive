package com.proactive.demo.dto.project;

import com.proactive.demo.model.Project;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ProjectResponse {

    private Long id;
    private String name;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private Project.Status status;
    private Project.Priority priority;
    private Double budget;
    private Integer progressPct;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Manager info
    private Long managerId;
    private String managerName;
    private String managerEmail;

    // Créateur
    private Long createdById;
    private String createdByName;

    public static ProjectResponse from(Project p) {
        ProjectResponse r = new ProjectResponse();
        r.setId(p.getId());
        r.setName(p.getName());
        r.setDescription(p.getDescription());
        r.setStartDate(p.getStartDate());
        r.setEndDate(p.getEndDate());
        r.setStatus(p.getStatus());
        r.setPriority(p.getPriority());
        r.setBudget(p.getBudget());
        r.setProgressPct(p.getProgressPct());
        r.setCreatedAt(p.getCreatedAt());
        r.setUpdatedAt(p.getUpdatedAt());

        if (p.getManager() != null) {
            r.setManagerId(p.getManager().getId());
            r.setManagerName(p.getManager().getFirstName() + " " + p.getManager().getLastName());
            r.setManagerEmail(p.getManager().getEmail());
        }
        if (p.getCreatedBy() != null) {
            r.setCreatedById(p.getCreatedBy().getId());
            r.setCreatedByName(p.getCreatedBy().getFirstName() + " " + p.getCreatedBy().getLastName());
        }
        return r;
    }
}
