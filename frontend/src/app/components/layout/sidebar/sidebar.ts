import { appConfig } from './../../../app.config';
import { routes } from './../../../app.routes';
import { Component, HostListener} from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router,RouterModule } from '@angular/router';
import { UserRole } from '../../../core/models/role.model';
import { AuthService } from '../../../core/services/auth.service';
import { AppService } from '../../../core/services/app.service';
import { TranslateModule } from '@ngx-translate/core';
import { ThemeService } from '../../../core/services/theme.service';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterModule, TranslateModule],
  templateUrl: './sidebar.html',
  styleUrl: './sidebar.scss',
})
export class Sidebar {

  constructor(public auth: AuthService, private router: Router,public appService: AppService, public themeService: ThemeService){}

  isMobileMenuOpen = false;

  @HostListener('window:resize',['$event'])
  onResize(event: any){
    if(window.innerWidth>768){
      this.isMobileMenuOpen = false;
      document.body.style.overflow = 'auto';
    }
  }

  toggleMobileMenu() {
    this.isMobileMenuOpen = !this.isMobileMenuOpen;
    if(this.isMobileMenuOpen){
      document.body.style.overflow = 'hidden';
    } else {
      document.body.style.overflow = 'auto';
    }
  }

  get role(): UserRole | null{
    return this.auth.getRole();
  }

  get logoSrc(): string {
    return this.themeService.getCurrentTheme() === 'dark'
      ? 'images/sepetix_white.png'
      : 'images/sepetix_black.png';
  }

  logout(){
    this.auth.logout();
    this.toggleMobileMenu();
  }

  toggleTheme(event: Event) {
    const isDark = (event.target as HTMLInputElement).checked;
    this.appService.setTheme(isDark ? 'dark' : 'light');
  }

  updateLang(lang: string) {
    this.appService.setLang(lang);
  }

}

