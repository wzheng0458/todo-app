# TaskFlow – Full-Stack Todo App

**Backend:** Java 17 · Spring Boot 3.3 · Spring Security + JWT · Spring Data JPA · H2 / MySQL
**Frontend:** Vue 3 (Composition API) · Vite · Vue Router · Pinia · Axios

## Features
| Requirement | Where |
|---|---|
| User register & login | `AuthController`, `AuthService`, `LoginView.vue`, `RegisterView.vue` |
| JWT authentication | `JwtUtil`, `JwtAuthFilter`, `SecurityConfig` (stateless, BCrypt passwords) |
| CRUD + RESTful API | `TodoController`, `TodoService` (each user only sees their own todos) |
| Database | H2 file DB by default; MySQL via `--spring.profiles.active=mysql` |
| Search, filter, sort, pagination | `GET /api/todos` query params, JPA `Specification` |
| File upload | One attachment per todo (5 MB, extension whitelist, UUID file names) |
| Application logging | `logback-spring.xml` (console + rolling `logs/` files) + `RequestLoggingFilter` |
| Responsive UI | `styles.css` – mobile-first, bottom-sheet modal on phones |
| Validation & errors | Bean Validation + `GlobalExceptionHandler` (consistent JSON errors) |

## Run it

### 1. Backend (needs JDK 17+ and Maven)
```bash
cd backend
mvn spring-boot:run
```
API runs on http://localhost:8080.
Database: MySQL (`tododb`). Run with the `mysql` profile:
DB_USERNAME=todo DB_PASSWORD=yourpassword mvn spring-boot:run -Dspring-boot.run.profiles=mysql

**Use MySQL instead:**
```sql
CREATE DATABASE tododb CHARACTER SET utf8mb4;
```
```bash
DB_USERNAME=root DB_PASSWORD=yourpass mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```

**Set a real JWT secret** (32+ characters) before deploying:
```bash
export JWT_SECRET="a-long-random-string-at-least-32-characters"
```

### 2. Frontend (needs Node 18+)
```bash
cd frontend
npm install
npm run dev
```
Open http://localhost:5173. Dev server proxies `/api` to port 8080.
For a separately hosted production build, set `VITE_API_URL` (see `.env.example`) and add your site to `app.cors.allowed-origins`.

## API reference
All `/api/todos/**` endpoints need `Authorization: Bearer <token>`.

| Method | Path | Description |
|---|---|---|
| POST | `/api/auth/register` | `{username, email, password}` → `{token, username, email}` |
| POST | `/api/auth/login` | `{username, password}` → `{token, ...}` |
| GET | `/api/todos` | List. Params: `q`, `completed`, `priority` (LOW/MEDIUM/HIGH), `sortBy` (createdAt, dueDate, title, priority, completed), `direction` (asc/desc), `page`, `size` |
| GET | `/api/todos/{id}` | Get one |
| POST | `/api/todos` | Create `{title, description?, priority?, dueDate?}` |
| PUT | `/api/todos/{id}` | Update |
| PATCH | `/api/todos/{id}/toggle` | Flip completed |
| DELETE | `/api/todos/{id}` | Delete (also removes its file) |
| POST | `/api/todos/{id}/attachment` | multipart field `file` |
| GET | `/api/todos/{id}/attachment` | Download |
| DELETE | `/api/todos/{id}/attachment` | Remove file |

Quick test:
```bash
TOKEN=$(curl -s -X POST localhost:8080/api/auth/register -H 'Content-Type: application/json' \
  -d '{"username":"demo","email":"demo@example.com","password":"secret1"}' | jq -r .token)
curl -X POST localhost:8080/api/todos -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' -d '{"title":"Buy milk","priority":"HIGH"}'
curl "localhost:8080/api/todos?q=milk&sortBy=dueDate&direction=asc" -H "Authorization: Bearer $TOKEN"
```

## Project layout
```
backend/src/main/java/com/example/todo/
  config/      SecurityConfig (filter chain, CORS, BCrypt)
  controller/  AuthController, TodoController
  service/     AuthService, TodoService, FileStorageService
  security/    JwtUtil, JwtAuthFilter, CustomUserDetailsService, RequestLoggingFilter
  model/       User, Todo, Priority
  repository/  UserRepository, TodoRepository
  dto/         request/response records
  exception/   GlobalExceptionHandler
frontend/src/
  api/         axios instance (JWT interceptor) + todo calls
  stores/      Pinia auth store
  router/      routes + auth guard
  views/       Login, Register, Todos
  components/  TodoModal (create/edit + upload)
```

## Production notes
- Move the JWT secret to an environment variable and use MySQL/PostgreSQL.
- Set `spring.jpa.hibernate.ddl-auto=validate` and use Flyway/Liquibase for migrations.
- The JWT is kept in `localStorage` for simplicity; use httpOnly cookies if XSS risk is a concern.
