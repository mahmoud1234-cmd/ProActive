package com.proactive.demo.service;

import com.proactive.demo.dto.task.TaskRequest;
import com.proactive.demo.dto.task.TaskResponse;
import com.proactive.demo.model.Project;
import com.proactive.demo.model.Task;
import com.proactive.demo.model.User;
import com.proactive.demo.repository.ProjectRepository;
import com.proactive.demo.repository.TaskRepository;
import com.proactive.demo.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    // ── Lecture ──────────────────────────────────────────────────────────

    /** Toutes les tâches d'un projet */
    public List<TaskResponse> getByProject(Long projectId) {
        Project project = getProject(projectId);
        return taskRepository.findAllByProjectOrderByCreatedAtDesc(project)
                .stream().map(TaskResponse::from).toList();
    }

    /** Tâches de l'utilisateur connecté (assignées ou créées par lui) */
    public List<TaskResponse> getMyTasks() {
        User me = currentUser();
        return taskRepository.findAllByAssigneeOrCreatedBy(me)
                .stream().map(TaskResponse::from).toList();
    }

    /** Détail d'une tâche */
    public TaskResponse getById(Long taskId) {
        return TaskResponse.from(getTask(taskId));
    }

    /** Statistiques des tâches d'un projet */
    public Map<String, Long> getStats(Long projectId) {
        Project project = getProject(projectId);
        return Map.of(
            "total",    taskRepository.countByProject(project),
            "a_faire",  taskRepository.countByProjectAndStatus(project, Task.Status.A_FAIRE),
            "en_cours", taskRepository.countByProjectAndStatus(project, Task.Status.EN_COURS),
            "termine",  taskRepository.countByProjectAndStatus(project, Task.Status.TERMINE)
        );
    }

    // ── Écriture ─────────────────────────────────────────────────────────

    @Transactional
    public TaskResponse create(Long projectId, TaskRequest req) {
        Task task = new Task();
        task.setProject(getProject(projectId));
        task.setCreatedBy(currentUser());
        applyRequest(task, req);
        return TaskResponse.from(taskRepository.save(task));
    }

    @Transactional
    public TaskResponse update(Long taskId, TaskRequest req) {
        Task task = getTask(taskId);
        applyRequest(task, req);
        return TaskResponse.from(taskRepository.save(task));
    }

    /** Changer uniquement le statut (drag & drop kanban) */
    @Transactional
    public TaskResponse updateStatus(Long taskId, Task.Status newStatus) {
        Task task = getTask(taskId);
        task.setStatus(newStatus);
        return TaskResponse.from(taskRepository.save(task));
    }

    @Transactional
    public void delete(Long taskId) {
        if (!taskRepository.existsById(taskId))
            throw new RuntimeException("Tâche non trouvée");
        taskRepository.deleteById(taskId);
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private void applyRequest(Task task, TaskRequest req) {
        if (req.getTitle()       != null) task.setTitle(req.getTitle());
        if (req.getDescription() != null) task.setDescription(req.getDescription());
        if (req.getStatus()      != null) task.setStatus(req.getStatus());
        if (req.getPriority()    != null) task.setPriority(req.getPriority());
        if (req.getDueDate()     != null) task.setDueDate(req.getDueDate());

        // Assignee — peut être null (désassigner)
        if (req.getAssigneeId() != null) {
            task.setAssignee(userRepository.findById(req.getAssigneeId())
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé")));
        } else if (req.getAssigneeId() == null && task.getId() != null) {
            // Permettre de désassigner explicitement
            task.setAssignee(null);
        }
    }

    private Project getProject(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Projet non trouvé"));
    }

    private Task getTask(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tâche non trouvée"));
    }

    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
    }
}
