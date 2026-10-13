import { Component } from '@angular/core';
import { MainNavbar } from '../../_components/main-navbar/main-navbar';
import { WorkInProgress } from '../../_components/work-in-progress/work-in-progress';

@Component({
  selector: 'app-suporte-page',
  standalone: true,
  imports: [MainNavbar, WorkInProgress],
  templateUrl: './suporte.html',
})
export class SuportePage {}
