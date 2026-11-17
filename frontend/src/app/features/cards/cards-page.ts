import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CardApi } from '../../core/card-api';
import { Card } from '../../core/models';

@Component({
  selector: 'app-cards-page',
  imports: [RouterLink],
  template: `
    <div class="header">
      <h1>Cards</h1>
      <a routerLink="/cards/new" class="btn btn-primary small-btn">+ Add</a>
    </div>

    <input type="search" class="search" placeholder="Search Dutch or English"
           [value]="query()" (input)="query.set($any($event.target).value)" />

    @if (loading()) {
      <p class="muted">Loading…</p>
    } @else {
      <p class="muted small">{{ filtered().length }} cards</p>
      <ul class="list">
        @for (c of filtered(); track c.id) {
          <li>
            <a [routerLink]="['/cards', c.id]" class="row">
              <span class="nl">
                @if (c.article) { <span class="article" [class.het]="c.article === 'HET'">{{ c.article.toLowerCase() }}</span> }
                {{ c.dutch }}
              </span>
              <span class="en muted">{{ c.english }}</span>
              @if (c.source === 'MISTAKE') { <span class="tag">mistake</span> }
            </a>
          </li>
        }
      </ul>
    }
  `,
  styles: `
    .header { display: flex; justify-content: space-between; align-items: center; }
    .small-btn { min-height: 44px; padding: 0 16px; }
    .search { width: 100%; margin: 8px 0; }
    .list { list-style: none; padding: 0; margin: 0; }
    .row {
      display: flex; flex-wrap: wrap; align-items: baseline; gap: 4px 10px;
      padding: 14px 4px; min-height: 48px; border-bottom: 1px solid var(--border);
      color: inherit; text-decoration: none;
    }
    .nl { font-weight: 600; font-size: 1.05rem; }
    .en { flex: 1; text-align: right; }
    .article { color: var(--de); font-weight: 500; &.het { color: var(--het); } }
    .tag { font-size: 0.7rem; padding: 2px 6px; border-radius: 999px; background: var(--surface-2); }
  `,
})
export class CardsPage {
  private readonly api = inject(CardApi);
  protected readonly cards = signal<Card[]>([]);
  protected readonly loading = signal(true);
  protected readonly query = signal('');

  protected readonly filtered = computed(() => {
    const q = this.query().trim().toLowerCase();
    return q
      ? this.cards().filter((c) => c.dutch.toLowerCase().includes(q) || c.english.toLowerCase().includes(q))
      : this.cards();
  });

  constructor() {
    this.api.list().subscribe({
      next: (cards) => {
        this.cards.set(cards);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }
}
