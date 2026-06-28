import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  AnswerResponse,
  ChecklistResponse,
  ConversationDetailResponse,
  ConversationResponse,
  DocumentResponse,
  InsightsResponse,
  InviteResponse,
  ProgressResponse,
  TemplateItemResponse,
  TemplateResponse,
  User,
} from '../models';

/** Thin typed wrapper over the backend REST API. Tenant scoping is enforced server-side. */
@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);

  // Documents
  listDocuments(): Observable<DocumentResponse[]> {
    return this.http.get<DocumentResponse[]>('/api/documents');
  }

  uploadDocument(file: File): Observable<DocumentResponse> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<DocumentResponse>('/api/documents', form);
  }

  deleteDocument(id: string): Observable<void> {
    return this.http.delete<void>(`/api/documents/${id}`);
  }

  /** Fetches a document's file as a blob (auth header is attached by the interceptor). */
  downloadDocument(id: string): Observable<Blob> {
    return this.http.get(`/api/documents/${id}/content`, { responseType: 'blob' });
  }

  // Chat
  ask(question: string, conversationId?: string, documentId?: string): Observable<AnswerResponse> {
    return this.http.post<AnswerResponse>('/api/chat/ask', { question, conversationId, documentId });
  }

  listConversations(): Observable<ConversationResponse[]> {
    return this.http.get<ConversationResponse[]>('/api/conversations');
  }

  getConversation(id: string): Observable<ConversationDetailResponse> {
    return this.http.get<ConversationDetailResponse>(`/api/conversations/${id}`);
  }

  // Checklists
  myChecklists(): Observable<ChecklistResponse[]> {
    return this.http.get<ChecklistResponse[]>('/api/checklists/me');
  }

  setItemCompleted(itemId: string, completed: boolean): Observable<unknown> {
    return this.http.patch(`/api/checklists/items/${itemId}`, { completed });
  }

  assignChecklist(templateId: string, userId: string): Observable<ChecklistResponse> {
    return this.http.post<ChecklistResponse>('/api/checklists/assign', { templateId, userId });
  }

  // Templates (admin)
  listTemplates(): Observable<TemplateResponse[]> {
    return this.http.get<TemplateResponse[]>('/api/checklist-templates');
  }

  createTemplate(name: string): Observable<TemplateResponse> {
    return this.http.post<TemplateResponse>('/api/checklist-templates', { name });
  }

  addTemplateItem(
    templateId: string,
    item: {
      title: string;
      description?: string;
      dueDay?: number;
      position?: number;
      documentIds?: string[];
    },
  ): Observable<TemplateItemResponse> {
    return this.http.post<TemplateItemResponse>(`/api/checklist-templates/${templateId}/items`, item);
  }

  // Admin dashboard
  progress(): Observable<ProgressResponse[]> {
    return this.http.get<ProgressResponse[]>('/api/admin/progress');
  }

  insights(): Observable<InsightsResponse> {
    return this.http.get<InsightsResponse>('/api/admin/insights');
  }

  listUsers(): Observable<User[]> {
    return this.http.get<User[]>('/api/admin/users');
  }

  inviteHire(email: string, fullName: string, startDate?: string): Observable<InviteResponse> {
    return this.http.post<InviteResponse>('/api/auth/invite', { email, fullName, startDate });
  }
}
