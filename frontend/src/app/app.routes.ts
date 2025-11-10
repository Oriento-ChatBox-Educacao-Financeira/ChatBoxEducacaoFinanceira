import { Routes } from '@angular/router';
import { Login } from './pages/login/login';
import { Register } from './pages/register/register';
import { Dashboard } from './pages/dashboard/dashboard';
import { AuthGuard } from './guards/auth.guard';

export const routes: Routes = [
  { path: '', redirectTo: '/login', pathMatch: 'full' },

  { path: 'login', component: Login },
  { path: 'register', component: Register },

  // Rota protegida - só acessível se estiver logado
  { path: 'dashboard', canActivate: [AuthGuard] }, // canActivate: [AuthGuard] },

  // redireciona para login se a rota não for encontrada
  { path: '**', redirectTo: '/login' },
];
