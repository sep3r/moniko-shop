# Security Hardening Notes (Moniko Shop)

## Changes applied

### Frontend dependency updates (addresses Runflare / GHSA findings)
- Upgraded `vite` from 5.4.x → ^6.4.5 (fixes multiple high/moderate CVEs affecting the dev server: path traversal, server.fs.deny bypass, launch-editor issues, esbuild, etc.)
- Upgraded `react-router-dom` from 6.x → ^7.18.4 (fixes GHSA-337j-9hxr-rhxg and related)
- Upgraded `axios` to latest ^1.13+
- Forced `source-map-js` ≥ 1.2.2 and `follow-redirects` ≥ 1.16.1 via overrides
- Removed `package-lock.json` so Docker/`npm install` resolves to current patched versions matching the ranges in `package.json`

**Action required after pull:**  
```bash
cd frontend
rm -rf node_modules package-lock.json
npm install
npm run build   # verify
```

Most Vite-related advisories only affect the **development server** (especially on Windows or when exposing `--host`). Production builds are static files served by nginx.

### Nginx security headers
Added to both `nginx.conf` and `nginx.compose.conf`:
- X-Frame-Options: SAMEORIGIN
- X-Content-Type-Options: nosniff
- X-XSS-Protection
- Referrer-Policy
- Permissions-Policy
- server_tokens off
- Deny access to dotfiles

### Backend
- Default admin password is now taken from env `ADMIN_PASSWORD` (fallback: `Admin@Moniko2026!`). Change it before first deploy.
- docker-compose.yml now documents and prefers env vars for `POSTGRES_PASSWORD`, `JWT_SECRET`, `ADMIN_PASSWORD`.
- JWT secret and DB credentials already come from environment variables.

## Production recommendations
1. Generate a strong unique `JWT_SECRET` (≥ 64 random bytes).
2. Use a strong unique Postgres password and never commit real secrets.
3. Prefer Docker secrets / platform secret management (Runflare secrets, etc.) over plain env.
4. Enable HTTPS and HSTS at the reverse-proxy / CDN level.
5. Restrict CORS_ALLOWED_ORIGINS to your real production domains.
6. Regularly run `npm audit` / Dependabot and keep Spring Boot + dependencies updated.
7. Change the seeded admin password immediately after first login.

## Remaining notes
- CSRF is disabled (standard for pure JWT SPA APIs). Ensure tokens are stored safely (httpOnly cookies would be even better long-term).
- Rate limiting / account lockout for login is not implemented; consider adding for public deployments.
