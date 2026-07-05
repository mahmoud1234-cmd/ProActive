import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  UserPermissionViewResponse,
  PermissionBulkUpdateRequest,
  RolePermissionResponse,
} from '../models/permission';

@Injectable({ providedIn: 'root' })
export class PermissionService {
  private api = `${environment.apiUrl}/permissions`;

  constructor(private http: HttpClient) {}

  getMyPermissions(): Observable<UserPermissionViewResponse> {
    return this.http.get<UserPermissionViewResponse>(`${this.api}/me`);
  }

  getUserPermissions(userId: number): Observable<UserPermissionViewResponse> {
    return this.http.get<UserPermissionViewResponse>(`${this.api}/users/${userId}`);
  }

  updateUserPermissions(userId: number, body: PermissionBulkUpdateRequest): Observable<UserPermissionViewResponse> {
    return this.http.put<UserPermissionViewResponse>(`${this.api}/users/${userId}`, body);
  }

  getRolePermissions(role: string): Observable<RolePermissionResponse> {
    return this.http.get<RolePermissionResponse>(`${this.api}/roles/${role}`);
  }

  applyPermissionsToRole(role: string, body: PermissionBulkUpdateRequest): Observable<RolePermissionResponse> {
    return this.http.put<RolePermissionResponse>(`${this.api}/roles/${role}`, body);
  }
}
