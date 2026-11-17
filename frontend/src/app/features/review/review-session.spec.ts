import { Card, Grade } from '../../core/models';
import { AGAIN_GAP, ReviewSession } from './review-session';

function card(id: string): Card {
  return {
    id, cardType: 'WORD', dutch: id, english: id, article: null, exampleNl: null, exampleEn: null,
    source: 'SEED', mistakeId: null, easeFactor: 2.5, intervalDays: 0, repetitions: 0,
    dueAt: '2026-11-18T00:00:00Z', lastReviewedAt: null,
  };
}

describe('ReviewSession', () => {
  let n = 0;
  const ids = () => `review-${++n}`;

  it('walks through cards and finishes', () => {
    const s = new ReviewSession([card('a'), card('b')], ids);
    expect(s.current?.id).toBe('a');
    s.grade(Grade.GOOD);
    expect(s.current?.id).toBe('b');
    s.grade(Grade.EASY);
    expect(s.done).toBe(true);
    expect(s.finished).toBe(2);
  });

  it(`brings an Again card back after ${AGAIN_GAP} other cards`, () => {
    const s = new ReviewSession(['a', 'b', 'c', 'd', 'e'].map(card), ids);
    s.grade(Grade.AGAIN); // a
    const order = [];
    while (!s.done) {
      order.push(s.current!.id);
      s.grade(Grade.GOOD);
    }
    expect(order).toEqual(['b', 'c', 'd', 'a', 'e']);
  });

  it('puts an Again card at the end when few cards are left', () => {
    const s = new ReviewSession([card('a'), card('b')], ids);
    s.grade(Grade.AGAIN);
    expect(s.current?.id).toBe('b');
    s.grade(Grade.GOOD);
    expect(s.current?.id).toBe('a');
    expect(s.total).toBe(2);
  });

  it('creates a review item with a unique id and the grade time', () => {
    const s = new ReviewSession([card('a')], () => 'fixed-id');
    const at = new Date('2026-11-18T07:15:00Z');
    expect(s.grade(Grade.HARD, at)).toEqual({
      id: 'fixed-id', cardId: 'a', grade: Grade.HARD, reviewedAt: '2026-11-18T07:15:00.000Z',
    });
  });
});
