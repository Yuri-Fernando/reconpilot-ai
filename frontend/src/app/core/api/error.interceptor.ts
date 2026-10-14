import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';
import { ApiError } from '../models/api.models';

export const apiErrorInterceptor: HttpInterceptorFn = (request, next) =>
  next(request).pipe(
    catchError((error: HttpErrorResponse) => {
      const body = error.error as Partial<ApiError> | null;
      const normalized: ApiError = {
        status: error.status,
        code: body?.code ?? 'NETWORK_OR_API_ERROR',
        detail: body?.detail ?? (error.status === 0 ? 'API indisponível. Verifique se o backend está em execução.' : error.message),
        traceId: body?.traceId,
      };
      return throwError(() => normalized);
    }),
  );
