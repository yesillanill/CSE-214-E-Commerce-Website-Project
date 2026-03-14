import { CanDeactivateFn } from '@angular/router';

export interface CanComponentDeactivate{
  canDeactivate: () => Promise<boolean> | boolean;
}

export const pendingChangesGuard: CanDeactivateFn<CanComponentDeactivate> = (component) => {
  return component.canDeactivate();
};

