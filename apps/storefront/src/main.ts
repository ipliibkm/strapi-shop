import { bootstrapApplication } from '@angular/platform-browser';
import { Component } from '@angular/core';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { MatButtonModule } from '@angular/material/button';
import { IpliShellComponent,IpliThemeService } from '@ipli/ui';
@Component({selector:'app-root',standalone:true,imports:[IpliShellComponent,MatButtonModule],template:`
 <ipli-shell appName="Your Brand" subtitle="Composable CMS · Shop · Booking">
 <div class="mx-auto max-w-3xl py-16 text-center"><span class="rounded-full border px-3 py-1 text-xs font-medium">Built with Angular Material + Tailwind</span>
 <h1 class="mt-6 text-5xl font-bold tracking-tight">One platform. Every customer touchpoint.</h1>
 <p class="mt-6 text-lg opacity-70">Content, products and bookings — fully customizable.</p>
 <div class="mt-8 flex justify-center gap-4"><button mat-flat-button disabled>Shop products (coming soon)</button><button mat-stroked-button disabled>Book a slot (coming soon)</button></div></div>
 </ipli-shell>
`})
class StorefrontApp { constructor(private readonly theme:IpliThemeService){theme.load('storefront');} }
bootstrapApplication(StorefrontApp,{providers:[provideAnimationsAsync()]}).catch(console.error);
