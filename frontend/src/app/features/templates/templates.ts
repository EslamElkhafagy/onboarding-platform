import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/api';
import { DocumentResponse, TemplateResponse, User } from '../../models';

@Component({
  selector: 'app-templates',
  imports: [FormsModule],
  template: `
    <h1>Checklist templates</h1>
    <p class="muted">
      Create a template, add its tasks, then assign it to a hire — that copies the items into
      their personal checklist, which they'll see under <strong>My checklist</strong>.
    </p>

    <div class="card">
      <h3>Invite a new hire</h3>
      <form class="inline-form" (ngSubmit)="invite()">
        <input [(ngModel)]="inviteName" name="inviteName" placeholder="Full name" required />
        <input [(ngModel)]="inviteEmail" name="inviteEmail" type="email" placeholder="Email" required />
        <input [(ngModel)]="inviteStart" name="inviteStart" type="date" class="narrow" title="Start date (optional)" />
        <button type="submit" [disabled]="!inviteName.trim() || !inviteEmail.trim()">Invite</button>
      </form>
      @if (inviteMsg()) {
        <p class="warn" style="color:var(--ok)">{{ inviteMsg() }}</p>
      }
      @if (inviteLink()) {
        <p class="muted" style="margin-bottom:0.3rem">
          Share this link so they can set a password (no email is sent yet):
        </p>
        <div class="inline-form">
          <input [value]="inviteLink()" readonly (focus)="$any($event.target).select()" />
          <button type="button" (click)="copyLink()">{{ copied() ? 'Copied!' : 'Copy' }}</button>
        </div>
      }
    </div>

    <form class="card inline-form" (ngSubmit)="createTemplate()">
      <input name="name" [(ngModel)]="newName" placeholder="New template name" required />
      <button type="submit" [disabled]="!newName.trim()">Create</button>
    </form>

    @if (error()) {
      <p class="error">{{ error() }}</p>
    }

    @if (loading()) {
      <p class="muted">Loading…</p>
    }

    @for (t of templates(); track t.id) {
      <div class="card template">
        <h2>{{ t.name }}</h2>
        <ul class="items">
          @for (item of t.items; track item.id) {
            <li>
              <span class="item-title">{{ item.title }}</span>
              @if (item.dueDay != null) {
                <span class="due">day {{ item.dueDay }}</span>
              }
              @if (item.description) {
                <p class="item-desc">{{ item.description }}</p>
              }
              @if (item.documents.length) {
                <div class="sources">
                  <span class="sources-label">Study docs:</span>
                  @for (d of item.documents; track d.id) {
                    <span class="chip">{{ d.filename }}</span>
                  }
                </div>
              }
            </li>
          } @empty {
            <li class="muted">No items yet.</li>
          }
        </ul>

        <form class="inline-form" (ngSubmit)="addItem(t)">
          <input [(ngModel)]="draft(t.id).title" name="title-{{ t.id }}" placeholder="Item title" required />
          <input [(ngModel)]="draft(t.id).description" name="desc-{{ t.id }}" placeholder="Description (optional)" />
          <input
            [(ngModel)]="draft(t.id).dueDay"
            name="due-{{ t.id }}"
            type="number"
            min="0"
            placeholder="Due day"
            class="narrow"
          />
          @if (documents().length) {
            <select
              multiple
              [(ngModel)]="draft(t.id).docIds"
              name="docs-{{ t.id }}"
              class="narrow"
              title="Attach study documents (optional; Ctrl/Cmd-click for multiple)"
            >
              @for (d of documents(); track d.id) {
                <option [ngValue]="d.id">{{ d.filename }}</option>
              }
            </select>
          }
          <button type="submit" [disabled]="!draft(t.id).title.trim()">Add item</button>
        </form>

        <div class="inline-form" style="margin-top:0.6rem; border-top:1px solid var(--border); padding-top:0.8rem">
          <span class="muted">Assign to:</span>
          <select [(ngModel)]="assignee[t.id]" name="assign-{{ t.id }}" [disabled]="!t.items.length">
            <option [ngValue]="undefined" disabled selected>Choose a person…</option>
            @for (u of users(); track u.id) {
              <option [ngValue]="u.id">{{ u.fullName || u.email }} ({{ u.role === 'ADMIN' ? 'Admin' : 'Hire' }})</option>
            }
          </select>
          <button type="button" (click)="assign(t)" [disabled]="!assignee[t.id] || !t.items.length">Assign</button>
          @if (!t.items.length) {
            <span class="muted" style="font-size:0.82rem">Add at least one item first.</span>
          }
        </div>
        @if (assignMsg()[t.id]) {
          <p class="warn" style="color:var(--ok); margin-top:0.4rem">{{ assignMsg()[t.id] }}</p>
        }
      </div>
    } @empty {
      @if (!loading()) {
        <p class="muted">No templates yet. Create one above.</p>
      }
    }
  `,
})
export class Templates implements OnInit {
  private readonly api = inject(ApiService);

  protected readonly templates = signal<TemplateResponse[]>([]);
  protected readonly users = signal<User[]>([]);
  protected readonly documents = signal<DocumentResponse[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly assignMsg = signal<Record<string, string>>({});
  protected readonly inviteMsg = signal<string | null>(null);
  protected readonly inviteLink = signal<string | null>(null);
  protected readonly copied = signal(false);

  protected newName = '';
  protected inviteName = '';
  protected inviteEmail = '';
  protected inviteStart = '';
  protected assignee: Record<string, string | undefined> = {};

  private readonly drafts = new Map<
    string,
    { title: string; description: string; dueDay: number | null; docIds: string[] }
  >();

  ngOnInit(): void {
    this.load();
    this.loadUsers();
    this.loadDocuments();
  }

  /** Per-template form model, created lazily so each card has its own input state. */
  draft(templateId: string) {
    let d = this.drafts.get(templateId);
    if (!d) {
      d = { title: '', description: '', dueDay: null, docIds: [] };
      this.drafts.set(templateId, d);
    }
    return d;
  }

  private load(): void {
    this.loading.set(true);
    this.api.listTemplates().subscribe({
      next: (ts) => {
        this.templates.set(ts);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  private loadUsers(): void {
    this.api.listUsers().subscribe({ next: (us) => this.users.set(us), error: () => {} });
  }

  private loadDocuments(): void {
    this.api.listDocuments().subscribe({ next: (ds) => this.documents.set(ds), error: () => {} });
  }

  createTemplate(): void {
    const name = this.newName.trim();
    if (!name) return;
    this.error.set(null);
    this.api.createTemplate(name).subscribe({
      next: (t) => {
        this.newName = '';
        this.templates.update((list) => [...list, t]);
      },
      error: (err) => this.error.set(err?.error?.error?.message ?? 'Could not create template.'),
    });
  }

  addItem(template: TemplateResponse): void {
    const d = this.draft(template.id);
    const title = d.title.trim();
    if (!title) return;
    this.api
      .addTemplateItem(template.id, {
        title,
        description: d.description.trim() || undefined,
        dueDay: d.dueDay ?? undefined,
        documentIds: d.docIds.length ? d.docIds : undefined,
      })
      .subscribe({
        next: (item) => {
          this.templates.update((list) =>
            list.map((t) => (t.id === template.id ? { ...t, items: [...t.items, item] } : t)),
          );
          this.drafts.set(template.id, { title: '', description: '', dueDay: null, docIds: [] });
        },
        error: (err) => this.error.set(err?.error?.error?.message ?? 'Could not add item.'),
      });
  }

  assign(template: TemplateResponse): void {
    const userId = this.assignee[template.id];
    if (!userId) return;
    const who = this.users().find((u) => u.id === userId);
    this.api.assignChecklist(template.id, userId).subscribe({
      next: () => {
        const name = who?.fullName || who?.email || 'the hire';
        this.setAssignMsg(template.id, `Assigned to ${name}. They'll see it under My checklist.`);
        this.assignee[template.id] = undefined;
      },
      error: (err) =>
        this.setAssignMsg(template.id, err?.error?.error?.message ?? 'Could not assign.'),
    });
  }

  invite(): void {
    const name = this.inviteName.trim();
    const email = this.inviteEmail.trim();
    if (!name || !email) return;
    this.inviteMsg.set(null);
    this.inviteLink.set(null);
    this.copied.set(false);
    this.api.inviteHire(email, name, this.inviteStart || undefined).subscribe({
      next: (res) => {
        this.inviteMsg.set(
          `Invited ${res.user.fullName || res.user.email}. You can now assign a checklist to them.`,
        );
        this.inviteLink.set(
          `${window.location.origin}/accept-invite?token=${encodeURIComponent(res.token)}`,
        );
        this.inviteName = '';
        this.inviteEmail = '';
        this.inviteStart = '';
        this.loadUsers();
      },
      error: (err) => {
        this.inviteMsg.set(null);
        this.error.set(err?.error?.error?.message ?? 'Could not invite hire.');
      },
    });
  }

  copyLink(): void {
    const link = this.inviteLink();
    if (!link) return;
    navigator.clipboard?.writeText(link).then(
      () => {
        this.copied.set(true);
        setTimeout(() => this.copied.set(false), 2000);
      },
      () => {},
    );
  }

  private setAssignMsg(templateId: string, msg: string): void {
    this.assignMsg.update((m) => ({ ...m, [templateId]: msg }));
  }
}
