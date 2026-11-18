import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { ChatApi } from '../../core/chat-api';
import { errorMessage } from '../../core/errors';
import { ConversationSummary, Scenario, ScenarioId } from '../../core/models';

/** Pick a role-play scene, or continue a recent conversation. */
@Component({
  selector: 'app-chat-home-page',
  imports: [RouterLink, DatePipe],
  template: `
    <h1>Praten</h1>
    <p class="muted">Practise speaking. Pick a scene:</p>

    <div class="scenarios">
      @for (s of scenarios(); track s.id) {
        <button type="button" class="scenario" (click)="start(s.id)" [disabled]="starting() !== null">
          <strong>{{ s.titleNl }}</strong>
          <span class="muted">{{ s.titleEn }}</span>
          <span class="small muted">{{ s.description }}</span>
          @if (starting() === s.id) {
            <span class="small">Starting…</span>
          }
        </button>
      }
    </div>

    @if (error(); as e) {
      <p class="error">{{ e }}</p>
    }

    @if (recent().length > 0) {
      <h2>Recent</h2>
      <ul class="recent">
        @for (c of recent(); track c.id) {
          <li>
            <a [routerLink]="['/chat', c.id]">
              <span>{{ c.title }}</span>
              <span class="small muted">{{ c.createdAt | date: 'EEE d MMM, HH:mm' }}</span>
            </a>
          </li>
        }
      </ul>
    }
  `,
  styles: `
    .scenarios { display: grid; gap: 10px; }
    .scenario {
      display: flex; flex-direction: column; align-items: flex-start; gap: 2px; text-align: left;
      padding: 14px 16px; min-height: 72px; border-radius: var(--radius);
      border: 1px solid var(--border); background: var(--surface); color: var(--text); font: inherit;
      &:active { transform: scale(0.99); }
      &:disabled { opacity: 0.6; }
      strong { font-size: 1.1rem; }
    }
    h2 { font-size: 1.1rem; margin: 24px 0 4px; }
    .recent {
      list-style: none; padding: 0; margin: 0;
      a {
        display: flex; justify-content: space-between; align-items: center; gap: 8px;
        min-height: 52px; border-bottom: 1px solid var(--border); color: inherit; text-decoration: none;
      }
    }
    .error { color: var(--again); }
  `,
})
export class ChatHomePage {
  private readonly api = inject(ChatApi);
  private readonly router = inject(Router);

  protected readonly scenarios = signal<Scenario[]>([]);
  protected readonly recent = signal<ConversationSummary[]>([]);
  protected readonly starting = signal<ScenarioId | null>(null);
  protected readonly error = signal<string | null>(null);

  constructor() {
    this.api.scenarios().subscribe((s) => this.scenarios.set(s));
    this.api.conversations().subscribe((c) => this.recent.set(c));
  }

  start(id: ScenarioId): void {
    this.starting.set(id);
    this.error.set(null);
    this.api.start(id).subscribe({
      next: (c) => this.router.navigate(['/chat', c.id]),
      error: (e) => {
        this.error.set(errorMessage(e, 'Could not start the conversation.'));
        this.starting.set(null);
      },
    });
  }
}
