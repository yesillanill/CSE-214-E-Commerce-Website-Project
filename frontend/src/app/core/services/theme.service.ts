import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

export type ThemeMode = 'light' | 'dark';

@Injectable({
  providedIn: 'root',
})
export class ThemeService {
  private themeSub = new BehaviorSubject<string>('dark');
  theme$ = this.themeSub.asObservable();

  constructor(){
    this.loadTheme();
  }

  loadTheme(){
    const theme = localStorage.getItem('app-theme') as ThemeMode;
    this.applyTheme(theme);
    this.themeSub.next(theme);
  }

  setTheme(theme: string) {
    this.applyTheme(theme);
    localStorage.setItem('app-theme', theme);
    this.themeSub.next(theme);
  }

  getCurrentTheme(): ThemeMode {
    return document.body.classList.contains('dark') ? 'dark' : 'light';
  }

  applyTheme(theme: string){
    document.body.classList.toggle('dark', theme==='dark')
  }

}
