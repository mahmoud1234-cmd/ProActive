package com.proactive.demo.dto.project;

import com.proactive.demo.model.Project;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class ProjectRequest {

    @Size(min = 2, max = 150)
    private String name;

    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private Project.Status status;
    private Project.Priority priority;
    private Project.ProjectType type;
    private Project.Methodology methodology;

    /** Outils séparés par virgule : "Jira,Slack,GitHub" */
    private String tools;

    private Double budget;
    private Integer progressPct;

    /** ID du manager principal */
    private Long managerId;

    /** IDs des managers affectés au projet */
    private List<Long> managerIds;

    /** IDs des membres (users) affectés au projet */
    private List<Long> memberIds;
}
