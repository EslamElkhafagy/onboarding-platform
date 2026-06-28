import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { ApiService } from '../../core/api';
import { InsightsResponse, ProgressResponse } from '../../models';

@Component({
  selector: 'app-dashboard',
  imports: [DecimalPipe, DatePipe],
  template: `
    <h1>Dashboard</h1>

    <section>
      <h2>Onboarding progress</h2>
      <table class="card table">
        <thead>
          <tr><th>Hire</th><th>Completed</th><th>Progress</th></tr>
        </thead>
        <tbody>
          @for (p of progress(); track p.userId) {
            <tr>
              <td>{{ p.fullName || p.email }}</td>
              <td>{{ p.completedItems }} / {{ p.totalItems }}</td>
              <td class="progress-cell">
                <div class="progress-bar"><span [style.width.%]="p.percentComplete"></span></div>
                <span>{{ p.percentComplete | number: '1.0-0' }}%</span>
              </td>
            </tr>
          } @empty {
            <tr><td colspan="3" class="muted center">No hires with checklists yet.</td></tr>
          }
        </tbody>
      </table>
    </section>

    @if (insights(); as ins) {
      <section class="insights">
        <h2>Question insights</h2>
        <div class="stat-row">
          <div class="card stat"><span class="num">{{ ins.totalQuestions }}</span><span>questions asked</span></div>
          <div class="card stat"><span class="num">{{ ins.unansweredCount }}</span><span>unanswered</span></div>
        </div>

        <div class="two-col">
          <div class="card">
            <h3>Top questions</h3>
            <ul class="plain">
              @for (q of ins.topQuestions; track q.question) {
                <li><span class="count">{{ q.count }}×</span> {{ q.question }}</li>
              } @empty {
                <li class="muted">No questions yet.</li>
              }
            </ul>
          </div>
          <div class="card">
            <h3>Recent gaps <span class="muted">(unanswered)</span></h3>
            <ul class="plain">
              @for (g of ins.recentGaps; track $index) {
                <li>
                  {{ g.question }}
                  <span class="muted" style="font-size:0.8rem">· {{ g.askedAt | date: 'short' }}</span>
                </li>
              } @empty {
                <li class="muted">No gaps — every question was answered.</li>
              }
            </ul>
          </div>
        </div>
      </section>
    }
  `,
})
export class Dashboard implements OnInit {
  private readonly api = inject(ApiService);

  protected readonly progress = signal<ProgressResponse[]>([]);
  protected readonly insights = signal<InsightsResponse | null>(null);

  ngOnInit(): void {
    this.api.progress().subscribe({ next: (p) => this.progress.set(p), error: () => {} });
    this.api.insights().subscribe({ next: (i) => this.insights.set(i), error: () => {} });
  }
}
