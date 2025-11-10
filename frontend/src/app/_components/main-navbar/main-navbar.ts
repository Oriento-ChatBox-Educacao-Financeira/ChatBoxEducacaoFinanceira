import { Component } from '@angular/core';
import { Topbar } from '../topbar/topbar';
import { Navbar } from '../navbar/navbar';

@Component({
  selector: 'app-main-navbar',
  imports: [Topbar, Navbar],
  templateUrl: './main-navbar.html',
  styleUrl: './main-navbar.css',
})
export class MainNavbar {}
