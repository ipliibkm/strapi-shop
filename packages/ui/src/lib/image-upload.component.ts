import { Component, ElementRef, input, output, viewChild } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

/** Translated text for the image upload control. */
export interface OpenShopfrontImageUploadLabels {
  title?: string;
  help?: string;
  choose?: string;
  remove?: string;
  uploading?: string;
  limitReached?: string;
}

/** Describes why a selected file was not accepted by the image picker. */
export interface OpenShopfrontImageUploadError {
  reason: 'file-too-large' | 'unsupported-type' | 'too-many-files';
  fileName?: string;
}

/** A reusable, presentational image picker with previews and drag-and-drop support. */
@Component({
  selector: 'openshopfront-image-upload',
  standalone: true,
  imports: [MatIconModule],
  templateUrl: './image-upload.component.html',
  styleUrls: ['./image-upload.component.css'],
})
export class OpenShopfrontImageUploadComponent {
  readonly images = input<string[]>([]);
  readonly labels = input<OpenShopfrontImageUploadLabels>({});
  readonly maxImages = input(8);
  readonly maxFileSizeBytes = input(8 * 1024 * 1024);
  readonly uploading = input(false);

  readonly filesSelected = output<File[]>();
  readonly removeImage = output<number>();
  readonly selectionRejected = output<OpenShopfrontImageUploadError>();

  private readonly fileInput = viewChild<ElementRef<HTMLInputElement>>('fileInput');

  remaining(): number {
    return Math.max(0, this.maxImages() - this.images().length);
  }

  openPicker(): void {
    if (!this.uploading() && this.remaining() > 0) this.fileInput()?.nativeElement.click();
  }

  onFiles(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.emitFiles(input.files);
    input.value = '';
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    if (!this.uploading()) this.emitFiles(event.dataTransfer?.files ?? null);
  }

  allowDrop(event: DragEvent): void {
    if (!this.uploading() && this.remaining() > 0) event.preventDefault();
  }

  private emitFiles(files: FileList | null): void {
    if (!files || this.remaining() === 0) return;
    const selected = Array.from(files);
    const errors: OpenShopfrontImageUploadError[] = [];
    const valid = selected.filter((file) => {
      if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) {
        errors.push({ reason: 'unsupported-type', fileName: file.name });
        return false;
      }
      if (file.size > this.maxFileSizeBytes()) {
        errors.push({ reason: 'file-too-large', fileName: file.name });
        return false;
      }
      return true;
    });
    const accepted = valid.slice(0, this.remaining());
    if (valid.length > this.remaining()) {
      errors.push({ reason: 'too-many-files' });
    }
    if (accepted.length) this.filesSelected.emit(accepted);
    for (const error of errors) this.selectionRejected.emit(error);
  }
}
