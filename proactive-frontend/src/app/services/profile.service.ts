import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { UserSummary } from './user.service';

export interface UpdateProfileRequest {
  firstName?:       string;
  lastName?:        string;
  email?:           string;
  currentPassword?: string;
  newPassword?:     string;
}

export interface UpdateProfileResponse {
  user:  UserSummary;
  token: string;
}

@Injectable({ providedIn: 'root' })
export class ProfileService {
  private api = `${environment.apiUrl}/profile`;

  constructor(private http: HttpClient) {}

  getProfile(): Observable<UserSummary> {
    return this.http.get<UserSummary>(this.api);
  }

  updateProfile(req: UpdateProfileRequest): Observable<UpdateProfileResponse> {
    return this.http.patch<UpdateProfileResponse>(this.api, req);
  }
}
