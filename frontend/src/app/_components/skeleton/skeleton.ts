import { Component, Input, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';

export type SkeletonVariant =
  | 'text'
  | 'rect'
  | 'circle'
  | 'kpi'
  | 'chart'
  | 'list-item'
  | 'card';

@Component({
  selector: 'app-skeleton',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './skeleton.html',
  styleUrl: './skeleton.css',
})
export class Skeleton {
  @Input() variant: SkeletonVariant = 'rect';
  @Input() width?: string;
  @Input() height?: string;
  @Input() radius?: string;

  private readonly _count = signal(1);
  @Input() set count(value: number | string | null | undefined) {
    const n = typeof value === 'string' ? parseInt(value, 10) : value;
    this._count.set(Number.isFinite(n) && (n as number) > 0 ? (n as number) : 1);
  }

  readonly itens = computed(() => Array.from({ length: this._count() }));

  get inlineStyles(): Record<string, string> {
    const styles: Record<string, string> = {};
    if (this.width) styles['width'] = this.width;
    if (this.height) styles['height'] = this.height;
    if (this.radius) styles['border-radius'] = this.radius;
    return styles;
  }
}
