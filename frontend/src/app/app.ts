import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { MainNavbar } from "./_components/main-navbar/main-navbar";

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, MainNavbar],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  protected readonly title = signal('frontend');
}
