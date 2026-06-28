import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { AuthResponse, InvitePreview, User } from '../models';

const TOKEN_KEY = 'onboarding.token';
const USER_KEY = 'onboarding.user';

/**
 * Holds the authenticated session. Token + user are persisted to localStorage so a refresh
 * keeps the user logged in; the token is attached to API calls by the auth interceptor.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);

  private readonly userSignal = signal<User | null>(readUser());

  readonly user = this.userSignal.asReadonly();
  readonly isLoggedIn = computed(() => this.userSignal() !== null);
  readonly isAdmin = computed(() => this.userSignal()?.role === 'ADMIN');

  get token(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  login(email: string, password: string): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>('/api/auth/login', { email, password })
      .pipe(tap((res) => this.setSession(res)));
  }

  register(companyName: string, email: string, password: string, fullName: string): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>('/api/auth/register', { companyName, email, password, fullName })
      .pipe(tap((res) => this.setSession(res)));
  }

  /** Public: look up an invite by its token so the accept screen can greet the hire. */
  previewInvite(token: string): Observable<InvitePreview> {
    return this.http.get<InvitePreview>(`/api/auth/invitations/${encodeURIComponent(token)}`);
  }

  /** Public: a hire exchanges their invite token for a password and is logged straight in. */
  setPassword(token: string, password: string): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>('/api/auth/set-password', { token, password })
      .pipe(tap((res) => this.setSession(res)));
  }

  logout(): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    this.userSignal.set(null);
  }

  private setSession(res: AuthResponse): void {
    localStorage.setItem(TOKEN_KEY, res.token);
    localStorage.setItem(USER_KEY, JSON.stringify(res.user));
    this.userSignal.set(res.user);
  }
}

function readUser(): User | null {
  const raw = localStorage.getItem(USER_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as User;
  } catch {
    return null;
  }
}
