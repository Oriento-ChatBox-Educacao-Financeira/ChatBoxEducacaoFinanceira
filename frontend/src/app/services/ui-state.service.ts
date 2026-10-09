import { Injectable, effect, signal } from '@angular/core';

const STORAGE_KEY = 'sidebar-collapsed-v2';

@Injectable({ providedIn: 'root' })
export class UiStateService {
  readonly sidebarCollapsed = signal<boolean>(this.initial());

  constructor() {
    effect(() => {
      const collapsed = this.sidebarCollapsed();
      if (typeof document !== 'undefined') {
        document.body.classList.toggle('sidebar-collapsed', collapsed);
      }
    });
  }

  toggleSidebar(): void {
    const next = !this.sidebarCollapsed();
    this.sidebarCollapsed.set(next);
    try {
      localStorage.setItem(STORAGE_KEY, next ? '1' : '0');
    } catch {}
  }

  private initial(): boolean {
    try {
      const saved = localStorage.getItem(STORAGE_KEY);
      // Default: começa fechada em qualquer sessão nova
      if (saved === null) return true;
      return saved === '1';
    } catch {
      return true;
    }
  }
}
