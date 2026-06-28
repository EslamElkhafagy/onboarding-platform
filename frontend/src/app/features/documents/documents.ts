import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ApiService } from '../../core/api';
import { AuthService } from '../../core/auth';
import { openBlobInNewTab } from '../../core/download';
import { DocumentResponse } from '../../models';

@Component({
  selector: 'app-documents',
  imports: [DatePipe],
  template: `
    <h1>Documents</h1>
    @if (!isAdmin()) {
      <p class="muted">Study materials shared by your team. Open one to read it, or ask the assistant about it from your checklist.</p>
    }

    @if (isAdmin()) {
      <div class="card uploader">
        <input #fileInput type="file" accept=".pdf,.docx" (change)="onPick(fileInput.files)" [disabled]="uploading()" />
        @if (uploading()) {
          <span class="muted">Uploading &amp; ingesting…</span>
        }
        @if (error()) {
          <span class="error">{{ error() }}</span>
        }
      </div>
    }

    @if (loading()) {
      <p class="muted">Loading…</p>
    }

    <table class="card table">
      <thead>
        <tr><th>File</th><th>Status</th><th>Uploaded</th><th></th></tr>
      </thead>
      <tbody>
        @for (doc of documents(); track doc.id) {
          <tr>
            <td>{{ doc.filename }}</td>
            <td><span class="status status-{{ doc.status.toLowerCase() }}">{{ doc.status }}</span>
              @if (doc.status === 'FAILED' && doc.errorMessage) {
                <span class="muted"> — {{ doc.errorMessage }}</span>
              }
            </td>
            <td>{{ doc.createdAt | date: 'medium' }}</td>
            <td>
              <button class="link-btn" [disabled]="busyId() === doc.id" (click)="open(doc)">Open</button>
              @if (isAdmin()) {
                <button class="link-btn danger" [disabled]="busyId() === doc.id" (click)="remove(doc)">
                  Delete
                </button>
              }
            </td>
          </tr>
        } @empty {
          <tr><td colspan="4" class="muted center">No documents yet.</td></tr>
        }
      </tbody>
    </table>
  `,
})
export class Documents implements OnInit {
  private readonly api = inject(ApiService);
  private readonly auth = inject(AuthService);

  protected readonly isAdmin = this.auth.isAdmin;
  protected readonly documents = signal<DocumentResponse[]>([]);
  protected readonly loading = signal(true);
  protected readonly uploading = signal(false);
  protected readonly busyId = signal<string | null>(null);
  protected readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.load();
  }

  private load(): void {
    this.loading.set(true);
    this.api.listDocuments().subscribe({
      next: (docs) => {
        this.documents.set(docs);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  open(doc: DocumentResponse): void {
    this.busyId.set(doc.id);
    this.api.downloadDocument(doc.id).subscribe({
      next: (blob) => {
        this.busyId.set(null);
        openBlobInNewTab(blob);
      },
      error: () => {
        this.busyId.set(null);
        this.error.set('Could not open that document.');
      },
    });
  }

  onPick(files: FileList | null): void {
    const file = files?.[0];
    if (!file) return;
    this.uploading.set(true);
    this.error.set(null);
    this.api.uploadDocument(file).subscribe({
      next: () => {
        this.uploading.set(false);
        this.load();
      },
      error: (err) => {
        this.uploading.set(false);
        this.error.set(err?.error?.error?.message ?? 'Upload failed.');
      },
    });
  }

  remove(doc: DocumentResponse): void {
    if (!confirm(`Delete "${doc.filename}"? This also removes its chunks and citations.`)) return;
    this.busyId.set(doc.id);
    this.api.deleteDocument(doc.id).subscribe({
      next: () => {
        this.busyId.set(null);
        this.documents.update((list) => list.filter((d) => d.id !== doc.id));
      },
      error: () => this.busyId.set(null),
    });
  }
}
