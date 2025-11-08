import { Component, EventEmitter, Output } from '@angular/core';

@Component({
  selector: 'app-topbar',
  imports: [],
  templateUrl: './topbar.html',
  styleUrl: './topbar.css',
})
export class Topbar {
  @Output() openMenuClick = new EventEmitter<void>();

  constructor() {}

  onMenuButtonClick() {
    this.openMenuClick.emit();
  }
}
