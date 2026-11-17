import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

/** App shell: page content on top, big tab bar at the bottom (thumb reach on a phone). */
@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <main class="page">
      <router-outlet />
    </main>
    <nav class="tabs" aria-label="Main">
      <a routerLink="/" routerLinkActive="active" [routerLinkActiveOptions]="{ exact: true }">Home</a>
      <a routerLink="/review" routerLinkActive="active">Review</a>
      <a routerLink="/cards" routerLinkActive="active">Cards</a>
    </nav>
  `,
  styles: `
    :host { display: flex; flex-direction: column; min-height: 100dvh; }
    .page {
      flex: 1; display: flex; flex-direction: column;
      width: 100%; max-width: 560px; margin: 0 auto; padding: 16px 16px 24px;
    }
    .tabs {
      position: sticky; bottom: 0; display: grid; grid-template-columns: repeat(3, 1fr);
      background: var(--surface); border-top: 1px solid var(--border);
      padding-bottom: env(safe-area-inset-bottom);
      a {
        display: flex; align-items: center; justify-content: center; min-height: 56px;
        color: var(--text-muted); text-decoration: none; font-weight: 600;
        &.active { color: var(--accent); }
      }
    }
  `,
})
export class App {}
