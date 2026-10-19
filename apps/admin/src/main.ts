import { bootstrapApplication } from '@angular/platform-browser';
import { Component, signal } from '@angular/core';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { IpliShellComponent, IpliThemeService } from '@ipli/ui';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatIconModule } from '@angular/material/icon';

@Component({selector:'app-root',standalone:true,imports:[IpliShellComponent,MatButtonModule,MatButtonToggleModule,MatIconModule],template:`
 <ipli-shell appName="Operations & content admin" subtitle="Bookings · Resources · Pages · Payments">
  <div class="mb-7 flex flex-wrap items-center justify-between gap-4">
    <div><h1 class="text-3xl font-semibold">Workspace</h1><p class="opacity-70">Manage your website, appointments and shop from one place.</p></div>
    <mat-button-toggle-group [value]="theme.current()" (change)="theme.setTheme($event.value)" aria-label="Admin appearance">
      <mat-button-toggle value="light">Light</mat-button-toggle><mat-button-toggle value="dark">Dark</mat-button-toggle><mat-button-toggle value="ocean">Ocean</mat-button-toggle>
    </mat-button-toggle-group>
  </div>
  <section class="grid gap-4 md:grid-cols-3">
   @for (item of sections; track item) {
     <article class="rounded-2xl border border-black/10 bg-white/5 p-6 shadow-sm"><h2 class="text-xl font-semibold">{{item}}</h2><p class="mt-2 text-sm opacity-70">Coming in the next implementation phase</p><button mat-button disabled>Open</button></article>
   }
  </section>
 </ipli-shell>
`})
class AdminApp { readonly sections=['Bookings & calendars','Content editor','Commerce & inventory','Resource planning','Google connections','Reports & monitoring']; constructor(public readonly theme:IpliThemeService) { this.theme.load('admin'); } }
bootstrapApplication(AdminApp,{providers:[provideAnimationsAsync()]}).catch(console.error);
