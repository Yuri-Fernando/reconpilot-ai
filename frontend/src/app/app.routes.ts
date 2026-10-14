import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
  { path: 'dashboard', loadComponent: () => import('./pages/dashboard/dashboard.page').then((m) => m.DashboardPage) },
  { path: 'imports', loadComponent: () => import('./pages/imports/imports.page').then((m) => m.ImportsPage) },
  { path: 'reconciliations', loadComponent: () => import('./pages/reconciliations/reconciliations.page').then((m) => m.ReconciliationsPage) },
  { path: 'reconciliations/:id', loadComponent: () => import('./pages/reconciliation-detail/reconciliation-detail.page').then((m) => m.ReconciliationDetailPage) },
  { path: 'audit', loadComponent: () => import('./pages/audit/audit.page').then((m) => m.AuditPage) },
  { path: 'about', loadComponent: () => import('./pages/about/about.page').then((m) => m.AboutPage) },
  { path: '**', redirectTo: 'dashboard' },
];
