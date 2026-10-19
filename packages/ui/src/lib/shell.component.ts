import { Component, input } from '@angular/core';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatIconModule } from '@angular/material/icon';
@Component({selector:'ipli-shell',standalone:true,imports:[MatToolbarModule,MatIconModule],template:`
 <div class="min-h-screen"><mat-toolbar class="!h-20 !bg-transparent !px-8"><mat-icon class="mr-3">widgets</mat-icon><span class="font-semibold">{{appName()}}</span><span class="ml-auto text-sm opacity-70">{{subtitle()}}</span></mat-toolbar>
 <main class="mx-auto max-w-7xl px-6 py-10"><ng-content /></main></div>
`})
export class IpliShellComponent { appName=input.required<string>();subtitle=input<string>(''); }
