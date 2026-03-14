import { Component } from '@angular/core';
import { TranslateService, TranslateModule } from '@ngx-translate/core';
import { RouterLink } from "@angular/router";

@Component({
  selector: 'app-access-denied',
  imports: [TranslateModule, RouterLink],
  templateUrl: './access-denied.html',
  styleUrl: './access-denied.scss',
})
export class AccessDenied {}
