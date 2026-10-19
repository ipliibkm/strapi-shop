import { bootstrapApplication } from '@angular/platform-browser';
import { Component,signal } from '@angular/core';
import { provideHttpClient } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { IpliShellComponent, IpliThemeService } from '@ipli/ui';
import { BookingComponent } from './booking.component';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
@Component({selector:'app-root',standalone:true,imports:[IpliShellComponent,BookingComponent,MatButtonModule,MatButtonToggleModule],template:`
 <ipli-shell appName="Operations & content admin" subtitle="Bookings · Resources · Pages · Payments">
 <div class="mb-6 flex flex-wrap items-center justify-between gap-4"><div><h1 class="text-3xl font-semibold">Workspace</h1><p class="opacity-70">Bookings and content operations</p></div>
 <mat-button-toggle-group [value]="theme.current()" (change)="theme.setTheme($event.value)" aria-label="Admin appearance">
 <mat-button-toggle value="light">Light</mat-button-toggle><mat-button-toggle value="dark">Dark</mat-button-toggle><mat-button-toggle value="ocean">Ocean</mat-button-toggle>
 </mat-button-toggle-group></div>
 <ip-bookings/>
 <section class="mt-8 grid gap-4 md:grid-cols-3">@for(section of sections;track section){<article class="rounded-lg border p-5"><h2 class="font-semibold">{{section}}</h2><p class="mt-2 opacity-70">Planned integration</p></article>}</section>
 </ipli-shell>
 `})
class AdminApp {
 readonly sections=['Content editor','Google connection','Shop & payments','Theme publishing','Resources and groups','Monitoring'];
 constructor(public readonly theme:IpliThemeService){this.theme.load('admin');}
}
bootstrapApplication(AdminApp,{providers:[provideAnimationsAsync(),provideHttpClient()]}).catch(console.error);
