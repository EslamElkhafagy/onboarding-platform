import { Routes } from '@angular/router';
import { adminGuard, authGuard } from './core/guards';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'chat' },
  {
    path: 'login',
    loadComponent: () => import('./features/login/login').then((m) => m.Login),
  },
  {
    path: 'accept-invite',
    loadComponent: () =>
      import('./features/accept-invite/accept-invite').then((m) => m.AcceptInvite),
  },
  {
    path: 'chat',
    canActivate: [authGuard],
    loadComponent: () => import('./features/chat/chat').then((m) => m.Chat),
  },
  {
    path: 'checklist',
    canActivate: [authGuard],
    loadComponent: () => import('./features/checklist/checklist').then((m) => m.Checklist),
  },
  {
    path: 'help',
    canActivate: [authGuard],
    loadComponent: () => import('./features/help/help').then((m) => m.Help),
  },
  {
    path: 'documents',
    canActivate: [authGuard],
    loadComponent: () => import('./features/documents/documents').then((m) => m.Documents),
  },
  {
    path: 'templates',
    canActivate: [adminGuard],
    loadComponent: () => import('./features/templates/templates').then((m) => m.Templates),
  },
  {
    path: 'dashboard',
    canActivate: [adminGuard],
    loadComponent: () => import('./features/dashboard/dashboard').then((m) => m.Dashboard),
  },
  { path: '**', redirectTo: 'chat' },
];
