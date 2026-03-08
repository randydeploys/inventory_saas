# CLAUDE.md — Inventory SaaS

## Projet

Application web de gestion d'inventaire multi-tenant pour le secteur entrepôt/logistique. Permet à plusieurs entreprises de gérer leurs bâtiments, zones de stockage et produits avec traçabilité complète des mouvements de stock.

## Stack technique

- **Backend** : Java 17+ / Spring Boot 3.x / Spring Data JPA / Hibernate / PostgreSQL
- **Frontend** : React / Vite / TypeScript / TanStack Query / React Router / ShadcnUI
- **Auth** : Spring Security + JWT (jjwt) — access token 15min + refresh token 7j
- **Validation** : Jakarta Validation (@NotNull, @Size, @Email...)
- **Documentation API** : SpringDoc OpenAPI (Swagger)
- **Migration DB** : Flyway
- **Build** : Maven
- **Tests** : JUnit 5 + Mockito
- **Containerisation** : Docker Compose (app + PostgreSQL)
- **Rate limiting** : Bucket4j (Spring Boot Starter) — sur les endpoints d'auth

## Architecture backend

Architecture MVC en couches. Package racine : `com.inventory`

```
com.inventory/
├── config/          → SecurityConfig, JwtConfig, CorsConfig
├── controller/      → AuthController, BuildingController, ProductController...
├── service/         → AuthService, BuildingService, ProductService...
├── repository/      → UserRepository, BuildingRepository, ProductRepository...
├── model/
│   ├── entity/      → User, Building, Room, Product, StockMovement...
│   ├── dto/         → LoginRequest, ProductResponse, MovementRequest...
│   └── enums/       → Role, TrackingType, MovementType
├── security/        → JwtTokenProvider, JwtAuthFilter, TenantFilter
├── exception/       → GlobalExceptionHandler, ResourceNotFoundException...
└── mapper/          → ProductMapper, BuildingMapper (Entity ↔ DTO) — tous les mappers ici
```

## Modèle de données

### Tenant
- id: UUID (PK)
- name: VARCHAR(255)
- slug: VARCHAR(100) — identifiant URL unique
- created_at, updated_at: TIMESTAMP

### User
- id: UUID (PK)
- tenant_id: UUID (FK → Tenant)
- email: VARCHAR(255) — unique par tenant
- password_hash: VARCHAR(255) — BCrypt
- first_name, last_name: VARCHAR(100)
- role: ENUM (ADMIN, MANAGER, READER)
- is_active: BOOLEAN
- created_at, updated_at: TIMESTAMP

### Building
- id: UUID (PK)
- tenant_id: UUID (FK → Tenant)
- name: VARCHAR(255)
- address: TEXT
- created_at, updated_at: TIMESTAMP
- updated_by: UUID (FK → User, nullable) — dernier utilisateur ayant modifié
- deleted_at: TIMESTAMP (nullable) — null = actif, non-null = archivé

### Room
- id: UUID (PK)
- tenant_id: UUID (FK → Tenant) — dupliqué depuis Building pour isolation directe
- building_id: UUID (FK → Building)
- name: VARCHAR(255)
- description: TEXT
- created_at, updated_at: TIMESTAMP
- updated_by: UUID (FK → User, nullable) — dernier utilisateur ayant modifié
- deleted_at: TIMESTAMP (nullable) — null = actif, non-null = archivé

### Category
- id: UUID (PK)
- tenant_id: UUID (FK → Tenant)
- name: VARCHAR(255)
- color: VARCHAR(7) — hex (#FF5733)
- created_at, updated_at: TIMESTAMP
- deleted_at: TIMESTAMP (nullable) — null = actif, non-null = archivé

### Product
- id: UUID (PK)
- tenant_id: UUID (FK → Tenant)
- category_id: UUID (FK → Category, nullable)
- name: VARCHAR(255)
- sku: VARCHAR(100) — unique par tenant (sur les produits non archivés)
- description: TEXT
- tracking_type: ENUM (QUANTITY, UNIQUE)
- serial_number: VARCHAR(255) — si UNIQUE (null si QUANTITY)
- min_quantity: INTEGER — seuil alerte stock bas sur le total de stock, uniquement pour QUANTITY (null si UNIQUE)
- unit: VARCHAR(50) — pièces, kg, litres... (null si UNIQUE)
- created_at, updated_at: TIMESTAMP
- updated_by: UUID (FK → User, nullable) — dernier utilisateur ayant modifié
- deleted_at: TIMESTAMP (nullable) — null = actif, non-null = archivé

**Note :** `room_id` et `quantity` ne sont plus sur Product — ils sont dans la table `ProductStock` pour permettre le stock multi-room.

### ProductStock
- id: UUID (PK)
- tenant_id: UUID (FK → Tenant) — pour filtrage direct
- product_id: UUID (FK → Product)
- room_id: UUID (FK → Room)
- quantity: INTEGER — toujours 1 pour UNIQUE
- Contrainte unique sur `(product_id, room_id)`

Le stock d'un produit QUANTITY peut être réparti sur plusieurs rooms. Le stock d'un produit UNIQUE a toujours un seul enregistrement (ou zéro si sorti de l'inventaire).
**Cycle de vie :** un enregistrement ProductStock est créé/augmenté lors d'un IN ou TRANSFER entrant, diminué ou supprimé lors d'un OUT ou TRANSFER sortant. Quand `quantity` atteint 0, le record est supprimé. Pas de soft delete — StockMovement est le registre immuable.

### StockMovement
- id: UUID (PK)
- tenant_id: UUID (FK → Tenant)
- product_id: UUID (FK → Product)
- from_room_id: UUID (FK → Room, nullable) — null uniquement si type = IN
- to_room_id: UUID (FK → Room, nullable) — null uniquement si type = OUT
- quantity: INTEGER — toujours 1 si tracking_type = UNIQUE
- type: ENUM (IN, OUT, TRANSFER)
- reason: TEXT
- performed_by: UUID (FK → User)
- created_at: TIMESTAMP

**Contraintes de cohérence des mouvements :**
- IN : `from_room_id` = null, `to_room_id` non-null
- OUT : `from_room_id` non-null, `to_room_id` = null
- TRANSFER : `from_room_id` non-null ET `to_room_id` non-null (et différents)

### RefreshToken
- id: UUID (PK)
- user_id: UUID (FK → User)
- token: VARCHAR(512) — valeur hashée
- expires_at: TIMESTAMP
- revoked: BOOLEAN
- created_at: TIMESTAMP

Les refresh tokens sont stockés en base. Le logout révoque le token en mettant `revoked = true`. Les tokens expirés ou révoqués sont rejetés lors du refresh.

### Relations
- Tenant 1:N → Users, Buildings, Categories, Products, StockMovements
- Building 1:N → Rooms
- Room 1:N → Products
- Category 1:N → Products
- Product 1:N → ProductStock (stock par room)
- Product 1:N → StockMovements
- Room 1:N → ProductStock
- User 1:N → StockMovements (performed_by)
- User 1:N → RefreshTokens
- User 1:N → Buildings/Rooms/Products (updated_by)

## Multi-tenant

Toutes les entités métier portent un `tenant_id`. L'isolation se fait via un `TenantFilter` dans Spring Security qui injecte automatiquement le `tenant_id` extrait du JWT dans toutes les requêtes JPA. Ne jamais exposer de données cross-tenant.

`Room` porte un `tenant_id` redondant (en plus de `building_id`) pour permettre le filtrage direct sans JOIN — toujours filtrer Room par `tenant_id` directement, pas uniquement via Building.

## Rôles et permissions

Trois rôles par tenant :

| Action | Admin | Manager | Reader |
|--------|-------|---------|--------|
| Gérer les utilisateurs (inviter, supprimer, changer rôles) | ✅ | ❌ | ❌ |
| Configurer le tenant | ✅ | ❌ | ❌ |
| CRUD Bâtiments | ✅ | ❌ | ❌ |
| CRUD Zones / Salles | ✅ | ✅ | ❌ |
| CRUD Produits | ✅ | ✅ | ❌ |
| CRUD Catégories | ✅ | ✅ | ❌ |
| Mouvements de stock | ✅ | ✅ | ❌ |
| Consulter inventaire + historique | ✅ | ✅ | ✅ |
| Exporter CSV / PDF | ✅ | ✅ | ✅ |
| Dashboard | — | — | — | (hors scope pour l'instant)

Utiliser `@PreAuthorize` de Spring Security pour contrôler l'accès.

## Format des réponses API

Toutes les réponses utilisent un wrapper `ApiResponse<T>` :

```json
{
  "success": true,
  "message": "Opération réussie",
  "data": { ... },
  "errors": []
}
```

Pour les listes paginées, `data` contient :

```json
{
  "content": [...],
  "page": 0,
  "size": 20,
  "totalElements": 150,
  "totalPages": 8
}
```

Taille de page par défaut : 20. Maximum autorisé : 100.

## Endpoints API

### Auth (public)
- `POST /api/auth/register` → crée tenant + premier user Admin
- `POST /api/auth/login` → retourne access_token + refresh_token
- `POST /api/auth/refresh` → renouvelle l'access token
- `POST /api/auth/logout` → révoque le refresh token (authentifié)

**Rate limiting (Bucket4j)** : `POST /api/auth/login` et `POST /api/auth/register` — max 10 requêtes/minute par IP. Retourne HTTP 429 avec message d'erreur si dépassé.

### Users (Admin seulement sauf GET)
- `GET /api/users` → liste des utilisateurs du tenant (Admin)
- `GET /api/users/{id}` → détail d'un utilisateur (Admin)
- `POST /api/users` → créer un utilisateur (Admin) — body : `{ email, firstName, lastName, role, password }` ; pas d'envoi d'email, l'Admin communique les identifiants manuellement
- `PUT /api/users/{id}/role` → changer le rôle (Admin)
- `PUT /api/users/{id}/deactivate` → désactiver un compte (Admin)
- `DELETE /api/users/{id}` → désactiver un utilisateur (Admin) — équivalent à deactivate ; pas de hard delete pour préserver l'intégrité des StockMovements

### Buildings
- `GET /api/buildings` → liste des bâtiments actifs (tous rôles)
- `GET /api/buildings?archived=true` → liste des bâtiments archivés (Admin)
- `GET /api/buildings/{id}` → détail (tous rôles)
- `POST /api/buildings` → créer (Admin)
- `PUT /api/buildings/{id}` → modifier (Admin)
- `DELETE /api/buildings/{id}` → archiver (Admin) — 409 si des produits actifs existent dans ses rooms (réponse inclut le nombre de produits et de rooms concernés) ; si toutes les rooms sont vides, archive le bâtiment et ses rooms
- `POST /api/buildings/{id}/reassign` → déplacer tous les produits actifs du bâtiment vers une room cible (Admin) — body : `{ "targetRoomId": "uuid" }` ; la room cible doit appartenir au même tenant

### Rooms
- `GET /api/buildings/{id}/rooms` → zones actives d'un bâtiment (tous rôles)
- `GET /api/rooms/{id}` → détail d'une zone (tous rôles)
- `POST /api/buildings/{id}/rooms` → créer (Admin, Manager)
- `PUT /api/rooms/{id}` → modifier (Admin, Manager)
- `DELETE /api/rooms/{id}` → archiver (Admin, Manager) — 409 si des produits actifs existent dans la zone (réponse inclut le nombre de produits concernés) ; si la zone est vide, archive la room
- `POST /api/rooms/{id}/reassign` → déplacer tous les produits actifs de la zone vers une room cible (Admin, Manager) — body : `{ "targetRoomId": "uuid" }` ; la room cible doit appartenir au même tenant et ne pas être la même room

### Categories
- `GET /api/categories` → liste des catégories actives (tous rôles)
- `GET /api/categories/{id}` → détail (tous rôles)
- `POST /api/categories` → créer (Admin, Manager)
- `PUT /api/categories/{id}` → modifier (Admin, Manager)
- `DELETE /api/categories/{id}` → archiver (Admin, Manager) — soft delete ; les produits liés conservent leur category_id mais la catégorie n'apparaît plus dans les listes

### Products
- `GET /api/products` → liste paginée des produits actifs + filtres (tous rôles)
  - Filtres disponibles : `categoryId`, `roomId`, `buildingId`, `trackingType`, `search` (nom ou SKU)
  - `roomId` et `buildingId` filtrent via ProductStock (produits ayant du stock dans cette room/building)
  - La réponse inclut `totalQuantity` (somme du stock sur toutes les rooms) et `stocks: [{ roomId, roomName, quantity }]`
- `GET /api/products/{id}` → détail complet avec liste des stocks par room (tous rôles)
- `POST /api/products` → créer (Admin, Manager) — ne crée pas de ProductStock ; le stock est ajouté via un mouvement IN
- `PUT /api/products/{id}` → modifier métadonnées (Admin, Manager) — ne modifie pas le stock
- `DELETE /api/products/{id}` → archiver (Admin, Manager) — soft delete ; ProductStock et StockMovements restent intacts
- `GET /api/products/low-stock` → produits QUANTITY dont le totalQuantity < min_quantity (tous rôles)

### StockMovements
- `GET /api/movements` → historique paginé (tous rôles)
  - Filtres disponibles : `productId`, `type`, `fromDate`, `toDate`, `performedBy`
- `POST /api/movements` → enregistrer un mouvement (Admin, Manager)
- `GET /api/products/{id}/movements` → historique d'un produit (tous rôles)

### Exports
- `GET /api/exports/products/csv` → export produits CSV (tous rôles)
- `GET /api/exports/products/pdf` → export produits PDF (tous rôles)
- `GET /api/exports/movements/csv` → export mouvements CSV (tous rôles)
- Export mouvements PDF : non prévu

### Tenant
- `GET /api/tenant` → informations du tenant courant (tous rôles)
- `PUT /api/tenant` → modifier le nom/slug du tenant (Admin)

## Flux d'authentification

1. `POST /api/auth/register` → crée le tenant + premier user Admin
2. `POST /api/auth/login` → retourne access_token (JWT 15min) + refresh_token (7j, stocké en base)
3. Chaque requête passe par `JwtAuthFilter` → extrait user + tenant_id du token
4. `TenantFilter` injecte le tenant_id dans le contexte JPA
5. `@PreAuthorize` contrôle l'accès selon le rôle
6. `POST /api/auth/logout` → marque le RefreshToken comme révoqué en base

## Conventions de code

### Backend (Java/Spring)
- Nommage : PascalCase pour classes, camelCase pour méthodes/variables
- Toujours utiliser des DTOs pour les requêtes et réponses — ne jamais exposer les entités directement
- Validation avec annotations Jakarta sur les DTOs (@NotNull, @NotBlank, @Size, @Email)
- Mapper entre Entity et DTO dans le package mapper (pas de mapping dans les controllers) — un mapper par entité
- Repository : étendre JpaRepository, ajouter des query methods typées
- Service : toute la logique métier, jamais dans les controllers
- Controller : uniquement routing + appel service + retour ResponseEntity
- Exceptions : utiliser un GlobalExceptionHandler avec @RestControllerAdvice
- Responses standardisées : ApiResponse<T> wrapper avec success, message, data, errors
- Logs : SLF4J, pas de System.out.println

### Frontend (React/TypeScript)
- Composants fonctionnels uniquement avec hooks
- TanStack Query pour tout appel API — pas de useEffect pour le fetching
- Types TypeScript stricts — pas de `any`
- ShadcnUI pour les composants UI de base
- Axios avec intercepteur pour injecter le JWT et gérer le refresh automatique
- Structure : pages/, components/, hooks/, services/, types/, lib/

## Phases de développement

### Phase 1 — Fondations (Semaines 1-3)
- Setup Spring Boot + PostgreSQL + Flyway
- Docker Compose (dès le départ pour l'environnement de dev)
- Entités : Tenant, User, Building, Room
- Auth complète : register, login, JWT, refresh token (stockage en base)
- CRUD Buildings + Rooms avec filtrage tenant_id
- Spring Security + @PreAuthorize
- GlobalExceptionHandler + DTOs standardisés + ApiResponse<T>
- Tests unitaires services + tests intégration controllers
- Swagger / OpenAPI

### Phase 2 — Coeur métier (Semaines 4-6)
- Product avec double tracking (QUANTITY / UNIQUE)
- ProductStock : table de stock multi-room, gestion création/mise à jour/suppression via mouvements
- CRUD Products avec pagination, filtres, recherche (filtrage via ProductStock)
- Catégories CRUD
- StockMovement : entrée (IN), sortie (OUT), transfert (TRANSFER) — chaque mouvement met à jour ProductStock atomiquement
- Historique mouvements avec filtres
- Validation métier (stock suffisant via ProductStock, zones valides, SKU unique, contraintes TRANSFER)

### Phase 3 — Frontend React (Semaines 7-10)
- Setup Vite + TS + TanStack Query + React Router
- Auth : login/register, stockage JWT, intercepteur Axios
- Layout sidebar avec navigation role-based
- Pages CRUD : Bâtiments, Zones, Produits, Catégories
- Page mouvements : formulaire + historique
- Composants ShadcnUI : DataTable, Dialog, Form, Toast

### Phase 4 — Features avancées (Semaines 11-13)
- Alertes stock bas
- Export CSV (Apache Commons CSV) + PDF (iText)
- Recherche avancée multi-critères
- Gestion utilisateurs Admin (création, rôles, désactivation)
- README avec screenshots

## Commandes utiles

```bash
# Backend
./mvnw spring-boot:run                          # Lancer le backend
./mvnw test                                      # Lancer les tests
./mvnw clean package -DskipTests                 # Build sans tests

# Frontend
npm run dev                                      # Lancer le dev server
npm run build                                    # Build production
npm run lint                                     # Linter

# Docker
docker-compose up -d                             # Lancer app + PostgreSQL
docker-compose down                              # Arrêter
```

## Règles pour Claude Code

- Toujours créer le DTO correspondant quand tu crées une entité
- Toujours créer le mapper correspondant quand tu crées une entité
- Toujours ajouter la migration Flyway quand tu modifies le schéma — ne jamais utiliser `spring.jpa.hibernate.ddl-auto=update`
- Toujours filtrer par `tenant_id` dans les repository queries — Room inclus (pas uniquement via Building)
- Toujours ajouter `@PreAuthorize` sur les endpoints avec le rôle minimum requis
- Toujours écrire les tests unitaires du service quand tu crées un nouveau service
- Ne jamais retourner le `password_hash` dans les DTOs de réponse
- Ne jamais permettre un mouvement de stock si la quantité est insuffisante (vérifier ProductStock.quantity >= quantité demandée)
- Ne jamais permettre un TRANSFER si `from_room_id` ou `to_room_id` est null, ou si les deux sont identiques
- Mouvements et ProductStock : IN → crée ou augmente le ProductStock dans `to_room_id` ; OUT → diminue et supprime le ProductStock si quantity=0 ; TRANSFER → diminue dans `from_room_id` (supprime si 0) et crée ou augmente dans `to_room_id`
- `POST /api/products` ne crée pas de ProductStock — le stock initial est ajouté via un mouvement IN
- `DELETE /api/users/{id}` désactive l'utilisateur (`is_active = false`) — jamais de hard delete pour préserver les StockMovements
- Toujours créer et gérer `ProductStockRepository` et `ProductStockMapper` avec l'entité Product
- Préférer les query methods Spring Data aux `@Query` natives quand c'est possible
- Utiliser `Optional` pour les retours repository et gérer proprement les 404
- Soft delete sur Building, Room, Product, Category via `deleted_at` — toujours filtrer `WHERE deleted_at IS NULL` dans toutes les queries (sauf route `?archived=true`)
- Toujours renseigner `updated_by` (user courant extrait du JWT) lors d'un PUT sur Building, Room, Product
- Rate limiting avec Bucket4j sur `/api/auth/login` et `/api/auth/register` — 10 req/min par IP, retourner HTTP 429 si dépassé
- La suppression d'un Building retourne 409 si des produits actifs existent dans ses rooms — utiliser `POST /api/buildings/{id}/reassign` d'abord ; si vide, archive le building et ses rooms
- La suppression d'une Room retourne 409 si des produits actifs existent — utiliser `POST /api/rooms/{id}/reassign` d'abord ; si vide, archive la room
- `reassign` valide que la room cible appartient au même tenant, est active, et est différente de la source
- La suppression d'une Category ne cascade pas — les produits liés conservent leur `category_id`
- `StockMovement` n'est jamais supprimé ni archivé — c'est un registre immuable
- Unicité du SKU s'applique uniquement sur les produits actifs (`deleted_at IS NULL`)
- Taille de page par défaut : 20, maximum : 100 — rejeter les requêtes dépassant 100
- `min_quantity`, `quantity` et `unit` sont null pour les produits UNIQUE — ne pas les valider ni les afficher
- `serial_number` est null pour les produits QUANTITY — ne pas le valider ni l'afficher
