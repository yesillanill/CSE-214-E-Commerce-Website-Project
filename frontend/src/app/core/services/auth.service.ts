import { UserRole } from '../models/role.model';
import { Injectable } from '@angular/core';
import { User } from '../models/user.model';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { HttpClient } from '@angular/common/http';

interface AuthResponse {
  token: string;
  id: number;
  name: string;
  surname: string;
  email: string;
  phone: string;
  role: UserRole;
}

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

  getToken(): string | null {
    return localStorage.getItem('token') || sessionStorage.getItem('token');
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

  loginWithEmail(loginDto: any, remember: boolean): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/login/email`, loginDto).pipe(
      tap(response => this.handleAuthentication(response, remember))
    );
  }

  loginWithPhone(loginDto: any, remember: boolean): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/login/phone`, loginDto).pipe(
      tap(response => this.handleAuthentication(response, remember))
    );
  }

  registerIndividual(registerDto: any): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/register/individual`, registerDto).pipe(
      tap(response => this.handleAuthentication(response, true))
    );
  }

  registerStore(registerDto: any): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/register/store`, registerDto).pipe(
      tap(response => this.handleAuthentication(response, true))
    );
  }

  getProfile(userId: number): Observable<User> {
    return this.http.get<User>(`http://localhost:8080/api/users/profile/${userId}`);
  }

  updateProfile(userId: number, payload: Partial<User>): Observable<User> {
    return this.http.put<User>(`http://localhost:8080/api/users/profile/${userId}`, payload).pipe(
      tap(updatedUser => {
        this.updateUser(updatedUser);
      })
    );
  }

  private handleAuthentication(response: AuthResponse, remember: boolean) {
    // AuthResponse'dan User objesi oluştur
    const user: User = {
      id: response.id,
      name: response.name,
      surname: response.surname,
      email: response.email,
      phone: response.phone,
      role: response.role,
      password: '' // Güvenlik: şifre saklanmaz
    };

    this.currentUserSubject.next(user);

    if (remember) {
      localStorage.setItem('token', response.token);
      localStorage.setItem('user', JSON.stringify(user));
    } else {
      sessionStorage.setItem('token', response.token);
      sessionStorage.setItem('user', JSON.stringify(user));
    }
  }

  logout() {
    const user = this.currentUserSubject.value;
    if (user && user.id) {
      this.http.post(`${this.apiUrl}/logout/${user.id}`, {}).subscribe({
        error: (e) => console.error('Logout error', e)
      });
    }
    this.currentUserSubject.next(null);
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    sessionStorage.removeItem('token');
    sessionStorage.removeItem('user');
  }

  isLoggedIn(): boolean {
    return this.currentUserSubject.value !== null && this.getToken() !== null;
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
    const token = this.getToken();
    const userStr = localStorage.getItem('user') || sessionStorage.getItem('user');

    if (token && userStr) {
      // Token var ve kullanıcı bilgisi var — oturumu sürdür
      this.currentUserSubject.next(JSON.parse(userStr));
    } else {
      // Token yoksa veya kullanıcı bilgisi yoksa — temizle
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      sessionStorage.removeItem('token');
      sessionStorage.removeItem('user');
      this.currentUserSubject.next(null);
    }
  }
}
