import { Injectable } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { BehaviorSubject } from 'rxjs';
import { ThemeService } from './theme.service';

@Injectable({
  providedIn: 'root',
})

export class AppService {
  private langSub = new BehaviorSubject<string>('tr');
  private fontSub = new BehaviorSubject<string>('medium');
  private themeSub = new BehaviorSubject<string>('dark');

  lang$ = this.langSub.asObservable();
  font$ = this.fontSub.asObservable();
  theme$ = this.themeSub.asObservable();

  constructor(private translate: TranslateService, private theme: ThemeService) {}

  initApp() {
    const savedTheme =localStorage.getItem('app-theme') ||'dark';
    const savedLang = localStorage.getItem('app-lang') || 'en';
    const savedFont = localStorage.getItem('app-font') || 'medium';
    this.setTheme(savedTheme);
    this.setLang(savedLang);
    this.setFont(savedFont);
  }

  setTheme(theme: string){
    this.theme.setTheme(theme);
  }

  setLang(lang: string){
    localStorage.setItem('app-lang', lang);
    this.translate.setDefaultLang(lang);
    this.translate.use(lang);
    this.langSub.next(lang);
  }

  setFont(font: string){
    localStorage.setItem('app-font', font);
    this.fontSub.next(font);
  }
}
