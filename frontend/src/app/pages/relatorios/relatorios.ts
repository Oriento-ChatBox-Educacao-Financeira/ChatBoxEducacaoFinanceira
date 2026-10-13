import { Component } from '@angular/core';
import { MainNavbar } from '../../_components/main-navbar/main-navbar';
import { WorkInProgress } from '../../_components/work-in-progress/work-in-progress';

@Component({
  selector: 'app-relatorios-page',
  standalone: true,
  imports: [MainNavbar, WorkInProgress],
  templateUrl: './relatorios.html',
})
export class RelatoriosPage {}
