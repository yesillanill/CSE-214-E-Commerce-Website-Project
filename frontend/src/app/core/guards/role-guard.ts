import {inject} from '@angular/core'
import { ActivatedRouteSnapshot, CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

function normalizeRole(role: string | null | undefined): string {
  if (!role) return '';
  const r = role.toLowerCase();
  if (r === 'admin') return 'admin';
  if (r === 'corporate' || r === 'corporateuser' || r === 'store') return 'corporate';
  if (r === 'individual' || r === 'individualuser') return 'individual';
  return r;
}

export const roleGuard: CanActivateFn = (route: ActivatedRouteSnapshot) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const userRole = normalizeRole(auth.getRole() as string);
  const expectedRole = normalizeRole(route.data['role'] as string);
  if(userRole === expectedRole){
    return true;
  }else{
    router.navigate(['/access-denied']);
    return false;
  }
};
