import { Component, OnInit, OnDestroy, AfterViewChecked, ChangeDetectorRef, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { AuthService } from '../../../core/services/auth.service';
import { AnalyticsService } from '../../../core/services/analytics.service';
import { Router } from '@angular/router';
import {
  PublicStats,
  IndividualAnalytics,
  CorporateAnalytics,
  AdminAnalytics
} from '../../../core/models/analytics.model';
import { Chart, registerables } from 'chart.js';

Chart.register(...registerables);

@Component({
  selector: 'app-analytics',
  imports: [CommonModule, RouterLink, TranslateModule],
  templateUrl: './analytics.html',
  styleUrl: './analytics.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class Analytics implements OnInit, OnDestroy, AfterViewChecked {
  // Public Stats
  publicStats: PublicStats | null = null;

  // Role-based analytics
  individualData: IndividualAnalytics | null = null;
  corporateData: CorporateAnalytics | null = null;
  adminData: AdminAnalytics | null = null;

  // Loading states
  isLoadingPublic = true;
  isLoadingPrivate = true;

  // Charts
  private charts: Chart[] = [];
  private chartsRendered = false;

  constructor(
    public auth: AuthService,
    private analyticsService: AnalyticsService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadPublicStats();
    if (this.auth.isLoggedIn()) {
      this.loadPrivateAnalytics();
    } else {
      this.isLoadingPrivate = false;
    }
  }

  ngAfterViewChecked(): void {
    if (!this.chartsRendered && !this.isLoadingPrivate) {
      if (this.individualData || this.corporateData || this.adminData) {
        this.renderCharts();
      }
    }
  }

  ngOnDestroy(): void {
    this.charts.forEach(c => c.destroy());
  }

  private renderCharts(): void {
    if (this.chartsRendered) return;
    if (this.individualData) this.renderIndividualCharts();
    if (this.corporateData) this.renderCorporateCharts();
    if (this.adminData) this.renderAdminCharts();
    this.chartsRendered = true;
  }

  // ─── PUBLIC STATS ────────────────────────────────

  private loadPublicStats(): void {
    this.analyticsService.getPublicStats().subscribe({
      next: (data) => {
        this.publicStats = data;
        this.isLoadingPublic = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.isLoadingPublic = false;
        this.cdr.markForCheck();
      }
    });
  }

  // ─── PRIVATE ANALYTICS ──────────────────────────

  private loadPrivateAnalytics(): void {
    if (this.auth.isIndividualUser()) {
      this.analyticsService.getIndividualAnalytics().subscribe({
        next: (data) => {
          this.individualData = data;
          this.isLoadingPrivate = false;
          this.cdr.markForCheck();
        },
        error: () => { this.isLoadingPrivate = false; this.cdr.markForCheck(); }
      });
    } else if (this.auth.isCorporateUser()) {
      this.analyticsService.getCorporateAnalytics().subscribe({
        next: (data) => {
          this.corporateData = data;
          this.isLoadingPrivate = false;
          this.cdr.markForCheck();
        },
        error: () => { this.isLoadingPrivate = false; this.cdr.markForCheck(); }
      });
    } else if (this.auth.isAdmin()) {
      this.analyticsService.getAdminAnalytics().subscribe({
        next: (data) => {
          this.adminData = data;
          this.isLoadingPrivate = false;
          this.cdr.markForCheck();
        },
        error: () => { this.isLoadingPrivate = false; this.cdr.markForCheck(); }
      });
    } else {
      this.isLoadingPrivate = false;
    }
  }

  // ─── INDIVIDUAL CHARTS ──────────────────────────

  private renderIndividualCharts(): void {
    if (!this.individualData) return;
    const d = this.individualData;

    // 1. Monthly Spend Line Chart
    this.createChart('monthlySpendChart', {
      type: 'line',
      data: {
        labels: d.monthlySpend.map(m => m.month),
        datasets: [{
          label: 'Harcama',
          data: d.monthlySpend.map(m => m.amount),
          borderColor: '#667eea',
          backgroundColor: 'rgba(102,126,234,0.15)',
          fill: true,
          tension: 0.4,
          pointRadius: 4
        }]
      },
      options: this.lineChartOptions('Aylık Harcama Trendi')
    });

    // 2. Order Status Donut
    const statusLabels = Object.keys(d.orderStatusDist);
    const statusValues = Object.values(d.orderStatusDist);
    this.createChart('orderStatusChart', {
      type: 'doughnut',
      data: {
        labels: statusLabels,
        datasets: [{
          data: statusValues,
          backgroundColor: ['#48bb78', '#4299e1', '#ecc94b', '#f56565', '#9f7aea', '#ed8936']
        }]
      },
      options: this.doughnutOptions('Sipariş Durumu Dağılımı')
    });

    // 3. Category Spend Bar (horizontal)
    this.createChart('categorySpendChart', {
      type: 'bar',
      data: {
        labels: d.categorySpend.map(c => c.category),
        datasets: [{
          label: 'Harcama',
          data: d.categorySpend.map(c => c.amount),
          backgroundColor: '#667eea'
        }]
      },
      options: {
        indexAxis: 'y' as const,
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { display: false }, title: { display: true, text: 'Kategori Bazlı Alışveriş', color: '#e0e0e0' } },
        scales: {
          x: { ticks: { color: '#a0a0a0' }, grid: { color: 'rgba(255,255,255,0.06)' } },
          y: { ticks: { color: '#a0a0a0' }, grid: { color: 'rgba(255,255,255,0.06)' } }
        }
      }
    });
  }

  // ─── CORPORATE CHARTS ───────────────────────────

  private renderCorporateCharts(): void {
    if (!this.corporateData) return;
    const d = this.corporateData;

    // 1. Revenue Line Chart
    this.createChart('revenueLineChart', {
      type: 'line',
      data: {
        labels: d.revenueSeries.map(m => m.month),
        datasets: [{
          label: 'Gelir',
          data: d.revenueSeries.map(m => m.amount),
          borderColor: '#48bb78',
          backgroundColor: 'rgba(72,187,120,0.15)',
          fill: true,
          tension: 0.4,
          pointRadius: 4
        }]
      },
      options: this.lineChartOptions('Aylık Gelir Trendi')
    });

    // 2. Category Revenue Bar
    this.createChart('categoryRevenueChart', {
      type: 'bar',
      data: {
        labels: d.categoryRevenue.map(c => c.category),
        datasets: [{
          label: 'Gelir',
          data: d.categoryRevenue.map(c => c.amount),
          backgroundColor: ['#667eea', '#48bb78', '#ecc94b', '#f56565', '#9f7aea', '#ed8936', '#38b2ac']
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { display: false }, title: { display: true, text: 'Kategori Bazlı Satış', color: '#e0e0e0' } },
        scales: {
          x: { ticks: { color: '#a0a0a0' }, grid: { color: 'rgba(255,255,255,0.06)' } },
          y: { ticks: { color: '#a0a0a0' }, grid: { color: 'rgba(255,255,255,0.06)' } }
        }
      }
    });

    // 3. Inventory Horizontal Bar
    const inv = d.inventoryStatus.slice(0, 20);
    this.createChart('inventoryChart', {
      type: 'bar',
      data: {
        labels: inv.map(i => i.productName),
        datasets: [{
          label: 'Stok',
          data: inv.map(i => i.stock),
          backgroundColor: inv.map(i => i.lowStock ? '#f56565' : '#4299e1')
        }]
      },
      options: {
        indexAxis: 'y' as const,
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { display: false }, title: { display: true, text: 'Envanter Durumu', color: '#e0e0e0' } },
        scales: {
          x: { ticks: { color: '#a0a0a0' }, grid: { color: 'rgba(255,255,255,0.06)' } },
          y: { ticks: { color: '#a0a0a0', font: { size: 10 } }, grid: { color: 'rgba(255,255,255,0.06)' } }
        }
      }
    });

    // 4. Membership Donut
    const memLabels = Object.keys(d.membershipDist);
    const memValues = Object.values(d.membershipDist);
    this.createChart('membershipChart', {
      type: 'doughnut',
      data: {
        labels: memLabels,
        datasets: [{
          data: memValues,
          backgroundColor: ['#ecc94b', '#a0aec0', '#ed8936']
        }]
      },
      options: this.doughnutOptions('Müşteri Segmentasyonu')
    });

    // 5. Top Products Bar
    const top = d.topProducts.slice(0, 10);
    this.createChart('topProductsChart', {
      type: 'bar',
      data: {
        labels: top.map(p => p.productName),
        datasets: [{
          label: 'Satış Adedi',
          data: top.map(p => p.salesCount),
          backgroundColor: '#667eea'
        }, {
          label: 'Gelir',
          data: top.map(p => p.revenue),
          backgroundColor: '#48bb78'
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: { title: { display: true, text: 'En Çok Satan Ürünler (Top 10)', color: '#e0e0e0' }, legend: { labels: { color: '#a0a0a0' } } },
        scales: {
          x: { ticks: { color: '#a0a0a0', font: { size: 9 }, maxRotation: 45 }, grid: { color: 'rgba(255,255,255,0.06)' } },
          y: { ticks: { color: '#a0a0a0' }, grid: { color: 'rgba(255,255,255,0.06)' } }
        }
      }
    });

    // 6. Satisfaction Bar
    const satLabels = Object.keys(d.satisfactionDist);
    const satValues = Object.values(d.satisfactionDist);
    this.createChart('satisfactionChart', {
      type: 'bar',
      data: {
        labels: satLabels,
        datasets: [{
          label: 'Müşteri Sayısı',
          data: satValues,
          backgroundColor: ['#48bb78', '#ecc94b', '#f56565']
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { display: false }, title: { display: true, text: 'Müşteri Memnuniyeti', color: '#e0e0e0' } },
        scales: {
          x: { ticks: { color: '#a0a0a0' }, grid: { color: 'rgba(255,255,255,0.06)' } },
          y: { ticks: { color: '#a0a0a0' }, grid: { color: 'rgba(255,255,255,0.06)' } }
        }
      }
    });
  }

  // ─── ADMIN CHARTS ───────────────────────────────

  private renderAdminCharts(): void {
    if (!this.adminData) return;
    const d = this.adminData;

    // 1. Platform Revenue Area Chart
    this.createChart('platformRevenueChart', {
      type: 'line',
      data: {
        labels: d.platformRevenueSeries.map(m => m.month),
        datasets: [{
          label: 'Platform Geliri',
          data: d.platformRevenueSeries.map(m => m.amount),
          borderColor: '#667eea',
          backgroundColor: 'rgba(102,126,234,0.2)',
          fill: true,
          tension: 0.4,
          pointRadius: 4
        }]
      },
      options: this.lineChartOptions('Platform Geneli Gelir Trendi')
    });

    // 2. Store Comparison Grouped Bar
    this.createChart('storeComparisonChart', {
      type: 'bar',
      data: {
        labels: d.topStores.map(s => s.storeName),
        datasets: [{
          label: 'Gelir',
          data: d.topStores.map(s => s.revenue),
          backgroundColor: '#667eea'
        }, {
          label: 'Sipariş',
          data: d.topStores.map(s => s.orderCount),
          backgroundColor: '#48bb78'
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: { title: { display: true, text: 'Mağaza Karşılaştırma (Top 10)', color: '#e0e0e0' }, legend: { labels: { color: '#a0a0a0' } } },
        scales: {
          x: { ticks: { color: '#a0a0a0', font: { size: 9 }, maxRotation: 45 }, grid: { color: 'rgba(255,255,255,0.06)' } },
          y: { ticks: { color: '#a0a0a0' }, grid: { color: 'rgba(255,255,255,0.06)' } }
        }
      }
    });

    // 3. Registration Trend Line (individual vs corporate)
    this.createChart('registrationChart', {
      type: 'line',
      data: {
        labels: d.registrationTrend.map(r => r.month),
        datasets: [{
          label: 'Bireysel',
          data: d.registrationTrend.map(r => r.individualCount),
          borderColor: '#4299e1',
          backgroundColor: 'rgba(66,153,225,0.1)',
          fill: true,
          tension: 0.4
        }, {
          label: 'Kurumsal',
          data: d.registrationTrend.map(r => r.corporateCount),
          borderColor: '#ed8936',
          backgroundColor: 'rgba(237,137,54,0.1)',
          fill: true,
          tension: 0.4
        }]
      },
      options: this.lineChartOptions('Kullanıcı Kayıt Trendi')
    });

    // 4. Category Performance Bar
    this.createChart('categoryPerfChart', {
      type: 'bar',
      data: {
        labels: d.categoryPerformance.map(c => c.category),
        datasets: [{
          label: 'Satış Hacmi',
          data: d.categoryPerformance.map(c => c.amount),
          backgroundColor: ['#667eea', '#48bb78', '#ecc94b', '#f56565', '#9f7aea', '#ed8936', '#38b2ac', '#e53e3e', '#d69e2e', '#3182ce']
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { display: false }, title: { display: true, text: 'Kategori Platform Performansı', color: '#e0e0e0' } },
        scales: {
          x: { ticks: { color: '#a0a0a0' }, grid: { color: 'rgba(255,255,255,0.06)' } },
          y: { ticks: { color: '#a0a0a0' }, grid: { color: 'rgba(255,255,255,0.06)' } }
        }
      }
    });
  }

  // ─── CHART HELPERS ──────────────────────────────

  private createChart(canvasId: string, config: any): void {
    const el = document.getElementById(canvasId) as HTMLCanvasElement;
    if (!el) return;
    const chart = new Chart(el, config);
    this.charts.push(chart);
  }

  private lineChartOptions(title: string): any {
    return {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        title: { display: true, text: title, color: '#e0e0e0' },
        legend: { labels: { color: '#a0a0a0' } }
      },
      scales: {
        x: { ticks: { color: '#a0a0a0' }, grid: { color: 'rgba(255,255,255,0.06)' } },
        y: { ticks: { color: '#a0a0a0' }, grid: { color: 'rgba(255,255,255,0.06)' } }
      }
    };
  }

  private doughnutOptions(title: string): any {
    return {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        title: { display: true, text: title, color: '#e0e0e0' },
        legend: { position: 'bottom' as const, labels: { color: '#a0a0a0' } }
      }
    };
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'DELIVERED': return 'badge-success';
      case 'SHIPPED': case 'IN_TRANSIT': return 'badge-info';
      case 'PROCESSING': case 'PENDING': return 'badge-warning';
      case 'REJECTED': case 'RETURNED': case 'CANCELLED': case 'FAILED': return 'badge-danger';
      default: return 'badge-default';
    }
  }

  getTotalUsers(): number {
    if (!this.adminData?.usersByRole) return 0;
    return Object.values(this.adminData.usersByRole).reduce((a, b) => a + b, 0);
  }
}
