import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { RouterModule, Router } from '@angular/router';

import { AuthService } from '../services/auth.service';
import { ProfileService } from '../services/profile.service';
import { UserSummary } from '../services/user.service';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.css'],
})
export class ProfileComponent implements OnInit {

  profile: UserSummary | null = null;
  loading = true;

  infoForm!:     FormGroup;
  passwordForm!: FormGroup;

  savingInfo   = false;
  savingPwd    = false;
  infoSuccess  = '';
  infoError    = '';
  pwdSuccess   = '';
  pwdError     = '';

  showCurrentPwd = false;
  showNewPwd     = false;
  showConfirmPwd = false;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private profileService: ProfileService,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.infoForm = this.fb.group({
      firstName: ['', [Validators.required, Validators.minLength(2)]],
      lastName:  ['', [Validators.required, Validators.minLength(2)]],
      email:     ['', [Validators.required, Validators.email]],
    });

    this.passwordForm = this.fb.group({
      currentPassword: ['', Validators.required],
      newPassword:     ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', Validators.required],
    }, { validators: this.passwordMatchValidator });

    this.loadProfile();
  }

  passwordMatchValidator(ctrl: AbstractControl): ValidationErrors | null {
    const np = ctrl.get('newPassword');
    const cp = ctrl.get('confirmPassword');
    if (np && cp && np.value && cp.value && np.value !== cp.value)
      return { mismatch: true };
    return null;
  }

  loadProfile(): void {
    this.loading = true;
    this.profileService.getProfile().subscribe({
      next: (p) => {
        this.profile = p;
        this.infoForm.patchValue({
          firstName: p.firstName,
          lastName:  p.lastName,
          email:     p.email,
        });
        this.loading = false;
      },
      error: () => { this.loading = false; },
    });
  }

  // ── Infos ─────────────────────────────────────────────────────
  saveInfo(): void {
    if (this.infoForm.invalid) { this.infoForm.markAllAsTouched(); return; }
    this.savingInfo = true;
    this.infoSuccess = ''; this.infoError = '';

    this.profileService.updateProfile(this.infoForm.value).subscribe({
      next: (res) => {
        this.savingInfo = false;
        this.profile = res.user;
        // Mettre à jour le token et le localStorage
        this.authService.updateStoredProfile(res.user, res.token);
        this.infoSuccess = 'Informations mises à jour.';
        setTimeout(() => this.infoSuccess = '', 4000);
      },
      error: (err) => {
        this.savingInfo = false;
        this.infoError = err.error?.message || 'Erreur lors de la mise à jour.';
      },
    });
  }

  // ── Mot de passe ──────────────────────────────────────────────
  savePwd(): void {
    if (this.passwordForm.invalid) { this.passwordForm.markAllAsTouched(); return; }
    this.savingPwd = true;
    this.pwdSuccess = ''; this.pwdError = '';

    const { currentPassword, newPassword } = this.passwordForm.value;

    this.profileService.updateProfile({ currentPassword, newPassword }).subscribe({
      next: (res) => {
        this.savingPwd = false;
        this.authService.updateStoredProfile(res.user, res.token);
        this.pwdSuccess = 'Mot de passe modifié avec succès.';
        this.passwordForm.reset();
        setTimeout(() => this.pwdSuccess = '', 4000);
      },
      error: (err) => {
        this.savingPwd = false;
        this.pwdError = err.error?.message || 'Erreur lors du changement de mot de passe.';
      },
    });
  }

  // ── Auth ──────────────────────────────────────────────────────
  get userName(): string {
    const u = this.authService.getCurrentUser();
    return u ? `${u.firstName} ${u.lastName}`.trim() : '';
  }
  get userRole(): string { return this.authService.getCurrentUser()?.role ?? 'USER'; }

  initials(): string {
    if (!this.profile) return '?';
    return `${this.profile.firstName[0] ?? ''}${this.profile.lastName[0] ?? ''}`.toUpperCase();
  }

  getRoleClass(role: string): string {
    return ({ ADMIN: 'role-admin', MANAGER: 'role-manager', USER: 'role-user' } as any)[role] ?? 'role-user';
  }

  logout(): void { this.authService.logout(); this.router.navigate(['/login']); }

  // Helpers form
  f(name: string) { return this.infoForm.get(name)!; }
  p(name: string) { return this.passwordForm.get(name)!; }

  // Force mot de passe
  getStrengthPct(pwd: string): string {
    if (!pwd) return '0%';
    let score = 0;
    if (pwd.length >= 6)  score++;
    if (pwd.length >= 10) score++;
    if (/[A-Z]/.test(pwd)) score++;
    if (/[0-9]/.test(pwd)) score++;
    if (/[^A-Za-z0-9]/.test(pwd)) score++;
    return `${(score / 5) * 100}%`;
  }

  getStrengthClass(pwd: string): string {
    if (!pwd) return '';
    let score = 0;
    if (pwd.length >= 6)  score++;
    if (pwd.length >= 10) score++;
    if (/[A-Z]/.test(pwd)) score++;
    if (/[0-9]/.test(pwd)) score++;
    if (/[^A-Za-z0-9]/.test(pwd)) score++;
    if (score <= 2) return 'strength-weak';
    if (score <= 3) return 'strength-medium';
    return 'strength-strong';
  }

  getStrengthLabel(pwd: string): string {
    const cls = this.getStrengthClass(pwd);
    if (cls === 'strength-weak')   return 'Faible';
    if (cls === 'strength-medium') return 'Moyen';
    return 'Fort';
  }
}
