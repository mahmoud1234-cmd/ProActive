import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  FormBuilder, FormGroup, Validators,
  AbstractControl, ValidationErrors, ReactiveFormsModule,
} from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.css'],
})
export class RegisterComponent implements OnInit {
  registerForm!: FormGroup;
  errorMessage  = '';
  successMessage = '';
  loading = false;
  registered = false; // Affiche l'écran de confirmation

  readonly roles = ['USER', 'MANAGER', 'ADMIN'] as const;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.registerForm = this.fb.group(
      {
        firstName:       ['', [Validators.required, Validators.minLength(2)]],
        lastName:        ['', [Validators.required, Validators.minLength(2)]],
        email:           ['', [Validators.required, Validators.email]],
        password:        ['', [Validators.required, Validators.minLength(6)]],
        confirmPassword: ['', [Validators.required]],
        role:            ['USER', [Validators.required]],
      },
      { validators: this.passwordMatchValidator },
    );

    if (this.authService.isAuthenticated()) {
      this.router.navigate(['/dashboard']);
    }
  }

  passwordMatchValidator(ctrl: AbstractControl): ValidationErrors | null {
    const pw  = ctrl.get('password');
    const cpw = ctrl.get('confirmPassword');
    if (pw && cpw && pw.value !== cpw.value) return { passwordMismatch: true };
    return null;
  }

  onSubmit(): void {
    if (this.registerForm.invalid) {
      Object.keys(this.registerForm.controls).forEach(k =>
        this.registerForm.get(k)?.markAsTouched()
      );
      return;
    }

    this.loading = true;
    this.errorMessage = '';

    const { confirmPassword, ...data } = this.registerForm.value;

    this.authService.register(data).subscribe({
      next: (res) => {
        this.loading = false;
        this.registered = true;
        this.successMessage = res.message;
      },
      error: (err) => {
        this.loading = false;
        this.errorMessage = err.error?.message || "Erreur lors de l'inscription";
      },
    });
  }

  get firstName()       { return this.registerForm.get('firstName')!; }
  get lastName()        { return this.registerForm.get('lastName')!; }
  get email()           { return this.registerForm.get('email')!; }
  get password()        { return this.registerForm.get('password')!; }
  get confirmPassword() { return this.registerForm.get('confirmPassword')!; }
  get role()            { return this.registerForm.get('role')!; }
}
