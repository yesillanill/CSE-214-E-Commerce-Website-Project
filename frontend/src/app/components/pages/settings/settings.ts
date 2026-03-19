import { AppService } from './../../../core/services/app.service';
import { Component, OnInit, OnDestroy, HostListener } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { Subscription } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { CommonModule } from '@angular/common';
import { CanComponentDeactivate } from '../../../core/guards/pending-changes-guard';
import Swal from 'sweetalert2';
import { ThemeService } from '../../../core/services/theme.service';
import { User } from '../../../core/models/user.model';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [ReactiveFormsModule, TranslateModule, CommonModule],
  templateUrl: './settings.html',
  styleUrls: ['./settings.scss']
})
export class Settings implements OnInit, OnDestroy, CanComponentDeactivate {
  userForm!: FormGroup;
  public authSub!: Subscription;

  constructor(
    public fb: FormBuilder,
    public auth: AuthService,
    public appService: AppService,
    public translate: TranslateService,
    public themeService: ThemeService
  ){}

  ngOnInit(){
    this.initForm();
    this.authSub = this.auth.currentUser$.subscribe(user => {
      if(user && this.userForm){
        this.auth.getProfile(user.id).subscribe({
          next: (fullProfile) => {
            let formattedDate = '';
            if(fullProfile.birthdate){
              formattedDate = new Date(fullProfile.birthdate).toISOString().split('T')[0];
            }
            this.userForm.patchValue({
              ...fullProfile,
              birthdate: formattedDate,
              email: fullProfile.email,
              phone: fullProfile.phone,
              password: fullProfile.password,
              membershipType: fullProfile.membershipType || ''
            });
            this.userForm.updateValueAndValidity();
          }
        });
      } else {
        if(this.userForm){
          this.userForm.reset();
        }
      }
    });
  }

  initForm(){
    this.userForm = this.fb.group({
      name: [{value: '',  disabled: true}],
      surname: [{value: '',  disabled: true}],
      role: [{value: '',  disabled: true}],
      birthdate: [{value: '',  disabled: true}],
      membershipType: [{value: '', disabled: true}],
      email: ['', Validators.email],
      phone: ['', Validators.pattern('^[0-9]{10,15}$')],
      password: ['', Validators.minLength(8)],
      street: [''],
      city: [''],
      postalCode: [''],
      country: [''],
      gender: [''],
      storeName: [''],
      companyName: [''],
      taxNumber: [''],
      taxOffice: [''],
      companyAddress: ['']
    })
  }

  async onUserSubmitAsync(): Promise<void> {
    if (!this.userForm.valid) return;
    const rawData = this.userForm.getRawValue();
    const updatePayload: Partial<User> = {};

    Object.keys(rawData).forEach(key => {
      const value = rawData[key];
      if (value !== null && value !== undefined && value !== '') {
        updatePayload[key as keyof User] = value;
      }
    });
    
    const user = this.auth.getUser();
    if(user) {
      this.auth.updateProfile(user.id, updatePayload).subscribe({
        next: () => {
          Swal.fire({
            icon: 'success',
            title: 'Success',
            background: getComputedStyle(document.body).getPropertyValue('--card-bg').trim(),
            color: getComputedStyle(document.body).getPropertyValue('--text-color').trim(),
            timer: 1500
          });
          this.userForm.markAsPristine();
        }
      });
    }
  }

  updateTheme(value: string){
    this.themeService.setTheme(value);
  }

  updateLang(value: string){
    this.appService.setLang(value);
  }

  updateFont(value: string){
    this.appService.setFont(value);
  }

  ngOnDestroy(): void {
    if(this.authSub){
      this.authSub.unsubscribe();
    }
  }

  canDeactivate(): Promise<boolean> | boolean {
    if(!this.userForm?.dirty){
      return true;
    }
    return Swal.fire({
      title: this.translate.instant('SETTINGS.UNSAVED.TITLE'),
      icon: 'warning',
      showCancelButton: true,
      showDenyButton: true,
      confirmButtonText: this.translate.instant('SETTINGS.UNSAVED.SAVE'),
      denyButtonText: this.translate.instant('SETTINGS.UNSAVED.DISCARD'),
      cancelButtonText: this.translate.instant('SETTINGS.UNSAVED.STAY'),
      background: getComputedStyle(document.body).getPropertyValue('--card-bg').trim(),
      color: getComputedStyle(document.body).getPropertyValue('--text-color').trim()
    }).then(result => {
      if(result.isConfirmed){
        return this.onUserSubmitAsync().then(() => true);
      }
      if(result.isDenied){
        return true;
      }
      return false;
    })
  }

  @HostListener('window:beforeunload',['$event']) unloadNotification($event: BeforeUnloadEvent){
    if(this.userForm?.dirty){
      $event.preventDefault();
      $event.returnValue = this.translate.instant('SETTINGS.UNSAVED');
    }
  }
}

