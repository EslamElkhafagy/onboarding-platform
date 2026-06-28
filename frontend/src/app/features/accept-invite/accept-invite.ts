import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../core/auth';
import { ApiErrorBody } from '../../models';

@Component({
  selector: 'app-accept-invite',
  imports: [FormsModule],
  template: `
    <div class="auth-wrap">
    <span class="auth-brand">Onboarding</span>
    <p class="auth-tagline">AI-powered onboarding for your new hires</p>
    <div class="card auth-card">
      @if (checking()) {
        <p class="muted">Checking your invite…</p>
      } @else if (invalid()) {
        <h1>Invite unavailable</h1>
        <p class="error">{{ invalid() }}</p>
        <p class="switch">
          Already set up? <button class="link-btn" (click)="goLogin()">Sign in</button>
        </p>
      } @else {
        <h1>Set your password</h1>
        <p class="muted">
          Welcome{{ fullName() ? ', ' + fullName() : '' }}! Set a password to join
          <strong>{{ companyName() }}</strong> as <strong>{{ email() }}</strong>.
        </p>

        <form (ngSubmit)="submit()">
          <label>Password<input name="password" type="password" [(ngModel)]="password" required /></label>
          <label>Confirm password<input name="confirm" type="password" [(ngModel)]="confirm" required /></label>

          @if (error()) {
            <p class="error">{{ error() }}</p>
          }

          <button type="submit" [disabled]="loading()">
            {{ loading() ? 'Please wait…' : 'Set password & sign in' }}
          </button>
        </form>
      }
    </div>
    </div>
  `,
})
export class AcceptInvite implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  protected readonly checking = signal(true);
  protected readonly invalid = signal<string | null>(null);
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly email = signal('');
  protected readonly fullName = signal<string | null>(null);
  protected readonly companyName = signal('');

  protected password = '';
  protected confirm = '';

  private token = '';

  ngOnInit(): void {
    this.token = this.route.snapshot.queryParamMap.get('token') ?? '';
    if (!this.token) {
      this.checking.set(false);
      this.invalid.set('This invite link is missing its token.');
      return;
    }
    this.auth.previewInvite(this.token).subscribe({
      next: (p) => {
        this.email.set(p.email);
        this.fullName.set(p.fullName);
        this.companyName.set(p.companyName);
        this.checking.set(false);
      },
      error: (err) => {
        this.checking.set(false);
        this.invalid.set(extractError(err));
      },
    });
  }

  submit(): void {
    if (this.password.length < 8) {
      this.error.set('Password must be at least 8 characters.');
      return;
    }
    if (this.password !== this.confirm) {
      this.error.set('Passwords do not match.');
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    this.auth.setPassword(this.token, this.password).subscribe({
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

  goLogin(): void {
    this.router.navigate(['/login']);
  }
}

function extractError(err: unknown): string {
  const body = (err as { error?: ApiErrorBody })?.error;
  return body?.error?.message ?? 'Something went wrong. Please try again.';
}
