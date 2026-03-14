import { USERS } from './../mock-data/user.mock';
import { UserRole } from '../models/role.model';
import { Injectable } from '@angular/core';
import { User } from '../models/user.model';
import { BehaviorSubject } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private currentUserSubject = new BehaviorSubject<User | null>(null);
  public currentUser$ = this.currentUserSubject.asObservable();

  // Mock kullanıcılar
  private users: User[] = USERS;

  constructor() {
    this.loadUserFromStorage();
  }

  getRole(): UserRole | null {
    return this.currentUserSubject.value?.role ?? null;
  }

  setRole(role: UserRole) {
    const user = this.currentUserSubject.value;
    if (user) {
      user.role = role;
      this.currentUserSubject.next(user);
    }
  }

  isAdmin(): boolean {
    return this.currentUserSubject.value?.role === "Admin";
  }

  isCorporateUser(): boolean {
    return this.currentUserSubject.value?.role === "CorporateUser";
  }

  isIndividualUser(): boolean {
    return this.currentUserSubject.value?.role === "IndividualUser";
  }

  login(user: User, remember: boolean) {
    this.currentUserSubject.next(user);

    if (remember) {
      localStorage.setItem('user', JSON.stringify(user));
    } else {
      sessionStorage.setItem('user', JSON.stringify(user));
    }
  }

  register(user: User) {
    this.users.push(user);
  }

  logout() {
    this.currentUserSubject.next(null);
    localStorage.removeItem('user');
    sessionStorage.removeItem('user');
  }

  isLoggedIn(): boolean {
    return this.currentUserSubject.value !== null;
  }

  getUser(): User | null {
    return this.currentUserSubject.value;
  }

  setCurrentUser(user: User) {
    this.currentUserSubject.next(user);
  }

  updateUser(payload: Partial<User>) {
    const current = this.currentUserSubject.value!;
    const updated = { ...current, ...payload };
    this.currentUserSubject.next(updated);
  }

  loadUserFromStorage() {
    const userStr = localStorage.getItem('user') || sessionStorage.getItem('user');
    if (userStr) {
      this.currentUserSubject.next(JSON.parse(userStr));
    }
  }
}

