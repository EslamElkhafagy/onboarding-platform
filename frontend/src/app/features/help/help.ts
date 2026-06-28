import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth';

/**
 * "How to use the system" guide. Role-aware: new hires see the chat + checklist flow, admins
 * additionally see the document/template/dashboard setup steps. Pure static content — no API.
 */
@Component({
  selector: 'app-help',
  imports: [RouterLink],
  template: `
    <section class="hero">
      <span class="role-badge">{{ auth.isAdmin() ? 'Admin guide' : 'New hire guide' }}</span>
      <h1>How to use the Onboarding Platform</h1>
      <p>
        Chat with an assistant that answers from your company's documents, and work through your
        onboarding checklist — all in one place.
      </p>
    </section>

    @if (auth.isAdmin()) {
      <h2>Setting things up (admin)</h2>
      <div class="steps">
        <div class="step">
          <div class="step-num">1</div>
          <div>
            <h3>Upload your documents</h3>
            <p>
              Go to <a routerLink="/documents">Documents</a> and upload your handbooks, policies,
              and guides. Only <strong>PDF</strong> and <strong>DOCX</strong> files are supported.
            </p>
            <p>Wait until the status turns <span class="status status-ready">READY</span> — that
              means the file has been processed and is searchable in chat.</p>
          </div>
        </div>
        <div class="step">
          <div class="step-num">2</div>
          <div>
            <h3>Build a checklist template</h3>
            <p>
              In <a routerLink="/templates">Templates</a>, create a reusable checklist (e.g. "Week
              1 setup") with the tasks every new hire should complete. Assigning it copies the
              items to that hire, so later edits don't disturb in-progress checklists.
            </p>
          </div>
        </div>
        <div class="step">
          <div class="step-num">3</div>
          <div>
            <h3>Track progress &amp; spot gaps</h3>
            <p>
              The <a routerLink="/dashboard">Dashboard</a> shows each hire's checklist progress and
              the most-asked questions — including ones the assistant couldn't answer, so you know
              which documents to improve.
            </p>
          </div>
        </div>
      </div>
    }

    <h2>{{ auth.isAdmin() ? 'Using the assistant' : 'Getting started' }}</h2>
    <div class="steps">
      <div class="step">
        <div class="step-num">{{ auth.isAdmin() ? '4' : '1' }}</div>
        <div>
          <h3>Ask questions in Chat</h3>
          <p>
            Open <a routerLink="/chat">Chat</a> and ask anything about your onboarding — benefits,
            policies, how-tos. Answers come straight from your company's documents, with the
            <span class="chip">source</span> chips showing exactly which file each answer used.
          </p>
        </div>
      </div>
      <div class="step">
        <div class="step-num">{{ auth.isAdmin() ? '5' : '2' }}</div>
        <div>
          <h3>Ask good questions</h3>
          <p>
            The assistant only answers from the documents — it won't make things up. Ask full
            questions like <em>"How many vacation days do new hires get?"</em> rather than a bare
            greeting. If nothing relevant is found, it'll tell you and point you to HR.
          </p>
        </div>
      </div>
      <div class="step">
        <div class="step-num">{{ auth.isAdmin() ? '6' : '3' }}</div>
        <div>
          <h3>Work through your checklist</h3>
          <p>
            Open <a routerLink="/checklist">My checklist</a> to see your assigned tasks. Tick each
            one as you finish; your progress bar updates automatically.
          </p>
        </div>
      </div>
    </div>

    <div class="callout">
      <span class="ico">💡</span>
      <div>
        <strong>Why did I get "I couldn't find anything"?</strong> The assistant answers only from
        uploaded documents. That message means nothing relevant was found — try rephrasing with
        words likely to appear in the docs, or ask your HR contact to upload the missing
        information. Greetings like <kbd>hi</kbd> won't return an answer by design.
      </div>
    </div>

    <h2>FAQ</h2>
    <dl class="faq card">
      <dt>What file types can be uploaded?</dt>
      <dd>PDF and DOCX. Each upload is processed automatically before it becomes searchable.</dd>
      <dt>Are answers accurate?</dt>
      <dd>Answers are grounded in your documents and cite their sources, so you can verify them.
        If the documents don't cover something, the assistant says so instead of guessing.</dd>
      <dt>Who can see my data?</dt>
      <dd>Everything is scoped to your company — documents, chats, and checklists are never shared
        across companies.</dd>
      @if (auth.isAdmin()) {
        <dt>How do I add a new hire?</dt>
        <dd>Invite them from the admin area; they'll get their own login with access to chat and
          their assigned checklist.</dd>
      }
    </dl>

    <p class="center muted" style="margin-top:1.5rem">
      Ready? <a routerLink="/chat">Head to the chat →</a>
    </p>
  `,
})
export class Help {
  protected readonly auth = inject(AuthService);
}
