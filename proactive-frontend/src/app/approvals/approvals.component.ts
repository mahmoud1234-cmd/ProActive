import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { UserService, PendingUser } from '../services/user.service';
import { PermissionService } from '../services/permission.service';
import { AccessLevel, PermissionFeature } from '../models/permission';

@Component({
  selector: 'app-approvals',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './approvals.component.html',
  styleUrls: ['./approvals.component.css'],
})
export class ApprovalsComponent implements OnInit {

  pendingUsers: PendingUser[] = [];
  loading      = false;
  processingId: number | null = null;
  errorMsg     = '';
  successMsg   = '';

  myApprovalLevel: AccessLevel = 'NO_ACCESS';

  constructor(
    private userService: UserService,
    private authService: AuthService,
    private permissionService: PermissionService,
    private router: Router,
  ) {}

  ngOnInit(): void {
    // Charger le niveau de permission APPROVALS de l'utilisateur connecté
    this.permissionService.getMyPermissions().subscribe({
      next: (res) => {
        const perm = res.permissions.find(p => p.feature === 'APPROVALS');
        this.myApprovalLevel = perm?.accessLevel ?? 'NO_ACCESS';
      },
    });
    this.load();
  }

  load(): void {
    this.loading = true;
    this.errorMsg = '';
    this.userService.getPendingUsers().subscribe({
      next: (list) => { this.pendingUsers = list; this.loading = false; },
      error: (err) => {
        this.loading = false;
        this.errorMsg = err.status === 403
          ? 'Accès refusé — permission insuffisante.'
          : 'Erreur lors du chargement.';
      },
    });
  }

  approve(user: PendingUser): void {
    this.processingId = user.id;
    this.userService.approveUser(user.id).subscribe({
      next: () => {
        this.processingId = null;
        this.pendingUsers = this.pendingUsers.filter(u => u.id !== user.id);
        this.successMsg = `${user.firstName} ${user.lastName} approuvé(e).`;
        setTimeout(() => this.successMsg = '', 4000);
      },
      error: () => { this.processingId = null; this.errorMsg = 'Erreur lors de l\'approbation.'; },
    });
  }

  reject(user: PendingUser): void {
    this.processingId = user.id;
    this.userService.rejectUser(user.id).subscribe({
      next: () => {
        this.processingId = null;
        this.pendingUsers = this.pendingUsers.filter(u => u.id !== user.id);
        this.successMsg = `${user.firstName} ${user.lastName} refusé(e).`;
        setTimeout(() => this.successMsg = '', 4000);
      },
      error: () => { this.processingId = null; this.errorMsg = 'Erreur lors du refus.'; },
    });
  }

  /** Peut voir les pending (READ_ONLY suffit) */
  get canView(): boolean {
    if (this.isAdmin) return true;
    return this.myApprovalLevel !== 'NO_ACCESS';
  }

  /** Peut approuver/rejeter (READ_WRITE requis) */
  get canWrite(): boolean {
    if (this.isAdmin) return true;
    return this.myApprovalLevel === 'READ_WRITE' || this.myApprovalLevel === 'FULL_ACCESS';
  }

  get isAdmin(): boolean { return this.authService.getUserRole() === 'ADMIN'; }

  get userName(): string {
    const u = this.authService.getCurrentUser();
    return u ? `${u.firstName} ${u.lastName}`.trim() : '';
  }

  get userRole(): string {
    return this.authService.getCurrentUser()?.role ?? 'USER';
  }

  getRoleClass(role: string): string {
    return ({ ADMIN: 'role-admin', MANAGER: 'role-manager', USER: 'role-user' } as any)[role] ?? 'role-user';
  }

  getRoleIcon(role: string): string {
    return ({ ADMIN: 'bi-shield-fill', MANAGER: 'bi-briefcase-fill', USER: 'bi-person-fill' } as any)[role] ?? 'bi-person-fill';
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
