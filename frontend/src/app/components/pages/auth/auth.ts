import { AuthService } from './../../../core/services/auth.service';
import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { ChangeDetectorRef } from '@angular/core';

@Component({
  selector: 'app-auth-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, TranslateModule],
  templateUrl: './auth.html',
  styleUrl: './auth.scss'
})
export class Auth {
  authForm: FormGroup;
  isLoginMode = true;
  errorMessage = '';
  loginMethod: 'email' | 'phone' = 'email';
  customerType: 'individual' | 'corporate' = 'individual';

  constructor(
    private fb: FormBuilder,
    private auth: AuthService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {
    this.authForm = this.fb.group({
      name: [''],
      surname: [''],
      phone: ['', Validators.pattern('[0-9]{12}$')],
      email: ['', [Validators.required, Validators.email]],
      birthdate: [''],
      password: ['', [Validators.required, Validators.minLength(8)]],
      address: [''],
      role: ['IndividualUser'],
      remember: [false]
    });
  }

  onSubmit() {
    if (this.authForm.invalid) return;

    const { name, surname, email, password, role, phone, birthdate, address, remember } = this.authForm.value;

    if (this.isLoginMode) {

      const foundUser = this.auth['users'].find(u => u.email === email && u.password === password);

      if (foundUser) {
        this.auth.login(foundUser, remember);
        this.router.navigate(['/']);
      } else {
        this.errorMessage = 'E-posta veya şifre hatalı!';
      }
    } else {
      const newUser = { id: Date.now(), name, surname, email, password, role, phone, birthdate, address };
      this.auth.register(newUser);
      alert('Kayıt başarılı! Şimdi giriş yapabilirsiniz.');
      this.setMode('login');
    }
  }

  setMode(mode: 'login' | 'register') {
    this.isLoginMode = (mode === 'login');
    this.cdr.detectChanges();
    this.errorMessage = '';
    this.authForm.reset({ role: 'IndividualUser', remember: false });
    const registerFields = ['name', 'surname','phone','birthDate','role'];
    if(this.isLoginMode){
      registerFields.forEach(field => {
        const control = this.authForm.get(field);
        if(control){
          control.clearValidators();
          control.updateValueAndValidity();
        }
      });
    } else {
      registerFields.forEach(field => {
        const control = this.authForm.get(field);
        if(control){
          if(field!=='address'){
            control.setValidators([Validators.required]);
          }
          if(field==='phone'){
            control.addValidators([Validators.pattern('[0-9]{12}$')]);
          }
          control.updateValueAndValidity();
        }
      });

    }
  }

  setLoginMehod(method: 'email' | 'phone'){
    this.loginMethod = method;
    this.cdr.detectChanges();
    if(method==='email'){
      this.authForm.get('phone')?.clearValidators();
    }else{
      this.authForm.get('email')?.clearValidators();
    }
    this.authForm.get('email')?.updateValueAndValidity();
    this.authForm.get('phone')?.updateValueAndValidity();
  }

}
