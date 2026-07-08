package com.proactive.demo.dto.project;

import com.proactive.demo.model.Project;
import com.proactive.demo.model.ProjectMember;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class ProjectResponse {

    private Long id;
    private String name;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private Project.Status status;
    private Project.Priority priority;
    private Project.ProjectType type;
    private Project.Methodology methodology;
    private String tools;
    private List<String> toolsList; // tools parsé en liste
    private Double budget;
    private Integer progressPct;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Manager principal
    private Long managerId;
    private String managerName;
    private String managerEmail;

    // Créateur
    private Long createdById;
    private String createdByName;

    // Membres
    private List<MemberInfo> members;
    private int memberCount;

    @Data
    public static class MemberInfo {
        private Long userId;
        private String fullName;
        private String email;
        private String role;          // Rôle global (ADMIN/MANAGER/USER)
        private String projectRole;   // Rôle dans le projet (MANAGER/MEMBER)
    }

    public static ProjectResponse from(Project p) {
        ProjectResponse r = new ProjectResponse();
        r.setId(p.getId());
        r.setName(p.getName());
        r.setDescription(p.getDescription());
        r.setStartDate(p.getStartDate());
        r.setEndDate(p.getEndDate());
        r.setStatus(p.getStatus());
        r.setPriority(p.getPriority());
        r.setType(p.getType());
        r.setMethodology(p.getMethodology());
        r.setTools(p.getTools());
        r.setBudget(p.getBudget());
        r.setProgressPct(p.getProgressPct());
        r.setCreatedAt(p.getCreatedAt());
        r.setUpdatedAt(p.getUpdatedAt());

        // Parsage outils — tools est une String "Jira,Slack,GitHub"
        if (p.getTools() != null && !p.getTools().isBlank()) {
            r.setToolsList(java.util.Arrays.stream(p.getTools().split(","))
                    .map(String::trim).filter(s -> !s.isEmpty())
                    .collect(java.util.stream.Collectors.toList()));
        }

        if (p.getManager() != null) {
            r.setManagerId(p.getManager().getId());
            r.setManagerName(p.getManager().getFirstName() + " " + p.getManager().getLastName());
            r.setManagerEmail(p.getManager().getEmail());
        }
        if (p.getCreatedBy() != null) {
            r.setCreatedById(p.getCreatedBy().getId());
            r.setCreatedByName(p.getCreatedBy().getFirstName() + " " + p.getCreatedBy().getLastName());
        }

        if (p.getMembers() != null) {
            r.setMembers(p.getMembers().stream().map(m -> {
                MemberInfo mi = new MemberInfo();
                mi.setUserId(m.getUser().getId());
                mi.setFullName(m.getUser().getFirstName() + " " + m.getUser().getLastName());
                mi.setEmail(m.getUser().getEmail());
                mi.setRole(m.getUser().getRole().name());
                mi.setProjectRole(m.getProjectRole().name());
                return mi;
            }).collect(Collectors.toList()));
            r.setMemberCount(p.getMembers().size());
        }

        return r;
    }
}
