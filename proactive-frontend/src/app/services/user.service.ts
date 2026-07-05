import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface UserSummary {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  role: 'ADMIN' | 'MANAGER' | 'USER';
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'BANNED';
  createdAt: string;
}

export interface PendingUser {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  role: 'ADMIN' | 'MANAGER' | 'USER';
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'BANNED';
  createdAt: string;
}

export interface EditUserRequest {
  firstName?: string;
  lastName?: string;
  email?: string;
  role?: 'ADMIN' | 'MANAGER' | 'USER';
}

@Injectable({ providedIn: 'root' })
export class UserService {
  private api = `${environment.apiUrl}/users`;

  constructor(private http: HttpClient) {}

  getAllUsers(role?: string, status?: string): Observable<UserSummary[]> {
    let params = new HttpParams();
    if (role)   params = params.set('role', role);
    if (status) params = params.set('status', status);
    return this.http.get<UserSummary[]>(this.api, { params });
  }

  getUserById(userId: number): Observable<UserSummary> {
    return this.http.get<UserSummary>(`${this.api}/${userId}`);
  }

  updateUser(userId: number, req: EditUserRequest): Observable<UserSummary> {
    return this.http.patch<UserSummary>(`${this.api}/${userId}`, req);
  }

  banUser(userId: number): Observable<UserSummary> {
    return this.http.put<UserSummary>(`${this.api}/${userId}/ban`, {});
  }

  unbanUser(userId: number): Observable<UserSummary> {
    return this.http.put<UserSummary>(`${this.api}/${userId}/unban`, {});
  }

  deleteUser(userId: number): Observable<{ message: string }> {
    return this.http.delete<{ message: string }>(`${this.api}/${userId}`);
  }

  getPendingUsers(): Observable<PendingUser[]> {
    return this.http.get<PendingUser[]>(`${this.api}/pending`);
  }

  approveUser(userId: number): Observable<PendingUser> {
    return this.http.put<PendingUser>(`${this.api}/${userId}/approve`, {});
  }

  rejectUser(userId: number): Observable<PendingUser> {
    return this.http.put<PendingUser>(`${this.api}/${userId}/reject`, {});
  }
}
