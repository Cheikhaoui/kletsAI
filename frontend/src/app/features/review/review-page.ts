import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CardApi } from '../../core/card-api';
import { Grade } from '../../core/models';
import { ReviewSync } from '../../core/review-sync';
import { uuid } from '../../core/uuid';
import { ReviewSession } from './review-session';

@Component({
  selector: 'app-review-page',
  imports: [RouterLink],
  templateUrl: './review-page.html',
  styleUrl: './review-page.scss',
})
export class ReviewPage {
  private readonly api = inject(CardApi);
  protected readonly sync = inject(ReviewSync);

  protected readonly Grade = Grade;
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly revealed = signal(false);

  private readonly session = signal<ReviewSession | null>(null);
  /** ReviewSession is a plain object; bump this after each change so the view updates. */
  private readonly version = signal(0);

  protected readonly card = computed(() => (this.version(), this.session()?.current));
  protected readonly done = computed(() => (this.version(), this.session()?.done ?? false));
  protected readonly progress = computed(() => {
    this.version();
    const s = this.session();
    return s ? { finished: s.finished, total: s.total, reviewed: s.reviewedCount } : null;
  });

  constructor() {
    this.api.reviewDeck(0).subscribe({
      next: (cards) => {
        this.session.set(new ReviewSession(cards, uuid));
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Could not load your cards. Is the backend running?');
        this.loading.set(false);
      },
    });
  }

  reveal(): void {
    this.revealed.set(true);
  }

  grade(grade: Grade): void {
    const session = this.session();
    if (!session || !this.revealed()) {
      return;
    }
    this.sync.enqueue(session.grade(grade));
    this.revealed.set(false);
    this.version.update((v) => v + 1);
  }
}
