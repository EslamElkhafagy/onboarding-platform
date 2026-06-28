import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth';
import { ApiErrorBody } from '../../models';

@Component({
  selector: 'app-login',
  imports: [FormsModule],
  template: `
    <div class="auth-wrap">
    <span class="auth-brand">Onboarding</span>
    <p class="auth-tagline">AI-powered onboarding for your new hires</p>
    <div class="card auth-card">
      <h1>{{ mode() === 'login' ? 'Sign in' : 'Create your company' }}</h1>

      <form (ngSubmit)="submit()">
        @if (mode() === 'register') {
          <label>Company name<input name="company" [(ngModel)]="companyName" required /></label>
          <label>Your name<input name="fullName" [(ngModel)]="fullName" required /></label>
        }
        <label>Email<input name="email" type="email" [(ngModel)]="email" required /></label>
        <label>Password<input name="password" type="password" [(ngModel)]="password" required /></label>

        @if (error()) {
          <p class="error">{{ error() }}</p>
        }

        <button type="submit" [disabled]="loading()">
          {{ loading() ? 'Please wait…' : mode() === 'login' ? 'Sign in' : 'Create account' }}
        </button>
      </form>

      <p class="switch">
        @if (mode() === 'login') {
          New company? <button class="link-btn" (click)="toggle()">Register</button>
        } @else {
          Already have an account? <button class="link-btn" (click)="toggle()">Sign in</button>
        }
      </p>
    </div>
    </div>
  `,
})
export class Login {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly mode = signal<'login' | 'register'>('login');
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);

  protected companyName = '';
  protected fullName = '';
  protected email = '';
  protected password = '';

  toggle(): void {
    this.error.set(null);
    this.mode.update((m) => (m === 'login' ? 'register' : 'login'));
  }

  submit(): void {
    this.loading.set(true);
    this.error.set(null);
    const request$ =
      this.mode() === 'login'
        ? this.auth.login(this.email, this.password)
        : this.auth.register(this.companyName, this.email, this.password, this.fullName);

    request$.subscribe({
      next: () => {
        this.loading.set(false);
        this.router.navigate(['/chat']);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(extractError(err));
      },
    });
  }
}

function extractError(err: unknown): string {
  const body = (err as { error?: ApiErrorBody })?.error;
  return body?.error?.message ?? 'Something went wrong. Please try again.';
}
