import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);

  // Auth endpoint'lerine token ekleme
  if (req.url.includes('/api/auth/')) {
    return next(req);
  }

  // Token'ı storage'dan al
  const token = getToken();

  // Token varsa Authorization header'ı ekle
  if (token) {
    req = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
  }

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      // 401 Unauthorized — token geçersiz veya süresi dolmuş
      if (error.status === 401) {
        // Storage'ı temizle
        localStorage.removeItem('token');
        localStorage.removeItem('user');
        sessionStorage.removeItem('token');
        sessionStorage.removeItem('user');
        // Login sayfasına yönlendir
        router.navigate(['/auth']);
      }
      return throwError(() => error);
    })
  );
};

function getToken(): string | null {
  return localStorage.getItem('token') || sessionStorage.getItem('token');
}
