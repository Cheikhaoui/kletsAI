import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', title: 'KletsAI', loadComponent: () => import('./features/home/home-page').then((m) => m.HomePage) },
  { path: 'review', title: 'Review · KletsAI', loadComponent: () => import('./features/review/review-page').then((m) => m.ReviewPage) },
  { path: 'cards', title: 'Cards · KletsAI', loadComponent: () => import('./features/cards/cards-page').then((m) => m.CardsPage) },
  { path: 'cards/new', title: 'New card · KletsAI', loadComponent: () => import('./features/cards/card-form-page').then((m) => m.CardFormPage) },
  { path: 'cards/:id', title: 'Edit card · KletsAI', loadComponent: () => import('./features/cards/card-form-page').then((m) => m.CardFormPage) },
  { path: '**', redirectTo: '' },
];
