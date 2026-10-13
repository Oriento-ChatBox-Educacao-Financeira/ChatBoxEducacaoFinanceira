import { Pipe, PipeTransform, inject } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { marked } from 'marked';
import DOMPurify from 'dompurify';

@Pipe({
  name: 'markdown',
  standalone: true,
  pure: true,
})
export class MarkdownPipe implements PipeTransform {
  private readonly sanitizer = inject(DomSanitizer);

  constructor() {
    marked.setOptions({
      gfm: true,
      breaks: true,
    });
  }

  transform(value: string | null | undefined): SafeHtml {
    if (!value) {
      return '';
    }

    const rawHtml = marked.parse(value, { async: false }) as string;

    const cleanHtml = DOMPurify.sanitize(rawHtml, {
      ALLOWED_TAGS: [
        'p',
        'br',
        'strong',
        'b',
        'em',
        'i',
        'u',
        's',
        'code',
        'pre',
        'blockquote',
        'ul',
        'ol',
        'li',
        'a',
        'h1',
        'h2',
        'h3',
        'h4',
        'h5',
        'h6',
        'hr',
        'span',
        'table',
        'thead',
        'tbody',
        'tr',
        'th',
        'td',
      ],
      ALLOWED_ATTR: ['href', 'title', 'target', 'rel', 'class'],
    });

    return this.sanitizer.bypassSecurityTrustHtml(cleanHtml);
  }
}
