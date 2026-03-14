import { Routes } from '@angular/router';
import { Analytics } from './components/pages/analytics/analytics';
import { AiAssistant } from './components/pages/ai-assistant/ai-assistant';

import { Customers } from './components/pages/customers/customers';
import { Shipments } from './components/pages/shipments/shipments';
import { Users } from './components/pages/users/users';
import { Orders } from './components/pages/orders/orders';
import { Inventory } from './components/pages/inventory/inventory';
import { Reports } from './components/pages/reports/reports';
import { Stores } from './components/pages/stores/stores';

import { Auth } from './components/pages/auth/auth'
import { Settings } from './components/pages/settings/settings';
import { PageNotFound } from './components/pages/page-not-found/page-not-found';
import { roleGuard } from './core/guards/role-guard';
import { AccessDenied } from './components/pages/access-denied/access-denied';
import { pendingChangesGuard } from './core/guards/pending-changes-guard';
import { Home } from './components/pages/home/home';
import { ProductDetail } from './components/pages/product-detail/product-detail';
import { Cart } from './components/pages/cart/cart'
import { Wishlist } from './components/pages/wishlist/wishlist';
import { Products } from './components/pages/products/products';

export const routes: Routes = [
  {path:"", component: Home},
  {path:'cart', component:Cart},
  {path: 'wishlist', component: Wishlist},
  {path:"analytics", component: Analytics},
  {path:"ai-asistant", component: AiAssistant},
  {path:"customers", component: Customers, canActivate: [roleGuard], data: {role:'CorporateUser'}},
  {path:"shipments", component: Shipments, canActivate: [roleGuard], data: {role:'CorporateUser'}},
  {path:"users", component: Users, canActivate: [roleGuard], data: {role:'Admin'}},
  {path:"reports", component: Reports, canActivate: [roleGuard], data: {role:'Admin'}},
  {path:"orders", component: Orders},
  {path:"inventory", component: Inventory, canActivate: [roleGuard], data: {role:'CorparateUser'}},
  {path:"stores", component: Stores, canActivate: [roleGuard], data: {role:'Admin'}},
  {path:"products/brand/:name", component: Products, data: {type: 'brand'}},
  {path:"products/store/:name", component: Products, data: {type: 'store'}},
  {path:"products/category/:name", component: Products, data: {type: 'category'}},
  {path: 'products', component: Products, data: { type: 'all' } },
  {path:"auth", component: Auth},
  {path:"settings", component: Settings, canDeactivate: [pendingChangesGuard]},
  {path:"access-denied", component: AccessDenied},
  {path:"**", component: PageNotFound}
];


