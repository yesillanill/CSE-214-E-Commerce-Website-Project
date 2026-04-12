import { AppService } from './../../../core/services/app.service';
import { Component, OnInit, OnDestroy, HostListener, ChangeDetectorRef } from '@angular/core';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { Subscription } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { CommonModule } from '@angular/common';
import { CanComponentDeactivate } from '../../../core/guards/pending-changes-guard';
import Swal from 'sweetalert2';
import { ThemeService } from '../../../core/services/theme.service';
import { User } from '../../../core/models/user.model';
import { CardService } from '../../../core/services/card.service';
import { PaymentCardCreate } from '../../../core/models/payment-card.model';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [ReactiveFormsModule, FormsModule, TranslateModule, CommonModule],
  templateUrl: './settings.html',
  styleUrls: ['./settings.scss']
})
export class Settings implements OnInit, OnDestroy, CanComponentDeactivate {
  userForm!: FormGroup;
  public authSub!: Subscription;
  showAddCardForm = false;
  newCard: Partial<PaymentCardCreate> = {};

  constructor(
    public fb: FormBuilder,
    public auth: AuthService,
    public appService: AppService,
    public translate: TranslateService,
    public themeService: ThemeService,
    public cardService: CardService,
    private cdr: ChangeDetectorRef
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
              password: '',
              membershipType: fullProfile.membershipType || ''
            });
            // Reset dirty state after loading profile data
            this.userForm.markAsPristine();
            this.userForm.markAsUntouched();
            this.cdr.markForCheck();
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
      phone: [''],
      password: [''],
      street: [''],
      city: [''],
      postalCode: [''],
      country: [''],
      gender: [{value: '', disabled: true}],
      storeName: [''],
      companyName: [''],
      taxNumber: [''],
      taxOffice: [''],
      companyAddress: ['']
    })
  }

  async onUserSubmitAsync(): Promise<void> {
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
      return new Promise<void>((resolve) => {
        this.auth.updateProfile(user.id, updatePayload).subscribe({
          next: () => {
            Swal.fire({
              icon: 'success',
              title: this.translate.instant('SETTINGS.SUCCESS'),
              background: getComputedStyle(document.body).getPropertyValue('--card-bg').trim(),
              color: getComputedStyle(document.body).getPropertyValue('--text-color').trim(),
              timer: 1500
            });
            this.userForm.markAsPristine();
            this.userForm.markAsUntouched();
            resolve();
          },
          error: () => resolve()
        });
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

  toggleAddCardForm() {
    this.showAddCardForm = !this.showAddCardForm;
    if (this.showAddCardForm) {
      this.newCard = {};
    }
  }

  addCard() {
    const user = this.auth.getUser();
    if (!user) return;
    const card: PaymentCardCreate = {
      userId: user.id,
      cardHolderName: this.newCard.cardHolderName || '',
      cardNumber: this.newCard.cardNumber || '',
      expiryMonth: this.newCard.expiryMonth || 1,
      expiryYear: this.newCard.expiryYear || 2026,
      cvv: this.newCard.cvv || ''
    };
    this.cardService.addCard(card);
    this.showAddCardForm = false;
    this.newCard = {};
  }

  deleteCard(cardId: number) {
    Swal.fire({
      title: this.translate.instant('SETTINGS.DELETE_CARD_CONFIRM'),
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: this.translate.instant('SETTINGS.DELETE_CARD_DELETE'),
      cancelButtonText: this.translate.instant('SETTINGS.DELETE_CARD_CANCEL'),
      background: getComputedStyle(document.body).getPropertyValue('--card-bg').trim(),
      color: getComputedStyle(document.body).getPropertyValue('--text-color').trim()
    }).then(result => {
      if (result.isConfirmed) {
        this.cardService.deleteCard(cardId);
      }
    });
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
