import { Card, Grade, ReviewItem } from '../../core/models';

/** How many other cards to show before an "Again" card comes back. */
export const AGAIN_GAP = 3;

/**
 * The state of one review session, with no Angular code so it is easy to test.
 * "Again" cards come back a few cards later in the same session; the server's SM-2 decides the real schedule.
 */
export class ReviewSession {
  private readonly queue: Card[];
  private finishedIds = new Set<string>();
  reviewedCount = 0;

  constructor(cards: Card[], private readonly newId: () => string = () => crypto.randomUUID()) {
    this.queue = [...cards];
  }

  get current(): Card | undefined {
    return this.queue[0];
  }

  get done(): boolean {
    return this.queue.length === 0;
  }

  /** Distinct cards in this session. */
  get total(): number {
    return new Set([...this.queue.map((c) => c.id), ...this.finishedIds]).size;
  }

  /** Cards answered with something other than Again. */
  get finished(): number {
    return this.finishedIds.size;
  }

  grade(grade: Grade, now: Date = new Date()): ReviewItem {
    const card = this.queue.shift();
    if (!card) {
      throw new Error('No card to grade');
    }
    this.reviewedCount++;
    if (grade === Grade.AGAIN) {
      this.queue.splice(Math.min(AGAIN_GAP, this.queue.length), 0, card);
    } else {
      this.finishedIds.add(card.id);
    }
    return { id: this.newId(), cardId: card.id, grade, reviewedAt: now.toISOString() };
  }
}
