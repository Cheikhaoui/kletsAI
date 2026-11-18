/**
 * Development: empty apiUrl, so calls go to /api on the dev server, which proxies them to
 * http://localhost:8080 (src/proxy.conf.json). Replaced by environment.prod.ts in production builds.
 */
export const environment = {
  production: false,
  apiUrl: '',
};
