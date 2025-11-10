import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { PrimaryButton } from './_components/primary-button/primary-button';
import { Login } from './pages/login/login';
import { Register } from './pages/register/register';
import { Navbar } from './_components/navbar/navbar';
import { Topbar } from './_components/topbar/topbar';
import { MainNavbar } from './_components/main-navbar/main-navbar';
import { ChatWidget } from './_components/chat-widget/chat-widget';
import { Dashboard } from "./pages/dashboard/dashboard";

@Component({
  selector: 'app-root',
  imports: [Login, Register, MainNavbar, ChatWidget, RouterOutlet, Dashboard],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  protected readonly title = signal('frontend');
}
