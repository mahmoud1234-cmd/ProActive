package com.proactive.demo.dto.task;

import com.proactive.demo.model.Task;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class TaskResponse {

    private Long id;
    private String title;
    private String description;
    private Task.Status status;
    private Task.Priority priority;
    private LocalDate dueDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Projet
    private Long projectId;
    private String projectName;

    // Assignée
    private Long assigneeId;
    private String assigneeName;
    private String assigneeEmail;

    // Créateur
    private Long createdById;
    private String createdByName;

    public static TaskResponse from(Task t) {
        TaskResponse r = new TaskResponse();
        r.setId(t.getId());
        r.setTitle(t.getTitle());
        r.setDescription(t.getDescription());
        r.setStatus(t.getStatus());
        r.setPriority(t.getPriority());
        r.setDueDate(t.getDueDate());
        r.setCreatedAt(t.getCreatedAt());
        r.setUpdatedAt(t.getUpdatedAt());

        if (t.getProject() != null) {
            r.setProjectId(t.getProject().getId());
            r.setProjectName(t.getProject().getName());
        }
        if (t.getAssignee() != null) {
            r.setAssigneeId(t.getAssignee().getId());
            r.setAssigneeName(t.getAssignee().getFirstName() + " " + t.getAssignee().getLastName());
            r.setAssigneeEmail(t.getAssignee().getEmail());
        }
        if (t.getCreatedBy() != null) {
            r.setCreatedById(t.getCreatedBy().getId());
            r.setCreatedByName(t.getCreatedBy().getFirstName() + " " + t.getCreatedBy().getLastName());
        }
        return r;
    }
}
