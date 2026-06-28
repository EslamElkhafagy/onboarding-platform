import { Component, computed, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from './core/auth';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  private readonly router = inject(Router);
  protected readonly auth = inject(AuthService);

  protected readonly initial = computed(() => {
    const u = this.auth.user();
    return (u?.fullName || u?.email || '?').trim().charAt(0).toUpperCase();
  });

  logout(): void {
    this.auth.logout();
    this.router.navigate(['/login']);
  }
}
