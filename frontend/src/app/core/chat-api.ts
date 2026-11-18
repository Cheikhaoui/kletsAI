import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API } from './api-url';
import { ConversationDetail, ConversationSummary, Scenario, ScenarioId, TurnResult } from './models';

@Injectable({ providedIn: 'root' })
export class ChatApi {
  private readonly http = inject(HttpClient);

  scenarios(): Observable<Scenario[]> {
    return this.http.get<Scenario[]>(`${API}/scenarios`);
  }

  conversations(): Observable<ConversationSummary[]> {
    return this.http.get<ConversationSummary[]>(`${API}/conversations`);
  }

  start(scenario: ScenarioId): Observable<ConversationDetail> {
    return this.http.post<ConversationDetail>(`${API}/conversations`, { scenario });
  }

  get(id: string): Observable<ConversationDetail> {
    return this.http.get<ConversationDetail>(`${API}/conversations/${id}`);
  }

  send(id: string, text: string): Observable<TurnResult> {
    return this.http.post<TurnResult>(`${API}/conversations/${id}/messages`, { text });
  }
}
