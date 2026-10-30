import { inject } from '@angular/core';
import { HttpInterceptorFn } from '@angular/common/http';
import { AdminAuth } from './auth.service';

// Authentication endpoints use Angular's cookie interceptor to avoid a dependency cycle.
export const adminAuthInterceptor: HttpInterceptorFn = (request, next) => {
  if (!request.url.startsWith('/api/v1/') || request.url.startsWith('/api/v1/auth/'))
    return next(request);
  if (['GET', 'HEAD', 'OPTIONS'].includes(request.method)) return next(request);
  const auth = inject(AdminAuth);
  const csrf = auth.csrf();
  return next(csrf ? request.clone({ setHeaders: { [csrf.headerName]: csrf.token } }) : request);
};
