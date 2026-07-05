import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Project, ProjectRequest, ProjectStats } from '../models/project';

@Injectable({ providedIn: 'root' })
export class ProjectService {
  private api = `${environment.apiUrl}/projects`;

  constructor(private http: HttpClient) {}

  getAll(): Observable<Project[]> {
    return this.http.get<Project[]>(this.api);
  }

  getStats(): Observable<ProjectStats> {
    return this.http.get<ProjectStats>(`${this.api}/stats`);
  }

  getById(id: number): Observable<Project> {
    return this.http.get<Project>(`${this.api}/${id}`);
  }

  create(req: ProjectRequest): Observable<Project> {
    return this.http.post<Project>(this.api, req);
  }

  update(id: number, req: ProjectRequest): Observable<Project> {
    return this.http.put<Project>(`${this.api}/${id}`, req);
  }

  delete(id: number): Observable<{ message: string }> {
    return this.http.delete<{ message: string }>(`${this.api}/${id}`);
  }
}
