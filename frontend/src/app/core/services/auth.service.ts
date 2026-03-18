import { UserRole } from '../models/role.model';
import { Injectable } from '@angular/core';
import { User } from '../models/user.model';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { HttpClient } from '@angular/common/http';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private apiUrl = 'http://localhost:8080/api/auth';
  private currentUserSubject = new BehaviorSubject<User | null>(null);
  public currentUser$ = this.currentUserSubject.asObservable();

  constructor(private http: HttpClient) {
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
    const role = this.currentUserSubject.value?.role as string | undefined;
    return role === "Admin" || role === "ADMIN";
  }

  isCorporateUser(): boolean {
    const role = this.currentUserSubject.value?.role as string | undefined;
    return role === "CorporateUser" || role === "CORPORATE" || role === "STORE";
  }

  isIndividualUser(): boolean {
    const role = this.currentUserSubject.value?.role as string | undefined;
    return role === "IndividualUser" || role === "INDIVIDUAL";
  }

  loginWithEmail(loginDto: any, remember: boolean): Observable<User> {
    return this.http.post<User>(`${this.apiUrl}/login/email`, loginDto).pipe(
      tap(user => this.handleAuthentication(user, remember))
    );
  }

  loginWithPhone(loginDto: any, remember: boolean): Observable<User> {
    return this.http.post<User>(`${this.apiUrl}/login/phone`, loginDto).pipe(
      tap(user => this.handleAuthentication(user, remember))
    );
  }

  registerIndividual(registerDto: any): Observable<User> {
    return this.http.post<User>(`${this.apiUrl}/register/individual`, registerDto);
  }

  registerStore(registerDto: any): Observable<User> {
    return this.http.post<User>(`${this.apiUrl}/register/store`, registerDto);
  }

  private handleAuthentication(user: User, remember: boolean) {
    this.currentUserSubject.next(user);
    if (remember) {
      localStorage.setItem('user', JSON.stringify(user));
    } else {
      sessionStorage.setItem('user', JSON.stringify(user));
    }
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
    const current = this.currentUserSubject.value;
    if (current) {
      const updated = { ...current, ...payload } as User;
      this.currentUserSubject.next(updated);
      if (localStorage.getItem('user')) localStorage.setItem('user', JSON.stringify(updated));
      if (sessionStorage.getItem('user')) sessionStorage.setItem('user', JSON.stringify(updated));
    }
  }

  loadUserFromStorage() {
    const userStr = localStorage.getItem('user') || sessionStorage.getItem('user');
    if (userStr) {
      this.currentUserSubject.next(JSON.parse(userStr));
    }
  }
}
