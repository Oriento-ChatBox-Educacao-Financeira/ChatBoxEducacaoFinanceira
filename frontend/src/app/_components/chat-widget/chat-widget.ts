import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MainNavbar } from '../main-navbar/main-navbar';
import { Navbar } from "../navbar/navbar";

@Component({
  selector: 'app-chat-widget',
  imports: [FormsModule, MainNavbar, Navbar],
  templateUrl: './chat-widget.html',
  styleUrl: './chat-widget.css',
})
export class ChatWidget {}
