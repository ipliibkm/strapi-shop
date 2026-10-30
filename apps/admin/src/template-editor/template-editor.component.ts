import {
  Component,
  EventEmitter,
  Input,
  OnChanges,
  OnDestroy,
  OnInit,
  Output,
  ViewChild,
  afterNextRender,
  inject,
  signal,
} from '@angular/core';
import { AdminI18n } from '../shared/i18n.service';

/** One insertable variable with a sample value used for preview substitution. */
export interface TemplateVariable {
  name: string;
  sample?: string | null;
}

type EditorMode = 'editor' | 'html' | 'preview';

/**
 * WYSIWYG HTML template editor with source view, live preview and variable insert chips.
 * The parent keeps the saved value; external changes win over local edits while focused typing
 * is never overwritten.
 */
@Component({
  selector: 'ip-template-editor',
  standalone: true,
  templateUrl: './template-editor.component.html',
  styleUrls: ['./template-editor.component.css'],
})
export class TemplateEditorComponent implements OnInit, OnChanges {
  /** Initial content. Later external changes are applied when they differ from the current one. */
  @Input() template = '';
  /** Variables offered as insert chips and used to substitute sample values in the preview. */
  @Input() variables: TemplateVariable[] = [];

  @Output() readonly templateChange = new EventEmitter<string>();

  @ViewChild('editable') editableEl?: { nativeElement: HTMLElement };
  @ViewChild('source') sourceEl?: { nativeElement: HTMLTextAreaElement };

  readonly mode = signal<EditorMode>('editor');
  private content = '';
  private destroyed = false;
  readonly i18n = inject(AdminI18n);

  /** Translates one interface string for the active admin language. */
  t(text: string, params?: Record<string, string | number>): string {
    return this.i18n.t(text, params);
  }

  /** Runs a callback after the next change detection flush completes. */
  private afterRender(callback: () => void): void {
    if (this.destroyed) return;
    afterNextRender(() => {
      if (!this.destroyed) callback();
    });
  }

  ngOnInit(): void {
    this.content = this.template ?? '';
    const editable = this.editableEl?.nativeElement;
    if (editable && !editable.innerHTML.trim()) editable.innerHTML = this.content;
  }

  ngOnChanges(): void {
    // External loads (initial value, seeded default, reset) win over local edits.
    const editable = this.editableEl?.nativeElement;
    if (!editable || document.activeElement === editable) return;
    if ((this.template ?? '') !== this.content) {
      editable.innerHTML = this.template ?? '';
      this.content = this.template ?? '';
    }
  }

  ngOnDestroy(): void {
    this.destroyed = true;
  }

  switchMode(next: EditorMode): void {
    if (this.mode() === next) return;
    this.mode.set(next);
    this.afterRender(() => {
      const source = this.sourceEl?.nativeElement;
      if (next === 'html' && source) source.value = this.content;
      else if (next === 'editor') {
        const el = this.editableEl?.nativeElement;
        if (el && !document.activeElement) el.innerHTML = this.content;
      }
    });
  }

  /** Applies one formatting command to the focused editor surface. */
  exec(command: string): void {
    const editable = this.editableEl?.nativeElement;
    if (!editable) return;
    if (this.mode() !== 'editor') {
      this.switchMode('editor');
      this.afterRender(() => this.exec(command));
      return;
    }
    editable.focus();
    document.execCommand(command);
    this.syncFromEditable();
  }

  /** Inserts one variable token at the cursor position. */
  insertVariable(name: string): void {
    const token = `{${name}}`;
    if (this.mode() !== 'editor') {
      this.switchMode('editor');
      this.afterRender(() => this.insertVariable(name));
      return;
    }
    const editable = this.editableEl?.nativeElement;
    if (!editable) return;
    editable.focus();
    document.execCommand('insertText', false, token);
    this.syncFromEditable();
  }

  onSourceInput(event: Event): void {
    this.content = (event.target as HTMLTextAreaElement).value ?? '';
    this.templateChange.emit(this.content);
  }

  syncFromEditable(): void {
    const editable = this.editableEl?.nativeElement;
    if (!editable) return;
    this.content = editable.innerHTML;
    this.templateChange.emit(this.content);
  }

  /** The preview document: template with sample values substituted, in an email-like shell. */
  previewHtml(): string {
    if (!this.content.trim()) {
      return shell(
        '<p class="te-empty">The template is empty. The built-in text mail is used instead.</p>',
      );
    }
    return shell(this.substituteSamples(this.content));
  }

  private substituteSamples(html: string): string {
    let out = html ?? '';
    for (const variable of this.variables) {
      if (!variable?.name) continue;
      const sample = variable.sample == null ? '' : String(variable.sample);
      out = out.split(`{${variable.name}}`).join(sample);
    }
    return out;
  }
}

function shell(body: string): string {
  return (
    '<!doctype html><html><head><meta charset="utf-8" />' +
    '<style>body{font-family:Arial,Helvetica,sans-serif;max-width:560px;margin:24px auto;padding:0 16px;color:#1f2937;line-height:1.5}' +
    'h1,h2,h3{line-height:1.25}.te-empty{color:#6b7280;font-style:italic}</style></head><body>' +
    body +
    '</body></html>'
  );
}
