import { inject } from '@angular/core';
import { HttpInterceptorFn } from '@angular/common/http';
import { from, switchMap } from 'rxjs';
import { AdminAuth } from './auth.service';

// Never attach bearer tokens to setup or third-party URLs.
export const adminAuthInterceptor:HttpInterceptorFn=(request,next)=>{
 if(!request.url.startsWith('/api/v1/') || request.url==='/api/v1/auth/config')return next(request);
 const auth=inject(AdminAuth);
 return from(auth.accessToken()).pipe(switchMap(token=>
  next(token?request.clone({setHeaders:{Authorization:'Bearer '+token}}):request)));
};
