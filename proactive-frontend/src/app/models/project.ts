export type ProjectStatus   = 'PLANNING' | 'ACTIVE' | 'ON_HOLD' | 'COMPLETED' | 'CANCELLED';
export type ProjectPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

export interface Project {
  id:           number;
  name:         string;
  description:  string | null;
  startDate:    string | null;
  endDate:      string | null;
  status:       ProjectStatus;
  priority:     ProjectPriority;
  budget:       number | null;
  progressPct:  number;
  createdAt:    string;
  updatedAt:    string;
  managerId:    number | null;
  managerName:  string | null;
  managerEmail: string | null;
  createdById:  number | null;
  createdByName:string | null;
}

export interface ProjectRequest {
  name?:        string;
  description?: string;
  startDate?:   string;
  endDate?:     string;
  status?:      ProjectStatus;
  priority?:    ProjectPriority;
  budget?:      number;
  progressPct?: number;
  managerId?:   number;
}

export interface ProjectStats {
  total:     number;
  planning:  number;
  active:    number;
  on_hold:   number;
  completed: number;
  cancelled: number;
}

export const STATUS_LABELS: Record<ProjectStatus, string> = {
  PLANNING:  'Planification',
  ACTIVE:    'En cours',
  ON_HOLD:   'En pause',
  COMPLETED: 'Terminé',
  CANCELLED: 'Annulé',
};

export const PRIORITY_LABELS: Record<ProjectPriority, string> = {
  LOW:      'Faible',
  MEDIUM:   'Moyenne',
  HIGH:     'Haute',
  CRITICAL: 'Critique',
};

export const STATUS_COLORS: Record<ProjectStatus, string> = {
  PLANNING:  '#f0a500',
  ACTIVE:    '#1a73e8',
  ON_HOLD:   '#8090a8',
  COMPLETED: '#27ae60',
  CANCELLED: '#e74c3c',
};

export const PRIORITY_COLORS: Record<ProjectPriority, string> = {
  LOW:      '#27ae60',
  MEDIUM:   '#f0a500',
  HIGH:     '#e67e22',
  CRITICAL: '#e74c3c',
};

export const ALL_STATUSES:   ProjectStatus[]   = ['PLANNING','ACTIVE','ON_HOLD','COMPLETED','CANCELLED'];
export const ALL_PRIORITIES: ProjectPriority[] = ['LOW','MEDIUM','HIGH','CRITICAL'];
