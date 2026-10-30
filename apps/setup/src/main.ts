import { provideHttpClient } from '@angular/common/http';
import { bootstrapApplication } from '@angular/platform-browser';
import { SetupComponent } from './setup/setup.component';

bootstrapApplication(SetupComponent, { providers: [provideHttpClient()] }).catch(console.error);
