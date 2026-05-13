import { Routes } from '@angular/router';
import { AuthGuard } from './guards/auth.guard';

export const routes: Routes = [
  { path: '', redirectTo: '/login', pathMatch: 'full' },

  {
    path: 'login',
    loadComponent: () => import('./pages/login/login').then((m) => m.Login),
  },
  {
    path: 'register',
    loadComponent: () => import('./pages/register/register').then((m) => m.Register),
  },
  {
    path: 'register/credenciais',
    loadComponent: () =>
      import('./pages/register-confirm/register-confirm').then((m) => m.RegisterConfirm),
  },
  {
    path: 'register/sucesso',
    loadComponent: () =>
      import('./pages/register-success/register-success').then((m) => m.RegisterSuccess),
  },

  {
    path: 'dashboard',
    loadComponent: () => import('./pages/dashboard/dashboard').then((m) => m.Dashboard),
    canActivate: [AuthGuard],
  },

  { path: '**', redirectTo: '/login' },
];
