import { Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { BookingApi, AvailabilityConflict, type Booking, type BookingRequest } from './booking-api.service';
@Component({standalone:true,selector:'ip-bookings',imports:[MatButtonModule,MatCardModule,MatFormFieldModule,MatInputModule],template:`
 <mat-card class="p-6"><mat-card-header><mat-card-title>Booking management</mat-card-title><mat-card-subtitle>Development API. Sign in before you use it.</mat-card-subtitle></mat-card-header>
 <mat-card-content>
 <div class="mt-5 flex flex-wrap gap-4">
 <mat-form-field><mat-label>Resource</mat-label><input matInput #resource value="room-1"></mat-form-field>
 <mat-form-field><mat-label>Start (UTC)</mat-label><input matInput #start value="2026-12-01T10:00:00Z"></mat-form-field>
 <mat-form-field><mat-label>End (UTC)</mat-label><input matInput #end value="2026-12-01T11:00:00Z"></mat-form-field>
 </div>
 <div class="flex flex-wrap gap-3"><button mat-flat-button [disabled]="busy()" (click)="refresh(resource.value,start.value,end.value)">Check availability</button>
 <button mat-stroked-button [disabled]="busy()||!free()" (click)="book(resource.value,start.value,end.value)">Create booking</button></div>
 @if (notice()) {<p role="alert" class="mt-4 rounded-md border p-3">{{notice()}}</p>}
 @if (free()) {<p class="mt-3">This time is available now. Availability can change before you book.</p>}
 @if (bookings().length) {<ul class="mt-4">@for(item of bookings();track item.id){<li class="border-b py-2">{{item.startsAt}} – {{item.endsAt}} · {{item.status}}</li>}</ul>}
 </mat-card-content></mat-card>
 `})
export class BookingComponent {
 private api=inject(BookingApi);
 busy=signal(false);free=signal(false);notice=signal('');bookings=signal<Booking[]>([]);
 async refresh(resourceId:string,startsAt:string,endsAt:string) {
  this.busy.set(true);this.free.set(false);this.notice.set('');
  try {const a=await this.api.availability({resourceId,startsAt,endsAt});this.free.set(a.available);this.notice.set(a.available?'This time is available.':'This time is not available. Select another time.');
   this.bookings.set(await this.api.list(resourceId,startsAt,endsAt));
  } catch {this.notice.set('The app cannot check availability. Check the server connection and sign-in status.');}
  finally {this.busy.set(false);}
 }
 async book(resourceId:string,startsAt:string,endsAt:string) {
  this.busy.set(true);this.free.set(false);
  try {const r=await this.api.create({resourceId,startsAt,endsAt});this.notice.set('Booking created: '+r.id);}
  catch(e) {
   if(e instanceof AvailabilityConflict){await this.refresh(resourceId,startsAt,endsAt);this.notice.set(e.message+(this.free()?' The time appears available again. Check it before you try again.':''));}
   else this.notice.set('The app could not create the booking. Check the server connection and sign-in status.');
  }finally{this.busy.set(false);}
 }
}
