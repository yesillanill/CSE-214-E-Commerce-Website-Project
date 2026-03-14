import {inject} from '@angular/core'
import { ActivatedRouteSnapshot, CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const roleGuard: CanActivateFn = (route: ActivatedRouteSnapshot) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const userRole = auth.getRole();
  const expectedRole =route.data['role'];
  if(userRole === expectedRole){
    return true;
  }else{
    router.navigate(['/access-denied']);
    return false;
  }
};

