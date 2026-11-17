import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CardApi } from '../../core/card-api';
import { CardStats } from '../../core/models';
import { ReviewSync } from '../../core/review-sync';

@Component({
  selector: 'app-home-page',
  imports: [RouterLink],
  template: `
    <h1>Goedemorgen!</h1>
    <p class="muted">Good morning! Ready for some Dutch?</p>

    @if (stats(); as s) {
      <div class="stats">
        <div class="stat"><strong>{{ s.dueNow }}</strong><span>due now</span></div>
        <div class="stat"><strong>{{ s.reviewedToday }}</strong><span>reviewed today</span></div>
        <div class="stat"><strong>{{ s.total }}</strong><span>cards</span></div>
      </div>
    } @else if (offline()) {
      <p class="muted small">Can't reach the server right now.</p>
    }

    @if (sync.pendingCount() > 0) {
      <p class="muted small">{{ sync.pendingCount() }} reviews waiting to sync</p>
    }

    <div class="stack">
      <a routerLink="/review" class="btn btn-primary">Review cards</a>
      <a routerLink="/cards/new" class="btn">Add a card</a>
    </div>
  `,
  styles: `
    h1 { margin-bottom: 4px; }
    .stats { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; margin: 20px 0; }
    .stat {
      display: flex; flex-direction: column; align-items: center; padding: 14px 4px;
      background: var(--surface); border: 1px solid var(--border); border-radius: var(--radius);
      strong { font-size: 1.6rem; }
      span { font-size: 0.8rem; color: var(--text-muted); text-align: center; }
    }
    .stack { display: flex; flex-direction: column; gap: 12px; margin-top: 24px; }
  `,
})
export class HomePage {
  private readonly api = inject(CardApi);
  protected readonly sync = inject(ReviewSync);
  protected readonly stats = signal<CardStats | null>(null);
  protected readonly offline = signal(false);

  constructor() {
    this.api.stats().subscribe({
      next: (s) => this.stats.set(s),
      error: () => this.offline.set(true),
    });
  }
}
