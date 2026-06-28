import { Component, OnInit, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { ApiService } from '../../core/api';
import { openBlobInNewTab } from '../../core/download';
import { ChecklistItemResponse, ChecklistResponse, DocumentRef } from '../../models';

@Component({
  selector: 'app-checklist',
  imports: [],
  template: `
    <h1>My checklist</h1>

    @if (loading()) {
      <p class="muted">Loading…</p>
    } @else if (!checklists().length) {
      <p class="muted">No checklist has been assigned to you yet.</p>
    }

    @for (cl of checklists(); track cl.id) {
      <div class="card checklist">
        <div class="progress-row">
          <strong>{{ cl.completedItems }} / {{ cl.totalItems }} done</strong>
          <span>{{ pct(cl) }}%</span>
        </div>
        <div class="progress-bar"><span [style.width.%]="pct(cl)"></span></div>

        <ul class="items">
          @for (item of cl.items; track item.id) {
            <li [class.done]="item.completed">
              <label>
                <input
                  type="checkbox"
                  [checked]="item.completed"
                  [disabled]="busyItem() === item.id"
                  (change)="toggle(cl, item)"
                />
                <span class="item-title">{{ item.title }}</span>
                @if (item.dueDay != null) {
                  <span class="due">day {{ item.dueDay }}</span>
                }
              </label>
              @if (item.description) {
                <p class="item-desc">{{ item.description }}</p>
              }
              @if (item.documents.length) {
                <div class="item-docs">
                  @for (d of item.documents; track d.id) {
                    <span class="doc-ref">
                      <span class="doc-name">📄 {{ d.filename }}</span>
                      <button type="button" class="link-btn" (click)="openDoc(d)">Open</button>
                      <button type="button" class="link-btn" (click)="askAbout(d)">Ask about this</button>
                    </span>
                  }
                </div>
              }
            </li>
          }
        </ul>
      </div>
    }
  `,
})
export class Checklist implements OnInit {
  private readonly api = inject(ApiService);
  private readonly router = inject(Router);

  protected readonly checklists = signal<ChecklistResponse[]>([]);
  protected readonly loading = signal(true);
  protected readonly busyItem = signal<string | null>(null);

  ngOnInit(): void {
    this.load();
  }

  /** Opens an attached study document in a new tab. */
  openDoc(doc: DocumentRef): void {
    this.api.downloadDocument(doc.id).subscribe({
      next: (blob) => openBlobInNewTab(blob),
      error: () => {},
    });
  }

  /** Jumps to the chat pre-scoped to this document. */
  askAbout(doc: DocumentRef): void {
    this.router.navigate(['/chat'], {
      queryParams: { documentId: doc.id, documentName: doc.filename },
    });
  }

  /** Percentage complete, derived from the item counts. */
  pct(cl: ChecklistResponse): number {
    return cl.totalItems ? Math.round((cl.completedItems / cl.totalItems) * 100) : 0;
  }

  private load(): void {
    this.loading.set(true);
    this.api.myChecklists().subscribe({
      next: (cls) => {
        this.checklists.set(cls);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  toggle(checklist: ChecklistResponse, item: ChecklistItemResponse): void {
    const next = !item.completed;
    this.busyItem.set(item.id);
    this.api.setItemCompleted(item.id, next).subscribe({
      next: () => {
        this.busyItem.set(null);
        this.applyToggle(checklist.id, item.id, next);
      },
      error: () => this.busyItem.set(null),
    });
  }

  /** Optimistically recompute the parent checklist's counters after a successful toggle. */
  private applyToggle(checklistId: string, itemId: string, completed: boolean): void {
    this.checklists.update((lists) =>
      lists.map((cl) => {
        if (cl.id !== checklistId) return cl;
        const items = cl.items.map((it) =>
          it.id === itemId
            ? { ...it, completed, completedAt: completed ? new Date().toISOString() : null }
            : it,
        );
        const completedItems = items.filter((it) => it.completed).length;
        return { ...cl, items, completedItems };
      }),
    );
  }
}
