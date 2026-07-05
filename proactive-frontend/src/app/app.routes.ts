import { Routes } from '@angular/router';
import { LoginComponent } from './auth/login/login.component';
import { RegisterComponent } from './auth/register/register.component';
import { AuthGuard } from './guards/auth.guard';
import { RoleGuard } from './guards/role.guard';

export const routes: Routes = [
  { path: '', redirectTo: '/login', pathMatch: 'full' },
  { path: 'login',    component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  {
    path: 'dashboard',
    loadComponent: () =>
      import('./dashboard/dashboard.component').then(m => m.DashboardComponent),
    canActivate: [AuthGuard],
  },
  {
    path: 'projects',
    loadComponent: () =>
      import('./projects/projects.component').then(m => m.ProjectsComponent),
    canActivate: [AuthGuard],
  },
  {
    path: 'approvals',
    loadComponent: () =>
      import('./approvals/approvals.component').then(m => m.ApprovalsComponent),
    canActivate: [AuthGuard],
  },
  {
    path: 'users',
    loadComponent: () =>
      import('./users/users.component').then(m => m.UsersComponent),
    canActivate: [AuthGuard],
  },
  {
    path: 'profile',
    loadComponent: () =>
      import('./profile/profile.component').then(m => m.ProfileComponent),
    canActivate: [AuthGuard],
  },
  {
    path: 'permissions',
    loadComponent: () =>
      import('./permissions/permissions.component').then(m => m.PermissionsComponent),
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['ADMIN'] },
  },
  { path: 'admin', redirectTo: '/permissions', pathMatch: 'full' },
  { path: '**', redirectTo: '/login' },
];
