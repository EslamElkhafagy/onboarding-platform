import { Component, OnInit, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { ApiService } from '../../core/api';
import { ConversationResponse, MessageView } from '../../models';

@Component({
  selector: 'app-chat',
  imports: [FormsModule, DecimalPipe],
  template: `
    <div class="chat-layout">
      <aside class="chat-sidebar card">
        <button class="full" (click)="newConversation()">+ New conversation</button>
        <ul class="conv-list">
          @for (c of conversations(); track c.id) {
            <li>
              <button
                class="conv-item"
                [class.active]="c.id === conversationId()"
                (click)="openConversation(c.id)"
              >
                {{ c.title || 'Untitled' }}
              </button>
            </li>
          } @empty {
            <li class="muted">No conversations yet.</li>
          }
        </ul>
      </aside>

      <section class="chat-main card">
        <div class="messages">
          @for (m of messages(); track m.id) {
            <div class="msg" [class.user]="m.role === 'USER'" [class.assistant]="m.role === 'ASSISTANT'">
              <div class="bubble">{{ m.content }}</div>
              @if (m.role === 'ASSISTANT' && m.sources.length) {
                <div class="sources">
                  <span class="sources-label">Sources:</span>
                  @for (s of m.sources; track s.chunkId) {
                    <span class="chip">{{ s.filename }} ({{ (s.score * 100) | number: '1.0-0' }}%)</span>
                  }
                </div>
              }
              @if (m.role === 'ASSISTANT' && m.wasAnswered === false) {
                <div class="warn">Not found in your documents — please check with HR.</div>
              }
            </div>
          } @empty {
            <p class="muted center">Ask a question about your onboarding documents.</p>
          }
          @if (sending()) {
            <div class="msg assistant"><div class="bubble muted">Thinking…</div></div>
          }
        </div>

        @if (docScope(); as scope) {
          <div class="scope-banner">
            Asking about <strong>{{ scope.name }}</strong> only.
            <button type="button" class="link-btn" (click)="clearScope()">Ask across all docs</button>
          </div>
        }

        @if (error()) {
          <p class="error">{{ error() }}</p>
        }

        <form class="composer" (ngSubmit)="send()">
          <input
            name="q"
            [(ngModel)]="question"
            placeholder="Type your question…"
            autocomplete="off"
            [disabled]="sending()"
          />
          <button type="submit" [disabled]="sending() || !question.trim()">Send</button>
        </form>
      </section>
    </div>
  `,
})
export class Chat implements OnInit {
  private readonly api = inject(ApiService);
  private readonly route = inject(ActivatedRoute);

  protected readonly conversations = signal<ConversationResponse[]>([]);
  protected readonly messages = signal<MessageView[]>([]);
  protected readonly conversationId = signal<string | undefined>(undefined);
  protected readonly sending = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly docScope = signal<{ id: string; name: string } | null>(null);
  protected question = '';

  ngOnInit(): void {
    this.loadConversations();
    // Arriving from a checklist task's "Ask about this" pre-scopes chat to that document.
    const params = this.route.snapshot.queryParamMap;
    const documentId = params.get('documentId');
    if (documentId) {
      this.docScope.set({ id: documentId, name: params.get('documentName') ?? 'this document' });
      this.newConversation();
    }
  }

  clearScope(): void {
    this.docScope.set(null);
  }

  private loadConversations(): void {
    this.api.listConversations().subscribe({
      next: (cs) => this.conversations.set(cs),
      error: () => {},
    });
  }

  newConversation(): void {
    this.conversationId.set(undefined);
    this.messages.set([]);
    this.error.set(null);
  }

  openConversation(id: string): void {
    this.api.getConversation(id).subscribe({
      next: (detail) => {
        this.conversationId.set(detail.id);
        this.messages.set(detail.messages);
        this.error.set(null);
      },
      error: () => this.error.set('Could not load that conversation.'),
    });
  }

  send(): void {
    const text = this.question.trim();
    if (!text || this.sending()) return;

    this.messages.update((list) => [...list, localUserMessage(text)]);
    this.question = '';
    this.sending.set(true);
    this.error.set(null);

    this.api.ask(text, this.conversationId(), this.docScope()?.id).subscribe({
      next: (res) => {
        this.sending.set(false);
        const wasNew = !this.conversationId();
        this.conversationId.set(res.conversationId);
        this.messages.update((list) => [
          ...list,
          {
            id: res.messageId,
            role: 'ASSISTANT',
            content: res.answer,
            wasAnswered: res.wasAnswered,
            createdAt: new Date().toISOString(),
            sources: res.sources,
          },
        ]);
        if (wasNew) this.loadConversations();
      },
      error: () => {
        this.sending.set(false);
        this.error.set('The assistant could not answer right now. Please try again.');
      },
    });
  }
}

let localId = 0;
function localUserMessage(content: string): MessageView {
  return {
    id: `local-${localId++}`,
    role: 'USER',
    content,
    wasAnswered: null,
    createdAt: new Date().toISOString(),
    sources: [],
  };
}
