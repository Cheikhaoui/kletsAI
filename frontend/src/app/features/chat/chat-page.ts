import { Component, ElementRef, OnInit, afterRenderEffect, inject, input, signal, viewChild } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ChatApi } from '../../core/chat-api';
import { errorMessage } from '../../core/errors';
import { ChatMessage, MistakeCategory } from '../../core/models';
import { uuid } from '../../core/uuid';

const CATEGORY_LABELS: Record<MistakeCategory, string> = {
  WORD_ORDER: 'Word order',
  DE_HET: 'de / het',
  VERB_CONJUGATION: 'Verb form',
  SPELLING: 'Spelling',
  VOCABULARY: 'Word choice',
  PREPOSITION: 'Preposition',
  OTHER: 'Grammar',
};

@Component({
  selector: 'app-chat-page',
  imports: [RouterLink],
  templateUrl: './chat-page.html',
  styleUrl: './chat-page.scss',
})
export class ChatPage implements OnInit {
  private readonly api = inject(ChatApi);

  /** Route param, bound by withComponentInputBinding(). */
  readonly id = input.required<string>();

  protected readonly title = signal('');
  protected readonly messages = signal<ChatMessage[]>([]);
  protected readonly draft = signal('');
  protected readonly sending = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly labels = CATEGORY_LABELS;

  private readonly scroller = viewChild<ElementRef<HTMLElement>>('scroller');

  constructor() {
    // Keep the newest message (or the typing dots) in view whenever messages or the sending state change.
    // scrollIntoView({block:'end'}) put the last bubble at the very bottom of the viewport, i.e. hidden
    // behind the sticky composer and tab bar. Scrolling the page to its bottom keeps it just above them.
    afterRenderEffect(() => {
      this.messages();
      this.sending();
      if (!this.scroller()) {
        return;
      }
      // Wait one frame so the cleared textarea has shrunk and the new bubble is laid out.
      requestAnimationFrame(() =>
        window.scrollTo({ top: document.documentElement.scrollHeight, behavior: 'smooth' }),
      );
    });
  }

  ngOnInit(): void {
    this.api.get(this.id()).subscribe({
      next: (c) => {
        this.title.set(c.title);
        this.messages.set(c.messages);
      },
      error: (e) => this.error.set(errorMessage(e, 'Could not load this conversation.')),
    });
  }

  send(): void {
    const text = this.draft().trim();
    if (!text || this.sending()) {
      return;
    }
    // Show the message right away; replace it with the server's version (with corrections) when it arrives.
    const pending: ChatMessage = {
      id: `pending-${uuid()}`, role: 'USER', text, lines: null, corrections: null, createdAt: new Date().toISOString(),
    };
    this.messages.update((m) => [...m, pending]);
    this.draft.set('');
    this.sending.set(true);
    this.error.set(null);

    this.api.send(this.id(), text).subscribe({
      next: (turn) => {
        this.messages.update((m) => [...m.filter((x) => x.id !== pending.id), turn.learner, turn.tutor]);
        this.sending.set(false);
      },
      error: (e) => {
        // Put the text back so it isn't lost.
        this.messages.update((m) => m.filter((x) => x.id !== pending.id));
        this.draft.set(text);
        this.error.set(errorMessage(e, 'The tutor could not answer. Try again.'));
        this.sending.set(false);
      },
    });
  }

  onKeydown(event: KeyboardEvent): void {
    // Enter sends; Shift+Enter makes a new line (useful with a hardware keyboard).
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.send();
    }
  }
}
