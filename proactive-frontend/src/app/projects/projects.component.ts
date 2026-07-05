import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RouterModule, Router } from '@angular/router';

import { AuthService } from '../services/auth.service';
import { ProjectService } from '../services/project.service';
import { UserService, UserSummary } from '../services/user.service';
import {
  Project, ProjectRequest, ProjectStats,
  ProjectStatus, ProjectPriority,
  STATUS_LABELS, PRIORITY_LABELS, STATUS_COLORS, PRIORITY_COLORS,
  ALL_STATUSES, ALL_PRIORITIES,
} from '../models/project';

type ViewMode = 'list' | 'kanban';

@Component({
  selector: 'app-projects',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, RouterModule],
  templateUrl: './projects.component.html',
  styleUrls: ['./projects.component.css'],
})
export class ProjectsComponent implements OnInit {

  // ── Données ───────────────────────────────────────────────────
  projects:    Project[]      = [];
  filtered:    Project[]      = [];
  managers:    UserSummary[]  = [];
  stats:       ProjectStats | null = null;
  selected:    Project | null = null;

  // ── UI ─────────────────────────────────────────────────────────
  viewMode:      ViewMode   = 'list';
  loading        = false;
  showForm       = false;
  showDeleteModal = false;
  editingId:     number | null = null;
  saving         = false;
  processingId:  number | null = null;
  searchTerm     = '';
  filterStatus:  ProjectStatus | '' = '';
  filterPriority: ProjectPriority | '' = '';
  errorMsg       = '';
  successMsg     = '';

  // ── Formulaire ────────────────────────────────────────────────
  form!: FormGroup;

  // ── Constantes ────────────────────────────────────────────────
  readonly statusLabels   = STATUS_LABELS;
  readonly priorityLabels = PRIORITY_LABELS;
  readonly statusColors   = STATUS_COLORS;
  readonly priorityColors = PRIORITY_COLORS;
  readonly allStatuses    = ALL_STATUSES;
  readonly allPriorities  = ALL_PRIORITIES;

  constructor(
    private fb: FormBuilder,
    private projectService: ProjectService,
    private userService: UserService,
    private authService: AuthService,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      name:        ['', [Validators.required, Validators.minLength(2)]],
      description: [''],
      startDate:   [''],
      endDate:     [''],
      status:      ['PLANNING'],
      priority:    ['MEDIUM'],
      budget:      [null],
      progressPct: [0, [Validators.min(0), Validators.max(100)]],
      managerId:   [null],
    });

    this.loadAll();
    if (this.isAdmin) this.loadManagers();
  }

  // ── Chargement ───────────────────────────────────────────────
  loadAll(): void {
    this.loading = true;
    this.projectService.getAll().subscribe({
      next: (list) => {
        this.projects = list;
        this.applyFilters();
        this.loading = false;
      },
      error: () => { this.loading = false; this.errorMsg = 'Erreur chargement projets.'; },
    });

    this.projectService.getStats().subscribe({
      next: (s) => this.stats = s,
      error: () => {},
    });
  }

  loadManagers(): void {
    this.userService.getAllUsers().subscribe({
      next: (users) => this.managers = users.filter(u => u.role === 'MANAGER' || u.role === 'ADMIN'),
      error: () => {},
    });
  }

  // ── Filtres ───────────────────────────────────────────────────
  applyFilters(): void {
    const t = this.searchTerm.toLowerCase();
    this.filtered = this.projects.filter(p => {
      const matchSearch = !t || p.name.toLowerCase().includes(t) ||
                          (p.description ?? '').toLowerCase().includes(t) ||
                          (p.managerName ?? '').toLowerCase().includes(t);
      const matchStatus   = !this.filterStatus   || p.status   === this.filterStatus;
      const matchPriority = !this.filterPriority || p.priority === this.filterPriority;
      return matchSearch && matchStatus && matchPriority;
    });
  }

  // ── Projets par statut (pour kanban) ─────────────────────────
  byStatus(status: ProjectStatus): Project[] {
    return this.filtered.filter(p => p.status === status);
  }

  // ── Sélection ─────────────────────────────────────────────────
  select(p: Project): void {
    this.selected = this.selected?.id === p.id ? null : p;
    this.showForm = false;
    this.showDeleteModal = false;
  }

  // ── Formulaire ────────────────────────────────────────────────
  openCreate(): void {
    this.editingId = null;
    this.form.reset({ status: 'PLANNING', priority: 'MEDIUM', progressPct: 0 });
    this.showForm = true;
    this.selected = null;
    this.showDeleteModal = false;
  }

  openEdit(p: Project): void {
    this.editingId = p.id;
    this.form.patchValue({
      name:        p.name,
      description: p.description ?? '',
      startDate:   p.startDate ?? '',
      endDate:     p.endDate ?? '',
      status:      p.status,
      priority:    p.priority,
      budget:      p.budget ?? '',
      progressPct: p.progressPct,
      managerId:   p.managerId ?? '',
    });
    this.showForm = true;
    this.showDeleteModal = false;
  }

  cancelForm(): void { this.showForm = false; this.editingId = null; }

  save(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.saving = true;
    this.errorMsg = '';

    const raw = this.form.value;
    const req: ProjectRequest = {
      name:        raw.name,
      description: raw.description || undefined,
      startDate:   raw.startDate   || undefined,
      endDate:     raw.endDate     || undefined,
      status:      raw.status,
      priority:    raw.priority,
      budget:      raw.budget      ? Number(raw.budget) : undefined,
      progressPct: raw.progressPct ? Number(raw.progressPct) : 0,
      managerId:   raw.managerId   ? Number(raw.managerId)   : undefined,
    };

    const obs = this.editingId
      ? this.projectService.update(this.editingId, req)
      : this.projectService.create(req);

    obs.subscribe({
      next: (saved) => {
        this.saving = false;
        this.showForm = false;
        if (this.editingId) {
          const idx = this.projects.findIndex(p => p.id === saved.id);
          if (idx > -1) this.projects[idx] = saved;
          if (this.selected?.id === saved.id) this.selected = saved;
        } else {
          this.projects.unshift(saved);
        }
        this.applyFilters();
        this.projectService.getStats().subscribe(s => this.stats = s);
        this.successMsg = this.editingId ? 'Projet mis à jour.' : 'Projet créé.';
        this.editingId = null;
        setTimeout(() => this.successMsg = '', 4000);
      },
      error: (err) => {
        this.saving = false;
        this.errorMsg = err.error?.message || 'Erreur lors de la sauvegarde.';
      },
    });
  }

  // ── Suppression ───────────────────────────────────────────────
  openDelete(p: Project): void {
    this.selected = p;
    this.showDeleteModal = true;
    this.showForm = false;
  }

  confirmDelete(): void {
    if (!this.selected) return;
    this.processingId = this.selected.id;
    this.projectService.delete(this.selected.id).subscribe({
      next: () => {
        this.processingId = null;
        this.projects = this.projects.filter(p => p.id !== this.selected!.id);
        this.applyFilters();
        this.projectService.getStats().subscribe(s => this.stats = s);
        this.successMsg = 'Projet supprimé.';
        this.selected = null;
        this.showDeleteModal = false;
        setTimeout(() => this.successMsg = '', 4000);
      },
      error: (err) => {
        this.processingId = null;
        this.errorMsg = err.error?.message || 'Erreur lors de la suppression.';
      },
    });
  }

  cancelDelete(): void { this.showDeleteModal = false; }

  // ── Auth ──────────────────────────────────────────────────────
  get userName(): string {
    const u = this.authService.getCurrentUser();
    return u ? `${u.firstName} ${u.lastName}`.trim() : '';
  }
  get userRole(): string { return this.authService.getCurrentUser()?.role ?? 'USER'; }
  get isAdmin():   boolean { return this.userRole === 'ADMIN'; }
  get canWrite():  boolean { return this.userRole === 'ADMIN' || this.userRole === 'MANAGER'; }

  logout(): void { this.authService.logout(); this.router.navigate(['/login']); }

  // ── Helpers visuels ───────────────────────────────────────────
  getRoleClass(role: string): string {
    return ({ ADMIN:'role-admin', MANAGER:'role-manager', USER:'role-user' } as any)[role] ?? 'role-user';
  }

  getStatusClass(s: ProjectStatus): string {
    return 'status-' + s.toLowerCase().replace('_', '-');
  }

  getPriorityClass(p: ProjectPriority): string {
    return 'priority-' + p.toLowerCase();
  }

  getDaysRemaining(endDate: string | null): number | null {
    if (!endDate) return null;
    const diff = new Date(endDate).getTime() - Date.now();
    return Math.ceil(diff / (1000 * 60 * 60 * 24));
  }

  // ── Drag & Drop Kanban ───────────────────────────────────────
  draggedProject: Project | null = null;
  dragOverStatus: ProjectStatus | null = null;

  onDragStart(event: DragEvent, project: Project): void {
    this.draggedProject = project;
    event.dataTransfer!.effectAllowed = 'move';
    event.dataTransfer!.setData('text/plain', String(project.id));
    // Ajouter un délai pour que l'élément disparaisse visuellement
    setTimeout(() => {
      const el = event.target as HTMLElement;
      el.classList.add('dragging');
    }, 0);
  }

  onDragEnd(event: DragEvent): void {
    const el = event.target as HTMLElement;
    el.classList.remove('dragging');
    this.draggedProject = null;
    this.dragOverStatus = null;
  }

  onDragOver(event: DragEvent, status: ProjectStatus): void {
    event.preventDefault();
    event.dataTransfer!.dropEffect = 'move';
    this.dragOverStatus = status;
  }

  onDragLeave(event: DragEvent): void {
    // Ne réinitialiser que si on quitte vraiment la colonne
    const related = event.relatedTarget as HTMLElement;
    const col = (event.currentTarget as HTMLElement);
    if (!col.contains(related)) {
      this.dragOverStatus = null;
    }
  }

  onDrop(event: DragEvent, targetStatus: ProjectStatus): void {
    event.preventDefault();
    this.dragOverStatus = null;

    if (!this.draggedProject || this.draggedProject.status === targetStatus) {
      this.draggedProject = null;
      return;
    }

    const project = this.draggedProject;
    const oldStatus = project.status;

    // Mise à jour optimiste locale
    const idx = this.projects.findIndex(p => p.id === project.id);
    if (idx > -1) this.projects[idx] = { ...project, status: targetStatus };
    this.applyFilters();
    this.draggedProject = null;

    // Persistance en base
    this.projectService.update(project.id, { status: targetStatus }).subscribe({
      next: (updated) => {
        const i = this.projects.findIndex(p => p.id === updated.id);
        if (i > -1) this.projects[i] = updated;
        if (this.selected?.id === updated.id) this.selected = updated;
        this.applyFilters();
        this.projectService.getStats().subscribe(s => this.stats = s);
        this.successMsg = `"${project.name}" déplacé vers ${this.statusLabels[targetStatus]}`;
        setTimeout(() => this.successMsg = '', 3000);
      },
      error: () => {
        // Rollback si échec
        const i = this.projects.findIndex(p => p.id === project.id);
        if (i > -1) this.projects[i] = { ...project, status: oldStatus };
        this.applyFilters();
        this.errorMsg = 'Erreur lors du déplacement.';
      },
    });
  }

  f(name: string) { return this.form.get(name)!; }
}
