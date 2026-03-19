import { AuthService } from './../../../core/services/auth.service';
import { Component, OnInit } from '@angular/core';
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
export class Auth implements OnInit {
  authForm!: FormGroup;
  isLoginMode = true;
  errorMessage = '';
  loginMethod: 'email' | 'phone' = 'email';
  customerType: 'individual' | 'corporate' = 'individual';

  constructor(
    private fb: FormBuilder,
    private auth: AuthService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.authForm = this.fb.group({
      name: [''],
      surname: [''],
      phone: [''],
      email: [''],
      password: [''],
      remember: [false],
      // Individual fields
      street: [''],
      city: [''],
      postalCode: [''],
      country: [''],
      gender: [''],
      birthDate: [''],
      // Corporate fields
      storeName: [''],
      companyName: [''],
      taxNumber: [''],
      taxOffice: [''],
      companyAddress: ['']
    });
    this.updateValidators();
  }

  onSubmit() {
    if (this.authForm.invalid) return;

    if (this.isLoginMode) {
      const remember = this.authForm.value.remember;
      const loginObs = this.loginMethod === 'email' 
        ? this.auth.loginWithEmail({ email: this.authForm.value.email, password: this.authForm.value.password }, remember)
        : this.auth.loginWithPhone({ phone: this.authForm.value.phone, password: this.authForm.value.password }, remember);

      loginObs.subscribe({
        next: (user) => {
          this.router.navigate(['/']);
        },
        error: (err) => {
          this.errorMessage = 'E-posta/Telefon veya şifre hatalı!';
        }
      });
    } else {
      if (this.customerType === 'individual') {
        const dto = {
          name: this.authForm.value.name,
          surname: this.authForm.value.surname,
          email: this.authForm.value.email,
          password: this.authForm.value.password,
          phone: this.authForm.value.phone,
          street: this.authForm.value.street,
          city: this.authForm.value.city,
          postalCode: this.authForm.value.postalCode,
          country: this.authForm.value.country,
          gender: this.authForm.value.gender,
          birthDate: this.authForm.value.birthDate
        };
        this.auth.registerIndividual(dto).subscribe({
          next: () => this.handleRegisterSuccess(),
          error: (err) => this.errorMessage = 'Kayıt başarısız oldu.'
        });
      } else {
        const dto = {
          name: this.authForm.value.name,
          surname: this.authForm.value.surname,
          email: this.authForm.value.email,
          password: this.authForm.value.password,
          phone: this.authForm.value.phone,
          storeName: this.authForm.value.storeName,
          companyName: this.authForm.value.companyName,
          taxNumber: this.authForm.value.taxNumber,
          taxOffice: this.authForm.value.taxOffice,
          componyAddress: this.authForm.value.companyAddress 
        };
        this.auth.registerStore(dto).subscribe({
          next: () => this.handleRegisterSuccess(),
          error: (err) => this.errorMessage = 'Kayıt başarısız oldu.'
        });
      }
    }
  }

  handleRegisterSuccess() {
    alert('Kayıt başarılı! Şimdi giriş yapabilirsiniz.');
    this.setMode('login');
  }

  setMode(mode: 'login' | 'register') {
    this.isLoginMode = (mode === 'login');
    this.errorMessage = '';
    this.authForm.patchValue({ remember: false });
    this.updateValidators();
  }

  setCustomerType(type: 'individual' | 'corporate') {
    this.customerType = type;
    this.updateValidators();
  }

  setLoginMethod(method: 'email' | 'phone') {
    this.loginMethod = method;
    this.updateValidators();
  }

  private updateValidators() {
    // reset all validators first
    Object.keys(this.authForm.controls).forEach(key => {
      this.authForm.get(key)?.clearValidators();
    });

    // set base validators
    this.authForm.get('password')?.setValidators([Validators.required, Validators.minLength(6)]);

    if (this.isLoginMode) {
      if (this.loginMethod === 'email') {
        this.authForm.get('email')?.setValidators([Validators.required, Validators.email]);
      } else {
        this.authForm.get('phone')?.setValidators([Validators.required, Validators.pattern('^[0-9]+$')]);
      }
    } else {
      // Register Mode
      const commonReq = ['name', 'surname', 'email', 'phone'];
      commonReq.forEach(f => this.authForm.get(f)?.setValidators([Validators.required]));
      this.authForm.get('email')?.addValidators([Validators.email]);
      this.authForm.get('phone')?.addValidators([Validators.pattern('^[0-9]+$')]);

      if (this.customerType === 'individual') {
        const indReq = ['street', 'city', 'postalCode', 'country', 'gender', 'birthDate'];
        indReq.forEach(f => this.authForm.get(f)?.setValidators([Validators.required]));
      } else {
        const corpReq = ['storeName', 'companyName', 'taxNumber', 'taxOffice', 'companyAddress'];
        corpReq.forEach(f => this.authForm.get(f)?.setValidators([Validators.required]));
      }
    }

    Object.keys(this.authForm.controls).forEach(key => {
      this.authForm.get(key)?.updateValueAndValidity();
    });
    this.cdr.detectChanges();
  }
}
