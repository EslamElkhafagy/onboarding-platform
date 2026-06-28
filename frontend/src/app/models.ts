// Shapes mirror the backend DTO records (see ARCHITECTURE.md "API contracts").

export type Role = 'ADMIN' | 'NEW_HIRE';

export interface User {
  id: string;
  email: string;
  fullName: string | null;
  role: Role;
  startDate?: string | null;
}

export interface AuthResponse {
  token: string;
  user: User;
}

export interface InviteResponse {
  user: User;
  token: string;
  expiresAt: string;
}

export interface InvitePreview {
  email: string;
  fullName: string | null;
  companyName: string;
}

export type DocumentStatus = 'UPLOADED' | 'PROCESSING' | 'READY' | 'FAILED';

export interface DocumentResponse {
  id: string;
  filename: string;
  mimeType: string;
  status: DocumentStatus;
  errorMessage: string | null;
  createdAt: string;
}

export interface SourceView {
  documentId: string;
  filename: string;
  chunkId: string;
  score: number;
}

export interface AnswerResponse {
  conversationId: string;
  messageId: string;
  answer: string;
  wasAnswered: boolean;
  sources: SourceView[];
}

export interface ConversationResponse {
  id: string;
  title: string | null;
  createdAt: string;
}

export interface MessageView {
  id: string;
  role: 'USER' | 'ASSISTANT';
  content: string;
  wasAnswered: boolean | null;
  createdAt: string;
  sources: SourceView[];
}

export interface ConversationDetailResponse {
  id: string;
  title: string | null;
  createdAt: string;
  messages: MessageView[];
}

export interface DocumentRef {
  id: string;
  filename: string;
}

export interface TemplateItemResponse {
  id: string;
  title: string;
  description: string | null;
  dueDay: number | null;
  position: number;
  documents: DocumentRef[];
}

export interface TemplateResponse {
  id: string;
  name: string;
  createdAt: string;
  items: TemplateItemResponse[];
}

export interface ChecklistItemResponse {
  id: string;
  title: string;
  description: string | null;
  dueDay: number | null;
  position: number;
  completed: boolean;
  completedAt: string | null;
  documents: DocumentRef[];
}

export interface ChecklistResponse {
  id: string;
  templateId: string | null;
  assignedAt: string;
  totalItems: number;
  completedItems: number;
  items: ChecklistItemResponse[];
}

export interface ProgressResponse {
  userId: string;
  fullName: string | null;
  email: string;
  checklistCount: number;
  totalItems: number;
  completedItems: number;
  percentComplete: number;
}

export interface QuestionCount {
  question: string;
  count: number;
}

export interface Gap {
  question: string;
  askedAt: string;
}

export interface InsightsResponse {
  totalQuestions: number;
  unansweredCount: number;
  topQuestions: QuestionCount[];
  recentGaps: Gap[];
}

/** Error envelope rendered by the backend GlobalExceptionHandler. */
export interface ApiErrorBody {
  error: { code: string; message: string };
}
