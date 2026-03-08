# 001 - Multi-tenant strategy

## Context
L'app gère plusieurs entreprises (tenants) sur la même base de données.

## Options
- A) Un schéma PostgreSQL par tenant — isolation forte, complexité élevée, migrations à jouer N fois
- B) Un tenant_id sur chaque table — simple, scalable, isolation logique

## Decision
Option B — tenant_id sur chaque table.

## Reason
Plus simple à implémenter, suffisant pour un SaaS de cette taille.
Un filtre global Spring garantit l'isolation à chaque requête.
Si un tenant compromet la sécurité, l'impact est limité aux données logiques, pas au schéma.
