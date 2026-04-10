import { Routes } from '@angular/router';
import { Analytics } from './components/pages/analytics/analytics';
import { AiAssistant } from './components/pages/ai-assistant/ai-assistant';

import { Customers } from './components/pages/customers/customers';
import { Users } from './components/pages/users/users';
import { Orders } from './components/pages/orders/orders';
import { Inventory } from './components/pages/inventory/inventory';
import { AddProduct } from './components/pages/add-product/add-product';
import { EditProduct } from './components/pages/edit-product/edit-product';
import { Reports } from './components/pages/reports/reports';
import { Stores } from './components/pages/stores/stores';
import { StoreOrders } from './components/pages/store-orders/store-orders';
import { AuditLogs } from './components/pages/audit-logs/audit-logs';
import { Support } from './components/pages/support/support';
import { SupportRequests } from './components/pages/support-requests/support-requests';

import { Auth } from './components/pages/auth/auth'
import { Settings } from './components/pages/settings/settings';
import { PageNotFound } from './components/pages/page-not-found/page-not-found';
import { roleGuard } from './core/guards/role-guard';
import { AccessDenied } from './components/pages/access-denied/access-denied';
import { pendingChangesGuard } from './core/guards/pending-changes-guard';
import { Home } from './components/pages/home/home';
import { ProductDetailPage } from './components/pages/product-detail/product-detail';
import { Cart } from './components/pages/cart/cart'
import { Wishlist } from './components/pages/wishlist/wishlist';
import { Products } from './components/pages/products/products';
import { Checkout } from './components/pages/checkout/checkout';
import { MyReviews } from './components/pages/my-reviews/my-reviews';

export const routes: Routes = [
  {path:"", component: Home},
  {path:'cart', component:Cart},
  {path:'checkout', component:Checkout, canActivate: [roleGuard], data: {role:'IndividualUser'}},
  {path: 'wishlist', component: Wishlist},
  {path:"analytics", component: Analytics},
  {path:"ai-assistant", component: AiAssistant},
  {path:"customers", component: Customers, canActivate: [roleGuard], data: {role:'Admin'}},
  {path:"users", component: Users, canActivate: [roleGuard], data: {role:'Admin'}},
  {path:"reports", component: Reports, canActivate: [roleGuard], data: {role:'Admin'}},
  {path:"orders", component: Orders, canActivate: [roleGuard], data: {role:'IndividualUser'}},
  {path:"my-reviews", component: MyReviews, canActivate: [roleGuard], data: {role:'IndividualUser'}},
  {path:"inventory/add", component: AddProduct, canActivate: [roleGuard], canDeactivate: [pendingChangesGuard], data: {role:'CorporateUser'}},
  {path:"inventory/edit/:id", component: EditProduct, canActivate: [roleGuard], canDeactivate: [pendingChangesGuard], data: {role:'CorporateUser'}},
  {path:"inventory", component: Inventory, canActivate: [roleGuard], data: {role:'CorporateUser'}},
  {path:"store-orders", component: StoreOrders, canActivate: [roleGuard], data: {role:'CorporateUser'}},
  {path:"stores", component: Stores, canActivate: [roleGuard], data: {role:'Admin'}},
  {path:"audit-logs", component: AuditLogs, canActivate: [roleGuard], data: {role:'Admin'}},
  {path:"support", component: Support},
  {path:"support-requests", component: SupportRequests, canActivate: [roleGuard], data: {role:'Admin'}},
  {path:"products/brand/:name", component: Products, data: {type: 'brand'}},
  {path:"products/store/:name", component: Products, data: {type: 'store'}},
  {path:"products/category/:name", component: Products, data: {type: 'category'}},
  {path:"products/:id", component: ProductDetailPage},
  {path: 'products', component: Products, data: { type: 'all' } },
  {path:"auth", component: Auth},
  {path:"settings", component: Settings, canDeactivate: [pendingChangesGuard]},
  {path:"access-denied", component: AccessDenied},
  {path:"**", component: PageNotFound}
];

