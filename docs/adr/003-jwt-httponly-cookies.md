# 003 - JWT via HttpOnly Cookies

## Context
L'application nécessite une authentification stateless par JWT.
Les tokens doivent être transportés entre le frontend React et le backend Spring Boot à chaque requête.

## Options

- **A) localStorage** — Stocker accessToken et refreshToken dans le localStorage du navigateur.
  Simple à implémenter, mais vulnérable aux attaques XSS : un script malveillant peut lire `localStorage` et exfiltrer les tokens.

- **B) HttpOnly Cookies** — Le backend émet les tokens via `Set-Cookie` avec les flags `HttpOnly; Secure; SameSite=Lax`.
  JavaScript ne peut pas lire ces cookies. Protection XSS native. Nécessite `withCredentials: true` côté Axios et `allowCredentials(true)` côté CORS.

- **C) Mémoire JavaScript + HttpOnly Cookie pour le refresh** — L'accessToken vit en mémoire React (jamais persisté), le refreshToken en cookie HttpOnly. Très sécurisé mais complexe : silent refresh au démarrage, file d'attente des requêtes pendant le refresh.

## Decision
Option B — HttpOnly Cookies pour les deux tokens.

## Reason
Protection XSS complète dès le départ sans complexité supplémentaire.
L'Option C est plus fine mais introduit une gestion d'état complexe côté frontend (silent refresh, queue de requêtes) inutile à ce stade.
`SameSite=Lax` suffit à couvrir la protection CSRF pour les navigateurs modernes.

## Détails d'implémentation

```
Set-Cookie: accessToken=<jwt>;    HttpOnly; Secure; SameSite=Lax; Path=/api;       Max-Age=900
Set-Cookie: refreshToken=<token>; HttpOnly; Secure; SameSite=Lax; Path=/api/auth;  Max-Age=604800
```

- `Path=/api/auth` sur le refreshToken → envoyé uniquement aux endpoints `/api/auth/*` (refresh + logout)
- Le refreshToken est stocké hashé en base (`RefreshToken` table) — révoqué à la déconnexion
- `JwtAuthFilter` lit le JWT depuis le cookie `accessToken`, pas depuis le header `Authorization`
- En développement : le flag `Secure` peut être désactivé pour HTTP local
