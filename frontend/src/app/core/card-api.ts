import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Card, CardContent, CardSource, CardStats, ReviewItem, SyncResponse } from './models';

@Injectable({ providedIn: 'root' })
export class CardApi {
  private readonly http = inject(HttpClient);

  list(source?: CardSource): Observable<Card[]> {
    return this.http.get<Card[]>('/api/cards', { params: source ? { source } : {} });
  }

  get(id: string): Observable<Card> {
    return this.http.get<Card>(`/api/cards/${id}`);
  }

  reviewDeck(days = 0): Observable<Card[]> {
    return this.http.get<Card[]>('/api/cards/review-deck', { params: { days } });
  }

  stats(): Observable<CardStats> {
    return this.http.get<CardStats>('/api/cards/stats');
  }

  create(content: CardContent): Observable<Card> {
    return this.http.post<Card>('/api/cards', content);
  }

  update(id: string, content: CardContent): Observable<Card> {
    return this.http.put<Card>(`/api/cards/${id}`, content);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`/api/cards/${id}`);
  }

  syncReviews(items: ReviewItem[]): Observable<SyncResponse> {
    return this.http.post<SyncResponse>('/api/reviews/sync', items);
  }
}
