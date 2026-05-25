import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MainNavbar } from '../../_components/main-navbar/main-navbar';
import { AuthService } from '../../services/auth.service';
import { Usuario } from '../../models/usuario.model';
import { Observable } from 'rxjs';

@Component({
  selector: 'app-perfil',
  standalone: true,
  imports: [CommonModule, MainNavbar],
  templateUrl: './perfil.html',
  styleUrl: './perfil.css',
})
export class PerfilPage {
  user$: Observable<Usuario | null>;

  constructor(private auth: AuthService) {
    this.user$ = this.auth.user$;
  }
}
