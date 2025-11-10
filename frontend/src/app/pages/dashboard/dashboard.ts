import { Component } from '@angular/core';
import { ChatWidget } from '../../_components/chat-widget/chat-widget';
// import { MainNavbar } from '../../_components/main-navbar/main-navbar';

@Component({
  selector: 'app-dashboard',
  imports: [ChatWidget],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
})
export class Dashboard {}
