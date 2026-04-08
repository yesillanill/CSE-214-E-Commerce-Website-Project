import { AppService } from './core/services/app.service';
import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { Sidebar } from './components/layout/sidebar/sidebar';
import { AuthService } from './core/services/auth.service';
import { FloatingActions } from './components/shared/floating-actions/floating-actions';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, Sidebar, FloatingActions],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})

export class App {
  constructor(
    private appService: AppService,
    public auth: AuthService,
  ){
    this.appService.initApp();
  }
}


