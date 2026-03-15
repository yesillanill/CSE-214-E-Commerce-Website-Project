import { Component } from '@angular/core';

@Component({
  selector: 'app-loading-spinner',
  standalone: true,
  template: `<div class="spinner-overlay"><span class="loader"></span></div>`,
  styleUrl: './loading-spinner.scss'
})
export class LoadingSpinner {}
