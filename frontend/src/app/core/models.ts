export type CardType = 'WORD' | 'SENTENCE';
export type Article = 'DE' | 'HET';
export type CardSource = 'MANUAL' | 'SEED' | 'MISTAKE';

export interface CardContent {
  cardType: CardType;
  dutch: string;
  english: string;
  article: Article | null;
  exampleNl: string | null;
  exampleEn: string | null;
}

export interface Card extends CardContent {
  id: string;
  source: CardSource;
  mistakeId: string | null;
  easeFactor: number;
  intervalDays: number;
  repetitions: number;
  dueAt: string;
  lastReviewedAt: string | null;
}

export interface CardStats {
  dueNow: number;
  total: number;
  reviewedToday: number;
}

/** Same codes as the backend Grade enum. */
export const Grade = { AGAIN: 0, HARD: 1, GOOD: 2, EASY: 3 } as const;
export type Grade = (typeof Grade)[keyof typeof Grade];

export interface ReviewItem {
  /** Generated on the phone so a retried sync is never counted twice. */
  id: string;
  cardId: string;
  grade: Grade;
  reviewedAt: string;
}

export interface SyncResponse {
  applied: string[];
  alreadySynced: string[];
  unknownCards: string[];
  cards: Card[];
}
