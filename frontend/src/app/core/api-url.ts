import { environment } from '../../environments/environment';

/** Base of every backend call. Dev: '/api' (proxied). Prod: '<Render backend URL>/api'. */
export const API = `${environment.apiUrl}/api`;
