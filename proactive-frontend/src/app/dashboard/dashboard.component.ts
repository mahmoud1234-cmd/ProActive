import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { Router, RouterModule } from '@angular/router';

import { AuthService } from '../services/auth.service';
import { PermissionService } from '../services/permission.service';
import { AccessLevel, PermissionFeature, FEATURE_LABELS } from '../models/permission';

interface NavItem {
  feature: PermissionFeature;
  label: string;
  icon: string;
  adminOnly?: boolean;
  route?: string;
}

const DEFAULT_PERMISSIONS: Record<string, Record<PermissionFeature, AccessLevel>> = {
  ADMIN: {
    DASHBOARD: 'FULL_ACCESS', PROJECTS: 'FULL_ACCESS', TASKS: 'FULL_ACCESS',
    REPORTS: 'FULL_ACCESS',   USERS: 'FULL_ACCESS',    APPROVALS: 'FULL_ACCESS',
    PERMISSIONS: 'FULL_ACCESS',
  },
  MANAGER: {
    DASHBOARD: 'READ_WRITE', PROJECTS: 'READ_WRITE', TASKS: 'READ_WRITE',
    REPORTS: 'READ_WRITE',   USERS: 'READ_ONLY',     APPROVALS: 'READ_WRITE',
    PERMISSIONS: 'NO_ACCESS',
  },
  USER: {
    DASHBOARD: 'READ_ONLY', PROJECTS: 'READ_ONLY', TASKS: 'READ_ONLY',
    REPORTS: 'READ_ONLY',   USERS: 'NO_ACCESS',    APPROVALS: 'NO_ACCESS',
    PERMISSIONS: 'NO_ACCESS',
  },
};

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css'],
})
export class DashboardComponent implements OnInit {

  sidebarOpen  = false;
  loadingPerms = true;
  myPermissions: Record<PermissionFeature, AccessLevel> = {} as any;

  readonly featureLabels = FEATURE_LABELS;

  readonly allNavItems: NavItem[] = [
    { feature: 'DASHBOARD',   label: 'Dashboard',    icon: 'bi-grid-1x2-fill' },
    { feature: 'PROJECTS',    label: 'Projets',      icon: 'bi-kanban-fill' },
    { feature: 'TASKS',       label: 'Tâches',       icon: 'bi-list-check' },
    { feature: 'REPORTS',     label: 'Rapports',     icon: 'bi-graph-up-arrow' },
    { feature: 'USERS',       label: 'Utilisateurs', icon: 'bi-people-fill',       route: '/users' },
    { feature: 'APPROVALS',   label: 'Approbations', icon: 'bi-person-check-fill', route: '/approvals' },
    { feature: 'PERMISSIONS', label: 'Permissions',  icon: 'bi-shield-lock-fill',  adminOnly: true, route: '/permissions' },
  ];

  constructor(
    private authService: AuthService,
    private router: Router,
    private permissionService: PermissionService,
  ) {}

  ngOnInit(): void {
    this.permissionService.getMyPermissions().subscribe({
      next: (res) => {
        res.permissions.forEach(p => this.myPermissions[p.feature] = p.accessLevel);
        this.loadingPerms = false;
      },
      error: () => {
        this.applyDefaultPermissions();
        this.loadingPerms = false;
      },
    });
  }

  private applyDefaultPermissions(): void {
    const defaults = DEFAULT_PERMISSIONS[this.userRole] ?? DEFAULT_PERMISSIONS['USER'];
    Object.entries(defaults).forEach(([f, l]) => {
      this.myPermissions[f as PermissionFeature] = l;
    });
  }

  get userName(): string {
    const u = this.authService.getCurrentUser();
    return u ? `${u.firstName} ${u.lastName}`.trim() : 'Utilisateur';
  }

  get userRole(): string {
    return this.authService.getCurrentUser()?.role ?? 'USER';
  }

  get isAdmin(): boolean { return this.userRole === 'ADMIN'; }

  get visibleNavItems(): NavItem[] {
    return this.allNavItems.filter(item => {
      if (item.adminOnly) return this.isAdmin;
      return this.canAccess(item.feature);
    });
  }

  canAccess(feature: PermissionFeature): boolean {
    if (this.isAdmin) return true;
    const level = this.myPermissions[feature];
    return !!level && level !== 'NO_ACCESS';
  }

  canWrite(feature: PermissionFeature): boolean {
    if (this.isAdmin) return true;
    const level = this.myPermissions[feature];
    return level === 'READ_WRITE' || level === 'FULL_ACCESS';
  }

  getAccessIcon(feature: PermissionFeature): string {
    if (this.isAdmin) return '';
    const level = this.myPermissions[feature];
    const icons: Partial<Record<AccessLevel, string>> = {
      READ_ONLY:   'bi-eye',
      READ_WRITE:  'bi-pencil-square',
      FULL_ACCESS: 'bi-unlock-fill',
    };
    return icons[level] ?? '';
  }

  getRoleClass(role: string): string {
    const map: Record<string, string> = {
      ADMIN: 'role-admin', MANAGER: 'role-manager', USER: 'role-user',
    };
    return map[role] ?? 'role-user';
  }

  toggleSidebar(): void { this.sidebarOpen = !this.sidebarOpen; }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
