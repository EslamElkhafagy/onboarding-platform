import { Component, computed, input, model, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

export interface SelectOption {
  id: string;
  label: string;
  /** Optional secondary text shown muted in the dropdown (e.g. a role). */
  hint?: string;
}

/**
 * Searchable select with a typeahead input and removable chips. Works for single-select
 * (`multiple=false`, default) or multi-select. Two-way bound via `selected` (array of ids;
 * a single-select holds 0 or 1). Self-contained — no external dependencies.
 */
@Component({
  selector: 'app-search-select',
  imports: [FormsModule],
  template: `
    <div class="search-select">
      @if (selected().length) {
        <div class="ss-chips">
          @for (id of selected(); track id) {
            <span class="chip removable">
              {{ labelOf(id) }}
              <button type="button" class="chip-x" (click)="remove(id)" title="Remove">×</button>
            </span>
          }
        </div>
      }

      <div class="ss-field">
        <input
          type="text"
          [ngModel]="query()"
          (ngModelChange)="query.set($event); open.set(true)"
          [ngModelOptions]="{ standalone: true }"
          (focus)="open.set(true)"
          (blur)="closeSoon()"
          (keydown.enter)="selectFirst($event)"
          [placeholder]="placeholder()"
          autocomplete="off"
        />

        @if (open()) {
          <ul class="ss-options">
            @for (o of filtered(); track o.id) {
              <li>
                <button type="button" (mousedown)="add(o.id)">
                  {{ o.label }}
                  @if (o.hint) {
                    <span class="muted ss-hint">{{ o.hint }}</span>
                  }
                </button>
              </li>
            } @empty {
              <li class="muted ss-empty">
                {{ options().length ? 'No matches' : emptyText() }}
              </li>
            }
          </ul>
        }
      </div>
    </div>
  `,
})
export class SearchSelect {
  readonly options = input<SelectOption[]>([]);
  readonly selected = model<string[]>([]);
  readonly multiple = input(false);
  readonly placeholder = input('Search…');
  readonly emptyText = input('Nothing to choose from');

  protected readonly query = signal('');
  protected readonly open = signal(false);

  /** Unselected options whose label or hint matches the query (case-insensitive). */
  protected readonly filtered = computed(() => {
    const q = this.query().trim().toLowerCase();
    const chosen = new Set(this.selected());
    return this.options().filter(
      (o) =>
        !chosen.has(o.id) &&
        (!q || o.label.toLowerCase().includes(q) || (o.hint ?? '').toLowerCase().includes(q)),
    );
  });

  labelOf(id: string): string {
    return this.options().find((o) => o.id === id)?.label ?? id;
  }

  add(id: string): void {
    if (this.multiple()) {
      if (!this.selected().includes(id)) this.selected.set([...this.selected(), id]);
    } else {
      this.selected.set([id]);
      this.open.set(false);
    }
    this.query.set('');
  }

  remove(id: string): void {
    this.selected.set(this.selected().filter((x) => x !== id));
  }

  selectFirst(event: Event): void {
    const first = this.filtered()[0];
    if (first) {
      event.preventDefault();
      this.add(first.id);
    }
  }

  /** Delay closing so a click on an option still registers. */
  closeSoon(): void {
    setTimeout(() => this.open.set(false), 150);
  }
}
