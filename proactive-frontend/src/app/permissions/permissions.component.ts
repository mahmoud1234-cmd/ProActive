import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';

import { AuthService } from '../services/auth.service';
import { UserService, UserSummary, PendingUser } from '../services/user.service';
import { PermissionService } from '../services/permission.service';
import {
  AccessLevel, PermissionFeature,
  UserPermissionViewResponse, RolePermissionResponse,
  ALL_FEATURES, ALL_ACCESS_LEVELS,
  ACCESS_LEVEL_LABELS, FEATURE_LABELS,
} from '../models/permission';

export type PermTab = 'role' | 'user' | 'pending';
export type ManagedRole = 'MANAGER' | 'USER';

@Component({
  selector: 'app-permissions',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './permissions.component.html',
  styleUrls: ['./permissions.component.css'],
})
export class PermissionsComponent implements OnInit {

  permTab: PermTab = 'pending';   // Ouvre sur les comptes en attente par défaut

  readonly allFeatures       = ALL_FEATURES;
  readonly allAccessLevels   = ALL_ACCESS_LEVELS;
  readonly accessLevelLabels = ACCESS_LEVEL_LABELS;
  readonly featureLabels     = FEATURE_LABELS;
  readonly managedRoles: ManagedRole[] = ['MANAGER', 'USER'];

  readonly featureIcons: Record<PermissionFeature, string> = {
    DASHBOARD:   'bi-grid-1x2-fill',
    PROJECTS:    'bi-kanban-fill',
    TASKS:       'bi-list-check',
    REPORTS:     'bi-graph-up-arrow',
    USERS:       'bi-people-fill',
    APPROVALS:   'bi-person-check-fill',
    PERMISSIONS: 'bi-shield-lock-fill',
  };

  // ── Pending ────────────────────────────────────────────────────
  pendingUsers: PendingUser[] = [];
  loadingPending = false;
  pendingError   = '';
  pendingSuccess = '';
  processingId: number | null = null;

  // ── Tab PAR RÔLE ───────────────────────────────────────────────
  selectedRole: ManagedRole = 'MANAGER';
  rolePermissions: RolePermissionResponse | null = null;
  roleEditable: Record<PermissionFeature, AccessLevel> = {} as any;
  loadingRole = false;
  savingRole  = false;
  roleError   = '';
  roleSuccess = '';

  // ── Tab PAR UTILISATEUR ────────────────────────────────────────
  users: UserSummary[] = [];
  selectedUser: UserSummary | null = null;
  userPermissions: UserPermissionViewResponse | null = null;
  userEditable: Record<PermissionFeature, AccessLevel> = {} as any;
  loadingUsers = false;
  loadingUser  = false;
  savingUser   = false;
  userError    = '';
  userSuccess  = '';

  constructor(
    private authService: AuthService,
    private router: Router,
    private userService: UserService,
    private permissionService: PermissionService,
  ) {}

  ngOnInit(): void {
    this.loadPendingUsers();
    this.loadUsers();
    this.loadRolePermissions(this.selectedRole);
  }

  get userName(): string {
    const u = this.authService.getCurrentUser();
    return u ? `${u.firstName} ${u.lastName}`.trim() : 'Admin';
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  setTab(tab: PermTab): void {
    this.permTab = tab;
    this.roleError = ''; this.roleSuccess = '';
    this.userError = ''; this.userSuccess = '';
    this.pendingError = ''; this.pendingSuccess = '';
    if (tab === 'pending') this.loadPendingUsers();
  }

  // ── PENDING ───────────────────────────────────────────────────
  loadPendingUsers(): void {
    this.loadingPending = true;
    this.pendingError = '';
    this.userService.getPendingUsers().subscribe({
      next: (list) => { this.pendingUsers = list; this.loadingPending = false; },
      error: () => { this.loadingPending = false; this.pendingError = 'Erreur chargement.'; },
    });
  }

  approve(user: PendingUser): void {
    this.processingId = user.id;
    this.userService.approveUser(user.id).subscribe({
      next: () => {
        this.processingId = null;
        this.pendingSuccess = `${user.firstName} ${user.lastName} approuvé(e).`;
        this.pendingUsers = this.pendingUsers.filter(u => u.id !== user.id);
        this.loadUsers(); // Rafraîchir la liste des users actifs
        setTimeout(() => this.pendingSuccess = '', 4000);
      },
      error: () => {
        this.processingId = null;
        this.pendingError = 'Erreur lors de l\'approbation.';
      },
    });
  }

  reject(user: PendingUser): void {
    this.processingId = user.id;
    this.userService.rejectUser(user.id).subscribe({
      next: () => {
        this.processingId = null;
        this.pendingSuccess = `${user.firstName} ${user.lastName} refusé(e).`;
        this.pendingUsers = this.pendingUsers.filter(u => u.id !== user.id);
        setTimeout(() => this.pendingSuccess = '', 4000);
      },
      error: () => {
        this.processingId = null;
        this.pendingError = 'Erreur lors du refus.';
      },
    });
  }

  // ── TAB RÔLE ──────────────────────────────────────────────────
  selectRole(role: ManagedRole): void {
    this.selectedRole = role;
    this.loadRolePermissions(role);
  }

  loadRolePermissions(role: ManagedRole): void {
    this.loadingRole = true;
    this.roleError = '';
    this.rolePermissions = null;
    this.permissionService.getRolePermissions(role).subscribe({
      next: (res) => {
        this.rolePermissions = res;
        this.roleEditable = {} as any;
        res.permissions.forEach(p => this.roleEditable[p.feature] = p.accessLevel);
        this.loadingRole = false;
      },
      error: () => { this.loadingRole = false; this.roleError = 'Impossible de charger.'; },
    });
  }

  saveRolePermissions(): void {
    this.savingRole = true;
    this.roleError = ''; this.roleSuccess = '';
    const permissions = this.allFeatures.map(f => ({ feature: f, accessLevel: this.roleEditable[f] }));
    this.permissionService.applyPermissionsToRole(this.selectedRole, { permissions }).subscribe({
      next: (res) => {
        this.rolePermissions = res;
        res.permissions.forEach(p => this.roleEditable[p.feature] = p.accessLevel);
        this.savingRole = false;
        this.roleSuccess = `Permissions appliquées à ${res.affectedUsers} compte(s).`;
        setTimeout(() => this.roleSuccess = '', 5000);
        if (this.selectedUser?.role === this.selectedRole) this.loadUserById(this.selectedUser);
      },
      error: (err) => {
        this.savingRole = false;
        if (err.status === 403) {
          this.roleError = 'Accès refusé. Reconnectez-vous avec le compte admin (admin@proactive.com).';
        } else {
          this.roleError = err.error?.message || 'Erreur lors de la sauvegarde.';
        }
      },
    });
  }

  resetRolePermissions(): void {
    this.rolePermissions?.permissions.forEach(p => this.roleEditable[p.feature] = p.accessLevel);
  }

  hasRoleChanges(): boolean {
    return this.rolePermissions?.permissions.some(p => this.roleEditable[p.feature] !== p.accessLevel) ?? false;
  }

  // ── TAB UTILISATEUR ───────────────────────────────────────────
  loadUsers(): void {
    this.loadingUsers = true;
    this.userService.getAllUsers().subscribe({
      next: (list) => { this.users = list.filter(u => u.role !== 'ADMIN'); this.loadingUsers = false; },
      error: () => { this.loadingUsers = false; },
    });
  }

  selectUser(user: UserSummary): void {
    if (this.selectedUser?.id === user.id) return;
    this.selectedUser = user;
    this.userPermissions = null;
    this.userSuccess = ''; this.userError = '';
    this.loadUserById(user);
  }

  private loadUserById(user: UserSummary): void {
    this.loadingUser = true;
    this.permissionService.getUserPermissions(user.id).subscribe({
      next: (perms) => {
        this.userPermissions = perms;
        this.userEditable = {} as any;
        perms.permissions.forEach(p => this.userEditable[p.feature] = p.accessLevel);
        this.loadingUser = false;
      },
      error: () => { this.loadingUser = false; this.userError = 'Erreur chargement.'; },
    });
  }

  saveUserPermissions(): void {
    if (!this.selectedUser) return;
    this.savingUser = true;
    this.userError = ''; this.userSuccess = '';
    const permissions = this.allFeatures.map(f => ({ feature: f, accessLevel: this.userEditable[f] }));
    this.permissionService.updateUserPermissions(this.selectedUser.id, { permissions }).subscribe({
      next: (updated) => {
        this.userPermissions = updated;
        updated.permissions.forEach(p => this.userEditable[p.feature] = p.accessLevel);
        this.savingUser = false;
        this.userSuccess = `Permissions de ${this.selectedUser!.firstName} mises à jour.`;
        setTimeout(() => this.userSuccess = '', 4000);
      },
      error: (err) => { this.savingUser = false; this.userError = err.error?.message || 'Erreur.'; },
    });
  }

  resetUserPermissions(): void {
    this.userPermissions?.permissions.forEach(p => this.userEditable[p.feature] = p.accessLevel);
  }

  hasUserChanges(): boolean {
    return this.userPermissions?.permissions.some(p => this.userEditable[p.feature] !== p.accessLevel) ?? false;
  }

  isCustom(feature: PermissionFeature): boolean {
    return this.userPermissions?.permissions.find(p => p.feature === feature)?.custom ?? false;
  }

  // ── Helpers ───────────────────────────────────────────────────
  getAccessClass(level: AccessLevel): string {
    return ({ NO_ACCESS:'lvl-none', READ_ONLY:'lvl-read', READ_WRITE:'lvl-write', FULL_ACCESS:'lvl-full' } as any)[level] ?? '';
  }
  getAccessIcon(level: AccessLevel): string {
    return ({ NO_ACCESS:'bi-slash-circle', READ_ONLY:'bi-eye', READ_WRITE:'bi-pencil-square', FULL_ACCESS:'bi-unlock-fill' } as any)[level] ?? '';
  }
  getRoleClass(role: string): string {
    return ({ ADMIN:'role-admin', MANAGER:'role-manager', USER:'role-user' } as any)[role] ?? 'role-user';
  }
  getRoleLabel(role: ManagedRole): string { return role === 'MANAGER' ? 'Manager' : 'Utilisateur'; }
  getRoleIcon(role: ManagedRole): string  { return role === 'MANAGER' ? 'bi-briefcase-fill' : 'bi-person-fill'; }
  getRoleIconForStr(role: string): string { return role === 'MANAGER' ? 'bi-briefcase-fill' : role === 'ADMIN' ? 'bi-shield-fill' : 'bi-person-fill'; }
  get usersForSelectedRole(): UserSummary[] { return this.users.filter(u => u.role === this.selectedRole); }
  get pendingCount(): number { return this.pendingUsers.length; }
}
