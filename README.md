# 📦 Inventory SaaS

Application web de gestion d'inventaire multi-tenant pour le secteur entrepôt/logistique. Permet à plusieurs entreprises de gérer leurs bâtiments, zones de stockage et produits avec traçabilité complète des mouvements de stock.

## 🛠 Stack technique

### Backend
- **Java 21** / **Spring Boot 3.x**
- **Spring Security + JWT** (HttpOnly cookies — access token 15min + refresh token 7j)
- **Spring Data JPA / Hibernate** + **PostgreSQL**
- **Flyway** — migrations de base de données versionnées
- **SpringDoc OpenAPI** — documentation Swagger
- **JUnit 5 + Mockito** — 70+ tests (unitaires + intégration)

### Frontend
- **React 18** / **TypeScript** / **Vite**
- **TanStack Query** — gestion du cache et des appels API
- **React Router** — routing SPA
- **Axios** — HTTP client avec intercepteur 401 (refresh automatique)
- **shadcn/ui + Tailwind CSS** — composants UI
- **Sonner** — notifications toast

### Infrastructure
- **Docker Compose** — PostgreSQL containerisé
- **Maven** — build backend

## 🏗 Architecture

### Multi-tenant

Chaque entreprise (tenant) a ses propres données isolées. Le `tenant_id` est extrait du JWT et injecté dans toutes les requêtes.

### Backend — Architecture MVC en couches

```
com.inventory/
├── config/          → SecurityConfig, JwtConfig, CorsConfig, SwaggerConfig
├── controller/      → AuthController, BuildingController, ProductController...
├── service/         → AuthService, BuildingService, StockMovementService...
├── repository/      → Spring Data JPA repositories
├── model/
│   ├── entity/      → User, Building, Room, Product, ProductStock, StockMovement
│   ├── dto/         → Request/Response DTOs
│   └── enums/       → Role, TrackingType, MovementType
├── security/        → JwtTokenProvider, JwtAuthFilter, CookieUtil, SecurityHelper
├── exception/       → GlobalExceptionHandler, ResourceNotFoundException
├── mapper/          → Entity ↔ DTO mappers
├── specification/   → JPA Criteria dynamic filters
└── validation/      → Custom validators (@ValidMovement, @ValidProduct)
```

### Frontend — Structure React

```
src/
├── components/      → UI réutilisables (layout, dialogs, cards)
├── pages/           → Une page par route
├── hooks/           → Custom hooks (TanStack Query wrappers)
├── services/        → Appels API (Axios)
├── types/           → Interfaces TypeScript
├── context/         → AuthContext (état global utilisateur)
└── lib/             → Axios instance, helpers
```

## 📊 Modèle de données

```
Tenant 1:N → Users, Buildings, Categories, Products
Building 1:N → Rooms
Category 1:N → Products
Product 1:N → ProductStock (stock par room)
Product 1:N → StockMovements (historique)
Room 1:N → ProductStock
User 1:N → StockMovements (performed_by)
```

### Produits — Double tracking

- **QUANTITY** : suivi par quantité (pièces, kg, litres). Stock réparti sur plusieurs rooms. Alerte quand `totalQuantity < minQuantity`.
- **UNIQUE** : suivi par numéro de série. Toujours 1 exemplaire. Un seul enregistrement ProductStock.

### Mouvements de stock

| Type | from_room | to_room | Effet sur ProductStock |
|------|-----------|---------|----------------------|
| IN | null | requis | Crée ou augmente le stock |
| OUT | requis | null | Diminue, supprime si qty=0 |
| TRANSFER | requis | requis | Diminue from + augmente to |

## 🔐 Sécurité

### Authentification JWT via HttpOnly Cookies

Les tokens ne sont **jamais** exposés à JavaScript (protection XSS). Ils transitent via des cookies `HttpOnly; Secure; SameSite=Lax`.

```
POST /api/auth/register → crée tenant + admin → émet cookies
POST /api/auth/login    → vérifie credentials → émet cookies
POST /api/auth/refresh  → renouvelle l'access token
POST /api/auth/logout   → révoque le refresh token + supprime cookies
GET  /api/auth/me       → retourne l'utilisateur courant
```

### Rôles et permissions

| Action | Admin | Manager | Reader |
|--------|-------|---------|--------|
| Gérer les utilisateurs | ✅ | ❌ | ❌ |
| CRUD Bâtiments | ✅ | ❌ | ❌ |
| CRUD Zones / Catégories / Produits | ✅ | ✅ | ❌ |
| Mouvements de stock | ✅ | ✅ | ❌ |
| Consulter inventaire + historique | ✅ | ✅ | ✅ |

## 🚀 Démarrage rapide

### Prérequis

- Java 21
- Node.js 18+
- Docker & Docker Compose
- pnpm (frontend)

### 1. Cloner le projet

```bash
git clone https://github.com/randydeploys/inventory_saas.git
cd inventory_saas
```

### 2. Configurer les variables d'environnement

```bash
cp backend/.env.example backend/.env
```

Éditer `backend/.env` :

```env
DB_HOST=localhost
DB_PORT=5433
DB_NAME=inventory_db
DB_USER=postgres
DB_PASSWORD=postgres
JWT_SECRET=votre-cle-secrete-minimum-32-caracteres-de-long!!
CORS_ALLOWED_ORIGINS=http://localhost:5173
```

### 3. Lancer PostgreSQL

```bash
docker-compose up -d
```

### 4. Lancer le backend

```bash
cd backend
./mvnw spring-boot:run
```

Le backend démarre sur `http://localhost:8080`.

### 5. Lancer le frontend

```bash
cd frontend
pnpm install
pnpm dev
```

Le frontend démarre sur `http://localhost:5173`.

### 6. Accéder à l'application

- **App** : http://localhost:5173
- **Swagger** : http://localhost:8080/swagger-ui/index.html

## 🧪 Tests

```bash
cd backend

# Tous les tests
./mvnw test

# Build sans tests
./mvnw clean package -DskipTests
```

**70+ tests** couvrant :
- Tests unitaires services (Mockito) : AuthService, BuildingService, RoomService, ProductService, CategoryService, StockMovementService
- Tests intégration controllers (MockMvc) : Auth, Buildings, Rooms, Products, Categories, StockMovements
- Tests JwtTokenProvider

## 📡 API Endpoints

### Auth (public)
| Méthode | URL | Description |
|---------|-----|-------------|
| POST | `/api/auth/register` | Inscription (crée tenant + admin) |
| POST | `/api/auth/login` | Connexion |
| POST | `/api/auth/refresh` | Renouveler le token |
| POST | `/api/auth/logout` | Déconnexion |
| GET | `/api/auth/me` | Utilisateur courant |

### Buildings (Admin)
| Méthode | URL | Description |
|---------|-----|-------------|
| GET | `/api/buildings` | Liste des bâtiments |
| GET | `/api/buildings/{id}` | Détail d'un bâtiment |
| POST | `/api/buildings` | Créer |
| PUT | `/api/buildings/{id}` | Modifier |
| DELETE | `/api/buildings/{id}` | Archiver (soft delete) |

### Rooms (Admin + Manager)
| Méthode | URL | Description |
|---------|-----|-------------|
| GET | `/api/buildings/{id}/rooms` | Zones d'un bâtiment |
| GET | `/api/rooms/{id}` | Détail d'une zone |
| POST | `/api/buildings/{id}/rooms` | Créer une zone |
| PUT | `/api/rooms/{id}` | Modifier |
| DELETE | `/api/rooms/{id}` | Archiver |

### Categories (Admin + Manager)
| Méthode | URL | Description |
|---------|-----|-------------|
| GET | `/api/categories` | Liste des catégories |
| POST | `/api/categories` | Créer |
| PUT | `/api/categories/{id}` | Modifier |
| DELETE | `/api/categories/{id}` | Archiver |

### Products (Admin + Manager)
| Méthode | URL | Description |
|---------|-----|-------------|
| GET | `/api/products` | Liste paginée + filtres |
| GET | `/api/products/{id}` | Détail avec stocks par room |
| POST | `/api/products` | Créer |
| PUT | `/api/products/{id}` | Modifier |
| DELETE | `/api/products/{id}` | Archiver |
| GET | `/api/products/low-stock` | Produits en stock bas |

### Stock Movements (Admin + Manager)
| Méthode | URL | Description |
|---------|-----|-------------|
| GET | `/api/movements` | Historique paginé + filtres |
| POST | `/api/movements` | Enregistrer un mouvement |
| GET | `/api/products/{id}/movements` | Historique d'un produit |

### Users (Admin uniquement)
| Méthode | URL | Description |
|---------|-----|-------------|
| GET | `/api/users` | Liste des utilisateurs |
| POST | `/api/users` | Créer un utilisateur |
| PUT | `/api/users/{id}/role` | Changer le rôle |
| PUT | `/api/users/{id}/deactivate` | Désactiver |

## 📁 Format des réponses API

Toutes les réponses utilisent un wrapper standardisé :

```json
{
  "success": true,
  "message": "Opération réussie",
  "data": { ... },
  "errors": []
}
```

## 🗃 Migrations

Les migrations Flyway sont versionnées :

| Version | Description |
|---------|-------------|
| V1 | Schema initial (tenant, users, building, room, refresh_token) |
| V2 | Categories |
| V3 | Products, ProductStock, StockMovement |

## 📝 Licence

Ce projet est un projet d'apprentissage personnel.
