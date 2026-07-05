import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';

import { UserService, UserSummary } from '../services/user.service';
import { PermissionService } from '../services/permission.service';
import { AuthService } from '../services/auth.service';
import {
  UserPermissionViewResponse,
  AccessLevel,
  PermissionFeature,
  ALL_FEATURES,
  ALL_ACCESS_LEVELS,
  ACCESS_LEVEL_LABELS,
  FEATURE_LABELS,
} from '../models/permission';

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './admin.component.html',
  styleUrls: ['./admin.component.css'],
})
export class AdminComponent implements OnInit {
  users: UserSummary[] = [];
  selectedUser: UserSummary | null = null;
  userPermissions: UserPermissionViewResponse | null = null;
  editablePermissions: Record<PermissionFeature, AccessLevel> = {} as Record<PermissionFeature, AccessLevel>;

  loading = false;
  loadingPermissions = false;
  saving = false;
  errorMessage = '';
  successMessage = '';

  readonly allFeatures = ALL_FEATURES;
  readonly allAccessLevels = ALL_ACCESS_LEVELS;
  readonly accessLevelLabels = ACCESS_LEVEL_LABELS;
  readonly featureLabels = FEATURE_LABELS;

  readonly featureIcons: Record<PermissionFeature, string> = {
    DASHBOARD:   'bi-grid-1x2-fill',
    PROJECTS:    'bi-kanban-fill',
    TASKS:       'bi-list-check',
    REPORTS:     'bi-graph-up-arrow',
    USERS:       'bi-people-fill',
    PERMISSIONS: 'bi-shield-lock-fill',
  };

  constructor(
    private userService: UserService,
    private permissionService: PermissionService,
    private authService: AuthService,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(): void {
    this.loading = true;
    this.errorMessage = '';
    this.userService.getAllUsers().subscribe({
      next: (users) => {
        this.users = users.filter((u) => u.role !== 'ADMIN');
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        this.errorMessage = err.status === 403
          ? 'Accès refusé. Vous devez être administrateur.'
          : 'Erreur lors du chargement des utilisateurs.';
      },
    });
  }

  selectUser(user: UserSummary): void {
    if (this.selectedUser?.id === user.id) return;
    this.selectedUser = user;
    this.userPermissions = null;
    this.successMessage = '';
    this.errorMessage = '';
    this.loadingPermissions = true;

    this.permissionService.getUserPermissions(user.id).subscribe({
      next: (perms) => {
        this.userPermissions = perms;
        this.editablePermissions = {} as Record<PermissionFeature, AccessLevel>;
        perms.permissions.forEach((p) => {
          this.editablePermissions[p.feature] = p.accessLevel;
        });
        this.loadingPermissions = false;
      },
      error: () => {
        this.loadingPermissions = false;
        this.errorMessage = 'Erreur lors du chargement des permissions.';
      },
    });
  }

  savePermissions(): void {
    if (!this.selectedUser) return;
    this.saving = true;
    this.successMessage = '';
    this.errorMessage = '';

    const permissions = this.allFeatures.map((feature) => ({
      feature,
      accessLevel: this.editablePermissions[feature],
    }));

    this.permissionService.updateUserPermissions(this.selectedUser.id, { permissions }).subscribe({
      next: (updated) => {
        this.userPermissions = updated;
        // Rafraîchir les permissions éditables avec la réponse du serveur
        updated.permissions.forEach((p) => {
          this.editablePermissions[p.feature] = p.accessLevel;
        });
        this.saving = false;
        this.successMessage = `Permissions de ${this.selectedUser!.firstName} ${this.selectedUser!.lastName} mises à jour.`;
        setTimeout(() => (this.successMessage = ''), 4000);
      },
      error: (err) => {
        this.saving = false;
        this.errorMessage = err.error?.message || 'Erreur lors de la sauvegarde.';
      },
    });
  }

  resetToDefault(): void {
    if (!this.userPermissions) return;
    this.userPermissions.permissions.forEach((p) => {
      this.editablePermissions[p.feature] = p.accessLevel;
    });
  }

  isCustom(feature: PermissionFeature): boolean {
    return this.userPermissions?.permissions.find((p) => p.feature === feature)?.custom ?? false;
  }

  hasChanges(): boolean {
    if (!this.userPermissions) return false;
    return this.userPermissions.permissions.some(
      (p) => this.editablePermissions[p.feature] !== p.accessLevel
    );
  }

  getAccessClass(level: AccessLevel): string {
    const map: Record<AccessLevel, string> = {
      NO_ACCESS:   'level-none',
      READ_ONLY:   'level-read',
      READ_WRITE:  'level-write',
      FULL_ACCESS: 'level-full',
    };
    return map[level];
  }

  getAccessIcon(level: AccessLevel): string {
    const map: Record<AccessLevel, string> = {
      NO_ACCESS:   'bi-slash-circle',
      READ_ONLY:   'bi-eye',
      READ_WRITE:  'bi-pencil-square',
      FULL_ACCESS: 'bi-unlock-fill',
    };
    return map[level];
  }

  getRoleClass(role: string): string {
    const map: Record<string, string> = {
      ADMIN:   'role-admin',
      MANAGER: 'role-manager',
      USER:    'role-user',
    };
    return map[role] ?? 'role-user';
  }

  get currentUserName(): string {
    const u = this.authService.getCurrentUser();
    return u ? `${u.firstName} ${u.lastName}`.trim() : 'Admin';
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
