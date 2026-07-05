import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RouterModule, Router } from '@angular/router';

import { AuthService } from '../services/auth.service';
import { UserService, UserSummary, EditUserRequest } from '../services/user.service';

type RoleFilter   = '' | 'ADMIN' | 'MANAGER' | 'USER';
type StatusFilter = '' | 'APPROVED' | 'BANNED' | 'REJECTED';

@Component({
  selector: 'app-users',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, RouterModule],
  templateUrl: './users.component.html',
  styleUrls: ['./users.component.css'],
})
export class UsersComponent implements OnInit {

  // ── Données ───────────────────────────────────────────────────
  allUsers:     UserSummary[] = [];
  filteredUsers: UserSummary[] = [];
  selectedUser: UserSummary | null = null;

  // ── Filtres ───────────────────────────────────────────────────
  searchTerm:   string = '';
  roleFilter:   RoleFilter   = '';
  statusFilter: StatusFilter = '';

  // ── UI ─────────────────────────────────────────────────────────
  loading        = false;
  processingId: number | null = null;
  showEditModal  = false;
  showDeleteModal = false;
  errorMsg   = '';
  successMsg = '';

  // ── Formulaire édition ────────────────────────────────────────
  editForm!: FormGroup;

  readonly roles   = ['USER', 'MANAGER', 'ADMIN'] as const;
  readonly statuses: StatusFilter[] = ['', 'APPROVED', 'BANNED', 'REJECTED'];

  constructor(
    private userService: UserService,
    private authService: AuthService,
    private router: Router,
    private fb: FormBuilder,
  ) {}

  ngOnInit(): void {
    this.editForm = this.fb.group({
      firstName: ['', [Validators.required, Validators.minLength(2)]],
      lastName:  ['', [Validators.required, Validators.minLength(2)]],
      email:     ['', [Validators.required, Validators.email]],
      role:      ['USER', Validators.required],
    });
    this.loadUsers();
  }

  // ── Chargement ────────────────────────────────────────────────
  loadUsers(): void {
    this.loading = true;
    this.errorMsg = '';
    this.userService.getAllUsers().subscribe({
      next: (users) => {
        this.allUsers = users;
        this.applyFilters();
        this.loading = false;
      },
      error: () => { this.loading = false; this.errorMsg = 'Erreur lors du chargement.'; },
    });
  }

  // ── Filtres ───────────────────────────────────────────────────
  applyFilters(): void {
    const term = this.searchTerm.toLowerCase();
    this.filteredUsers = this.allUsers.filter(u => {
      const matchSearch = !term ||
        u.firstName.toLowerCase().includes(term) ||
        u.lastName.toLowerCase().includes(term)  ||
        u.email.toLowerCase().includes(term);
      const matchRole   = !this.roleFilter   || u.role   === this.roleFilter;
      const matchStatus = !this.statusFilter || u.status === this.statusFilter;
      return matchSearch && matchRole && matchStatus;
    });
  }

  // ── Sélection ─────────────────────────────────────────────────
  selectUser(user: UserSummary): void {
    this.selectedUser = this.selectedUser?.id === user.id ? null : user;
    this.showEditModal  = false;
    this.showDeleteModal = false;
    this.errorMsg = '';
    this.successMsg = '';
  }

  // ── Édition ───────────────────────────────────────────────────
  openEdit(): void {
    if (!this.selectedUser) return;
    this.editForm.patchValue({
      firstName: this.selectedUser.firstName,
      lastName:  this.selectedUser.lastName,
      email:     this.selectedUser.email,
      role:      this.selectedUser.role,
    });
    this.showEditModal = true;
    this.showDeleteModal = false;
  }

  saveEdit(): void {
    if (!this.selectedUser || this.editForm.invalid) return;
    this.processingId = this.selectedUser.id;
    const req: EditUserRequest = this.editForm.value;

    this.userService.updateUser(this.selectedUser.id, req).subscribe({
      next: (updated) => {
        this.processingId = null;
        this.showEditModal = false;
        // Mettre à jour dans la liste
        const idx = this.allUsers.findIndex(u => u.id === updated.id);
        if (idx > -1) this.allUsers[idx] = updated;
        this.selectedUser = updated;
        this.applyFilters();
        this.successMsg = `${updated.firstName} ${updated.lastName} mis à jour.`;
        setTimeout(() => this.successMsg = '', 4000);
      },
      error: (err) => {
        this.processingId = null;
        this.errorMsg = err.error?.message || 'Erreur lors de la mise à jour.';
      },
    });
  }

  // ── Ban / Unban ───────────────────────────────────────────────
  toggleBan(): void {
    if (!this.selectedUser) return;
    const isBanned = this.selectedUser.status === 'BANNED';
    this.processingId = this.selectedUser.id;

    const action = isBanned
      ? this.userService.unbanUser(this.selectedUser.id)
      : this.userService.banUser(this.selectedUser.id);

    action.subscribe({
      next: (updated) => {
        this.processingId = null;
        const idx = this.allUsers.findIndex(u => u.id === updated.id);
        if (idx > -1) this.allUsers[idx] = updated;
        this.selectedUser = updated;
        this.applyFilters();
        this.successMsg = isBanned
          ? `${updated.firstName} débanni.`
          : `${updated.firstName} banni.`;
        setTimeout(() => this.successMsg = '', 4000);
      },
      error: (err) => {
        this.processingId = null;
        this.errorMsg = err.error?.message || 'Erreur.';
      },
    });
  }

  // ── Suppression ───────────────────────────────────────────────
  openDelete(): void {
    this.showDeleteModal = true;
    this.showEditModal   = false;
  }

  confirmDelete(): void {
    if (!this.selectedUser) return;
    this.processingId = this.selectedUser.id;

    this.userService.deleteUser(this.selectedUser.id).subscribe({
      next: () => {
        this.processingId = null;
        this.allUsers = this.allUsers.filter(u => u.id !== this.selectedUser!.id);
        this.selectedUser = null;
        this.showDeleteModal = false;
        this.applyFilters();
        this.successMsg = 'Utilisateur supprimé.';
        setTimeout(() => this.successMsg = '', 4000);
      },
      error: (err) => {
        this.processingId = null;
        this.errorMsg = err.error?.message || 'Erreur lors de la suppression.';
      },
    });
  }

  cancelDelete(): void { this.showDeleteModal = false; }
  cancelEdit():   void { this.showEditModal   = false; }

  // ── Auth ──────────────────────────────────────────────────────
  get userName(): string {
    const u = this.authService.getCurrentUser();
    return u ? `${u.firstName} ${u.lastName}`.trim() : '';
  }
  get userRole(): string { return this.authService.getCurrentUser()?.role ?? 'USER'; }
  get isAdmin():  boolean { return this.userRole === 'ADMIN'; }

  logout(): void { this.authService.logout(); this.router.navigate(['/login']); }

  // ── Helpers visuels ──────────────────────────────────────────
  getRoleClass(role: string): string {
    return ({ ADMIN: 'role-admin', MANAGER: 'role-manager', USER: 'role-user' } as any)[role] ?? 'role-user';
  }

  getStatusClass(status: string): string {
    const m: Record<string, string> = {
      APPROVED: 'status-active',
      BANNED:   'status-banned',
      REJECTED: 'status-rejected',
      PENDING:  'status-pending',
    };
    return m[status] ?? '';
  }

  getStatusLabel(status: string): string {
    const m: Record<string, string> = {
      APPROVED: 'Actif',
      BANNED:   'Banni',
      REJECTED: 'Rejeté',
      PENDING:  'En attente',
    };
    return m[status] ?? status;
  }

  getStatusIcon(status: string): string {
    const m: Record<string, string> = {
      APPROVED: 'bi-check-circle-fill',
      BANNED:   'bi-slash-circle-fill',
      REJECTED: 'bi-x-circle-fill',
      PENDING:  'bi-hourglass-split',
    };
    return m[status] ?? 'bi-circle';
  }

  get userCount(): { total: number; active: number; banned: number; rejected: number } {
    return {
      total:    this.allUsers.length,
      active:   this.allUsers.filter(u => u.status === 'APPROVED').length,
      banned:   this.allUsers.filter(u => u.status === 'BANNED').length,
      rejected: this.allUsers.filter(u => u.status === 'REJECTED').length,
    };
  }

  initials(u: UserSummary): string {
    return `${u.firstName[0] ?? ''}${u.lastName[0] ?? ''}`.toUpperCase();
  }

  isProcessing(id: number): boolean { return this.processingId === id; }
}
