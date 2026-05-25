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

  { path: 'dashboard', redirectTo: '/dashboard/visao-geral', pathMatch: 'full' },

  {
    path: 'dashboard/visao-geral',
    loadComponent: () =>
      import('./pages/dashboard-visao-geral/dashboard-visao-geral').then(
        (m) => m.DashboardVisaoGeral,
      ),
    // canActivate: [AuthGuard],
  },

  {
    path: 'dashboard/dre',
    loadComponent: () => import('./pages/dashboard-dre/dashboard-dre').then((m) => m.DashboardDre),
    // canActivate: [AuthGuard],
  },

  {
    path: 'dashboard/fluxo-caixa',
    loadComponent: () =>
      import('./pages/dashboard-fluxo-de-caixa/dashboard-fluxo-de-caixa').then(
        (m) => m.DashboardFluxoCaixa,
      ),
    // canActivate: [AuthGuard],
  },

  {
    path: 'dashboard/balanco-patrimonial',
    loadComponent: () =>
      import('./pages/dashboard-balanco/dashboard-balanco').then(
        (m) => m.DashboardBalancoPatrimonial,
      ),
    // canActivate: [AuthGuard],
  },

  {
    path: 'chat',
    loadComponent: () => import('./pages/chat/chat').then((m) => m.ChatPageComponent),
    // canActivate: [AuthGuard],
  },

  {
    path: 'upload-planilha',
    loadComponent: () =>
      import('./pages/upload-planilha/upload-planilha').then((m) => m.UploadPlanilhaComponent),
    // canActivate: [AuthGuard],
  },

  {
    path: 'configuracoes',
    loadComponent: () =>
      import('./pages/configuracoes/configuracoes').then((m) => m.ConfiguracoesPage),
  },

  {
    path: 'perfil',
    loadComponent: () => import('./pages/perfil/perfil').then((m) => m.PerfilPage),
  },

  { path: '**', redirectTo: '/login' },
];
