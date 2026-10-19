import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
export interface Booking { id:string; resourceId:string; startsAt:string; endsAt:string; status:string; }
export interface Availability { available:boolean; resourceId:string; startsAt:string; endsAt:string; }
export interface BookingRequest {resourceId:string; startsAt:string; endsAt:string;}
export class AvailabilityConflict extends Error { constructor(public readonly correlationId:string){ super('The slot is no longer available. The list has been refreshed. Please choose again.'); } }
@Injectable({providedIn:'root'})
export class BookingApi {
 private http=inject(HttpClient);
 // Same-origin API reverse proxy required. Do not put credentials into frontend builds.
 availability(r:BookingRequest):Promise<Availability> {return firstValueFrom(this.http.get<Availability>('/api/v1/availability',{params:{...r}}));}
 list(resourceId:string,from:string,to:string):Promise<Booking[]> {return firstValueFrom(this.http.get<Booking[]>('/api/v1/admin/bookings',{params:{resourceId,from,to}}));}
 async create(r:BookingRequest):Promise<Booking> {
  try {return await firstValueFrom(this.http.post<Booking>('/api/v1/admin/bookings',r));}
  catch(error) {const e=error as HttpErrorResponse; if(e.status===409 && e.error?.code==='AVAILABILITY_CONFLICT')throw new AvailabilityConflict(String(e.error.correlationId??''));throw error;}
 }
}
