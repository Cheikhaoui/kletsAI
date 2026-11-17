import { Injectable, computed, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { CardApi } from './card-api';
import { ReviewItem } from './models';

/**
 * Queues graded reviews and sends them to the server.
 * Step 2: the queue lives in memory and is retried on the next grade or when the browser comes back online.
 * Step 5 (PWA) will persist it in IndexedDB so it survives closing the app.
 */
@Injectable({ providedIn: 'root' })
export class ReviewSync {
  private readonly api = inject(CardApi);
  private readonly queue = signal<ReviewItem[]>([]);
  private flushing = false;

  readonly pendingCount = computed(() => this.queue().length);

  constructor() {
    window.addEventListener('online', () => void this.flush());
  }

  enqueue(item: ReviewItem): void {
    this.queue.update((q) => [...q, item]);
    void this.flush();
  }

  async flush(): Promise<void> {
    if (this.flushing || this.queue().length === 0) {
      return;
    }
    this.flushing = true;
    const batch = this.queue();
    try {
      const res = await firstValueFrom(this.api.syncReviews(batch));
      // Everything the server accepted, had already seen, or can't apply (card deleted) is done.
      const done = new Set([...res.applied, ...res.alreadySynced, ...res.unknownCards]);
      this.queue.update((q) => q.filter((i) => !done.has(i.id)));
    } catch {
      // Offline or server down: keep the queue, try again later.
    } finally {
      this.flushing = false;
    }
    // Items graded while this request was in flight.
    if (this.queue().some((i) => !batch.includes(i))) {
      void this.flush();
    }
  }
}
