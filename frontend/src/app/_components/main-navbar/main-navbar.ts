import { Component } from '@angular/core';
import { CommonModule, AsyncPipe } from '@angular/common';
import { Observable, map } from 'rxjs';
import { Topbar } from '../topbar/topbar';
import { Navbar } from '../navbar/navbar';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-main-navbar',
  standalone: true,
  imports: [CommonModule, Topbar, Navbar, AsyncPipe],
  templateUrl: './main-navbar.html',
  styleUrl: './main-navbar.css',
})
export class MainNavbar {
  nomeUsuario$: Observable<string>;

  constructor(private auth: AuthService) {
    this.nomeUsuario$ = this.auth.user$.pipe(
      map((user) => user?.nome ?? 'Convidado'),
    );
  }
}
