# 004 - Spring Boot 3 + Java 17

## Context
Le backend a besoin d'un framework web robuste avec un écosystème mature
pour l'authentification, l'accès base de données et la validation.

## Options
- A) Spring Boot 3 + Java 17 — écosystème massif, très demandé en entreprise, verbeux
- B) Node.js + Express — léger, rapide à prototyper, écosystème fragmenté
- C) Python + FastAPI — moderne, performant, moins de libs enterprise-grade

## Decision
Option A — Spring Boot 3 avec Java 17.

## Reason
Spring Boot est le framework backend le plus demandé sur le marché Java.
Spring Security fournit JWT, RBAC et multi-tenant out of the box.
Java 17 est la version LTS active, requise par Spring Boot 3.
Objectif portfolio : démontrer une compétence backend enterprise-grade.