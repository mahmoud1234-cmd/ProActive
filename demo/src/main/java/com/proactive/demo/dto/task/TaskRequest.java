package com.proactive.demo.dto.task;

import com.proactive.demo.model.Task;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class TaskRequest {

    @Size(min = 2, max = 150)
    private String title;

    private String description;

    private Task.Status status;

    private Task.Priority priority;

    private LocalDate dueDate;

    /** ID de la personne assignée (null = non assignée) */
    private Long assigneeId;
}
