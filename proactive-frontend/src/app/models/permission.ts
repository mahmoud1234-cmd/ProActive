export type AccessLevel = 'NO_ACCESS' | 'READ_ONLY' | 'READ_WRITE' | 'FULL_ACCESS';

export type PermissionFeature =
  | 'DASHBOARD'
  | 'PROJECTS'
  | 'TASKS'
  | 'REPORTS'
  | 'USERS'
  | 'APPROVALS'
  | 'PERMISSIONS';

export type UserRole = 'ADMIN' | 'MANAGER' | 'USER';

export interface FeaturePermissionView {
  feature: PermissionFeature;
  accessLevel: AccessLevel;
  custom: boolean;
}

export interface UserPermissionViewResponse {
  userId: number;
  email: string;
  role: UserRole;
  permissions: FeaturePermissionView[];
}

export interface PermissionAssignmentRequest {
  feature: PermissionFeature;
  accessLevel: AccessLevel;
}

export interface PermissionBulkUpdateRequest {
  permissions: PermissionAssignmentRequest[];
}

export interface RolePermissionResponse {
  role: UserRole;
  permissions: FeaturePermissionView[];
  affectedUsers: number;
}

export const ALL_FEATURES: PermissionFeature[] = [
  'DASHBOARD', 'PROJECTS', 'TASKS', 'REPORTS', 'USERS', 'APPROVALS', 'PERMISSIONS',
];

export const ALL_ACCESS_LEVELS: AccessLevel[] = [
  'NO_ACCESS', 'READ_ONLY', 'READ_WRITE', 'FULL_ACCESS',
];

export const ACCESS_LEVEL_LABELS: Record<AccessLevel, string> = {
  NO_ACCESS:   'Aucun accès',
  READ_ONLY:   'Lecture seule',
  READ_WRITE:  'Lecture / Écriture',
  FULL_ACCESS: 'Accès complet',
};

export const FEATURE_LABELS: Record<PermissionFeature, string> = {
  DASHBOARD:   'Tableau de bord',
  PROJECTS:    'Projets',
  TASKS:       'Tâches',
  REPORTS:     'Rapports',
  USERS:       'Utilisateurs',
  APPROVALS:   'Approbations',
  PERMISSIONS: 'Permissions',
};
