import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Topbar } from '../topbar/topbar';
import { Navbar } from '../navbar/navbar';

@Component({
  selector: 'app-main-navbar',
  standalone: true,
  imports: [CommonModule, Topbar, Navbar],
  templateUrl: './main-navbar.html',
  styleUrl: './main-navbar.css',
})
export class MainNavbar {
  nomeUsuario = 'Lucelho Silva';
}
