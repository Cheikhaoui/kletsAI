import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { App } from './app';

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideRouter([])],
    }).compileComponents();
  });

  it('renders the bottom tab bar', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const tabs = (fixture.nativeElement as HTMLElement).querySelectorAll('nav.tabs a');
    expect([...tabs].map((a) => a.textContent?.trim())).toEqual(['Home', 'Review', 'Cards']);
  });
});
