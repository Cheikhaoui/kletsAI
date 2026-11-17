import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, input, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { CardApi } from '../../core/card-api';
import { Article, CardContent, CardType } from '../../core/models';

/** Add a new card (/cards/new) or edit one (/cards/:id). */
@Component({
  selector: 'app-card-form-page',
  imports: [ReactiveFormsModule],
  template: `
    <h1>{{ id() ? 'Edit card' : 'New card' }}</h1>

    <form [formGroup]="form" (ngSubmit)="save()" class="form">
      <div class="segmented" role="radiogroup" aria-label="Card type">
        <label><input type="radio" formControlName="cardType" value="WORD" /> Word</label>
        <label><input type="radio" formControlName="cardType" value="SENTENCE" /> Sentence</label>
      </div>

      @if (form.value.cardType === 'WORD') {
        <div class="segmented" role="radiogroup" aria-label="Article">
          <label><input type="radio" formControlName="article" value="DE" /> de</label>
          <label><input type="radio" formControlName="article" value="HET" /> het</label>
          <label><input type="radio" formControlName="article" [value]="null" /> none</label>
        </div>
      }

      <label>Dutch
        <input formControlName="dutch" lang="nl" autocapitalize="off" autocomplete="off" />
      </label>
      <label>English
        <input formControlName="english" autocomplete="off" />
      </label>
      <label>Example (Dutch) <span class="muted small">optional</span>
        <input formControlName="exampleNl" lang="nl" autocomplete="off" />
      </label>
      <label>Example (English) <span class="muted small">optional</span>
        <input formControlName="exampleEn" autocomplete="off" />
      </label>

      @if (error(); as e) {
        <p class="error">{{ e }}</p>
      }

      <button type="submit" class="btn btn-primary" [disabled]="form.invalid || saving()">
        {{ saving() ? 'Saving…' : 'Save' }}
      </button>
      @if (id()) {
        <button type="button" class="btn btn-danger" (click)="remove()" [disabled]="saving()">Delete card</button>
      }
    </form>
  `,
  styles: `
    .form { display: flex; flex-direction: column; gap: 14px; }
    label { display: flex; flex-direction: column; gap: 6px; font-weight: 500; }
    .segmented {
      display: grid; grid-auto-flow: column; grid-auto-columns: 1fr; gap: 6px;
      label {
        flex-direction: row; justify-content: center; align-items: center; min-height: 48px;
        border: 1px solid var(--border); border-radius: var(--radius); background: var(--surface);
        &:has(input:checked) { border-color: var(--accent); background: var(--accent-soft); }
      }
      input { position: absolute; opacity: 0; pointer-events: none; }
    }
    .error { color: var(--again); margin: 0; }
  `,
})
export class CardFormPage implements OnInit {
  private readonly api = inject(CardApi);
  private readonly router = inject(Router);

  /** Route param, bound by withComponentInputBinding(). */
  readonly id = input<string>();

  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly form = inject(FormBuilder).group({
    cardType: ['WORD' as CardType, Validators.required],
    article: [null as Article | null],
    dutch: ['', [Validators.required, Validators.maxLength(500)]],
    english: ['', [Validators.required, Validators.maxLength(500)]],
    exampleNl: ['', Validators.maxLength(500)],
    exampleEn: ['', Validators.maxLength(500)],
  });

  ngOnInit(): void {
    const id = this.id();
    if (id) {
      this.api.get(id).subscribe((c) =>
        this.form.setValue({
          cardType: c.cardType, article: c.article, dutch: c.dutch, english: c.english,
          exampleNl: c.exampleNl ?? '', exampleEn: c.exampleEn ?? '',
        }),
      );
    }
  }

  save(): void {
    const v = this.form.getRawValue();
    const content: CardContent = {
      cardType: v.cardType!,
      article: v.cardType === 'WORD' ? v.article : null,
      dutch: v.dutch!,
      english: v.english!,
      exampleNl: v.exampleNl || null,
      exampleEn: v.exampleEn || null,
    };
    const id = this.id();
    this.saving.set(true);
    this.error.set(null);
    (id ? this.api.update(id, content) : this.api.create(content)).subscribe({
      next: () => this.router.navigate(['/cards']),
      error: (e: HttpErrorResponse) => {
        this.error.set(e.error?.detail ?? 'Could not save the card.');
        this.saving.set(false);
      },
    });
  }

  remove(): void {
    const id = this.id();
    if (!id) {
      return;
    }
    this.saving.set(true);
    this.api.delete(id).subscribe({
      next: () => this.router.navigate(['/cards']),
      error: () => {
        this.error.set('Could not delete the card.');
        this.saving.set(false);
      },
    });
  }
}
