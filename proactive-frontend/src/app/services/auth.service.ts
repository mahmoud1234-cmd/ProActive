import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject, tap, throwError } from 'rxjs';
import { environment } from '../../environments/environment';
import { LoginRequest, RegisterRequest, AuthResponse } from '../models/auth';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private apiUrl = `${environment.apiUrl}/auth`;
  private currentUserSubject = new BehaviorSubject<AuthResponse | null>(null);
  public currentUser$ = this.currentUserSubject.asObservable();

  constructor(private http: HttpClient) {
    this.loadStoredUser();
  }

  private loadStoredUser(): void {
    const token = localStorage.getItem('token');
    if (token) {
      try {
        const payload = JSON.parse(atob(token.split('.')[1]));
        if (Date.now() >= payload.exp * 1000) { this.logout(); return; }
        const roleFromToken = this.parseRoleFromToken(payload.role);
        if (roleFromToken) localStorage.setItem('userRole', roleFromToken);
      } catch { this.logout(); return; }

      this.currentUserSubject.next({
        token,
        refreshToken: localStorage.getItem('refreshToken') || '',
        email:        localStorage.getItem('userEmail') || '',
        role:         localStorage.getItem('userRole') || '',
        firstName:    localStorage.getItem('userFirstName') || '',
        lastName:     localStorage.getItem('userLastName') || '',
        id:           parseInt(localStorage.getItem('userId') || '0'),
      });
    }
  }

  private parseRoleFromToken(roleClaim: unknown): string | null {
    if (typeof roleClaim !== 'string' || !roleClaim) return null;
    return roleClaim.startsWith('ROLE_') ? roleClaim.slice(5) : roleClaim;
  }

  login(credentials: LoginRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.apiUrl}/login`, credentials)
      .pipe(tap(res => this.handleAuthResponse(res)));
  }

  /** Register retourne maintenant { message: string } — pas de token */
  register(user: RegisterRequest): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.apiUrl}/register`, user);
  }

  refreshToken(): Observable<AuthResponse> {
    const refreshToken = localStorage.getItem('refreshToken');
    if (!refreshToken) return throwError(() => new Error('No refresh token'));
    return this.http.post<AuthResponse>(
      `${this.apiUrl}/refresh`,
      {},
      { headers: { Authorization: `Bearer ${refreshToken}` } },
    );
  }

  private handleAuthResponse(res: AuthResponse): void {
    localStorage.setItem('token',         res.token);
    localStorage.setItem('refreshToken',  res.refreshToken);
    localStorage.setItem('userEmail',     res.email);
    localStorage.setItem('userRole',      res.role);
    localStorage.setItem('userFirstName', res.firstName);
    localStorage.setItem('userLastName',  res.lastName);
    localStorage.setItem('userId',        res.id.toString());
    this.currentUserSubject.next(res);
  }

  logout(): void {
    ['token','refreshToken','userEmail','userRole','userFirstName','userLastName','userId']
      .forEach(k => localStorage.removeItem(k));
    this.currentUserSubject.next(null);
  }

  isAuthenticated(): boolean {
    const token = localStorage.getItem('token');
    if (!token) return false;
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      if (Date.now() >= payload.exp * 1000) { this.logout(); return false; }
      return true;
    } catch { this.logout(); return false; }
  }

  getToken(): string | null         { return localStorage.getItem('token'); }
  getUserRole(): string | null      { return localStorage.getItem('userRole'); }
  getCurrentUser(): AuthResponse | null { return this.currentUserSubject.value; }

  /** Met à jour le profil en localStorage après modification */
  updateStoredProfile(user: { firstName: string; lastName: string; email: string; role: any }, newToken: string): void {
    localStorage.setItem('token',         newToken);
    localStorage.setItem('userEmail',     user.email);
    localStorage.setItem('userFirstName', user.firstName);
    localStorage.setItem('userLastName',  user.lastName);
    const current = this.currentUserSubject.value;
    if (current) {
      this.currentUserSubject.next({ ...current, token: newToken, email: user.email, firstName: user.firstName, lastName: user.lastName });
    }
  }
}
