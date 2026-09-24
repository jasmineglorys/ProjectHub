# ProjectHub — ACCET Student Project Sharing Website

Full-stack rebuild of the Figma prototype: **React (Vite) + Spring Boot + MySQL**, with JWT
authentication, BCrypt password hashing, role-based access and complete CRUD.

```
project-sharing-website/
├── backend/      Spring Boot 3.3.4 · Java 17 · Spring Web, Data JPA, Security, Validation
├── frontend/     React 18 · Vite · React Router 6 · Axios · plain CSS
└── database/     schema.sql, sample-data.sql
```

---

## 1. Prerequisites

| Tool | Version |
|---|---|
| JDK | 17 or newer |
| Maven | 3.8+ (or use the IDE's bundled Maven) |
| Node.js | 18 or newer |
| MySQL | 8.0 (installed directly — **not** XAMPP) |

---

## 2. Database setup

Start the MySQL service, then:

```bash
mysql -u root -p
```

You do **not** need to create the database by hand — the JDBC URL contains
`createDatabaseIfNotExist=true` and `spring.jpa.hibernate.ddl-auto=update`, so Hibernate
creates `projecthub_db` and all six tables on first startup.

If you prefer to create the schema manually:

```bash
mysql -u root -p < database/schema.sql
```

Then change `spring.jpa.hibernate.ddl-auto=update` to `validate` in
`backend/src/main/resources/application.properties`.

---

## 3. Backend — Spring Boot

Edit `backend/src/main/resources/application.properties` and set your MySQL credentials:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/projecthub_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Kolkata
spring.datasource.username=root
spring.datasource.password=root
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
```

Run it:

```bash
cd backend
mvn spring-boot:run
```

The API starts on **http://localhost:8080**.

On the very first run `DataSeeder` inserts demo accounts and eight approved projects
(set `app.seed.enabled=false` to skip this).

| Role | Email | Password |
|---|---|---|
| Admin | `admin@accet.ac.in` | `admin@2024` |
| Student | `arjun@student.accet.ac.in` | `student123` |
| Student | `priya@student.accet.ac.in` | `student123` |

---

## 4. Frontend — React

```bash
cd frontend
npm install
npm run dev
```

The app starts on **http://localhost:5173**.

`frontend/.env` holds the API base URL:

```
VITE_API_BASE_URL=http://localhost:8080/api
```

Start the backend **first**, then the frontend.

---

## 5. How the layers connect

```
React component
      ↓  (useEffect / event handler)
src/api/*.js          ← projectApi, authApi, adminApi
      ↓
Axios instance (src/api/axiosConfig.js)
   · baseURL = VITE_API_BASE_URL
   · request interceptor attaches "Authorization: Bearer <jwt>"
      ↓  HTTP
@RestController       ← validates the DTO with @Valid
      ↓
@Service              ← business rules, ownership checks, DTO mapping
      ↓
@Repository (Spring Data JPA)
      ↓  Hibernate
MySQL (projecthub_db)
```

**Ports** — backend `8080`, frontend `5173`, MySQL `3306`.

**CORS** — `config/CorsConfig.java` registers a `CorsConfigurationSource` for `/api/**`
allowing the origins listed in `app.cors.allowed-origins`, methods
GET/POST/PUT/PATCH/DELETE/OPTIONS and the `Authorization` header. `SecurityConfig` wires that
source into the filter chain, so pre-flight `OPTIONS` requests succeed.

---

## 6. Authentication flow

1. **Register** — `POST /api/auth/register`. The service checks for a duplicate email and roll
   number, hashes the password with `BCryptPasswordEncoder` (strength 10) and stores only the
   hash in `users.password_hash`.
2. **Login** — `POST /api/auth/login`. `passwordEncoder.matches(raw, hash)` verifies the
   password; on success `JwtService` signs a HS256 token carrying the subject (email), `uid`
   and `role`, valid for 24 hours.
3. **Storage** — the React `AuthContext` saves the token and user in `localStorage`
   (`ph_token`, `ph_user`) and restores the session on refresh, re-validating via
   `GET /api/auth/me`.
4. **Every request** — the Axios interceptor adds `Authorization: Bearer <token>`.
   `JwtAuthenticationFilter` validates it and populates the `SecurityContext`.
5. **Authorisation** — `/api/admin/**` requires `ROLE_ADMIN` (enforced twice: in the filter
   chain and with `@PreAuthorize` on the controller). Editing or deleting a project requires
   ownership or the admin role — checked in `ProjectService`, not in the browser.
6. **Route guards** — `ProtectedRoute` redirects anonymous users to `/login` and blocks
   non-admins from `/admin`.

---

## 7. REST API reference

### Auth
| Method | Endpoint | Access | Purpose |
|---|---|---|---|
| POST | `/api/auth/register` | Public | Create a student account, returns a JWT |
| POST | `/api/auth/login` | Public | Log in, returns a JWT |
| GET | `/api/auth/me` | Authenticated | Current user + liked/bookmarked ids |

### Projects
| Method | Endpoint | Access | Purpose |
|---|---|---|---|
| GET | `/api/projects` | Public | Approved projects; `search`, `department`, `category`, `sort`, `page`, `size` |
| GET | `/api/projects/{id}` | Public | One project, increments the view count |
| POST | `/api/projects` | Student | Submit a project (created as `PENDING`) |
| PUT | `/api/projects/{id}` | Owner/Admin | Update a project |
| DELETE | `/api/projects/{id}` | Owner/Admin | Delete a project |
| GET | `/api/projects/my` | Authenticated | The caller's own submissions, any status |
| GET | `/api/projects/bookmarked` | Authenticated | The caller's saved projects |
| POST | `/api/projects/{id}/like` | Authenticated | Toggle like |
| POST | `/api/projects/{id}/bookmark` | Authenticated | Toggle bookmark |

### Departments & meta
| Method | Endpoint | Access | Purpose |
|---|---|---|---|
| GET | `/api/departments/stats` | Public | Per-department approved counts |
| GET | `/api/meta/departments` | Public | Allowed department values |
| GET | `/api/meta/categories` | Public | Allowed category values |

### Admin
| Method | Endpoint | Access | Purpose |
|---|---|---|---|
| GET | `/api/admin/projects?status=PENDING` | Admin | List by status (`PENDING`/`APPROVED`/`REJECTED`/`ALL`) |
| PATCH | `/api/admin/projects/{id}/status` | Admin | Approve, reject or revoke |
| DELETE | `/api/admin/projects/{id}` | Admin | Delete any project |
| GET | `/api/admin/stats` | Admin | Counts, department/category breakdown, recent activity |

**Status codes** — `200` OK, `201` Created (register, submit), `400` validation or bad input,
`401` missing/invalid token, `403` wrong role or not the owner, `404` not found, `409`
duplicate email or roll number, `500` unexpected. Errors share one JSON shape produced by
`GlobalExceptionHandler`:

```json
{
  "timestamp": "2026-09-21T10:14:33.21",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/projects",
  "fieldErrors": { "title": "Title must be between 5 and 200 characters" }
}
```

---

## 8. Quick API check

```bash
# Log in and capture the token
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"arjun@student.accet.ac.in","password":"student123"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")

# Public browse
curl "http://localhost:8080/api/projects?sort=popular&size=3"

# Authenticated call
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/projects/my
```

---

## 9. Validation summary

**Backend** (`jakarta.validation` on the DTOs, plus service rules): title 5–200 chars,
description 20–4000, at least one technology and one named team member, at most 5 members,
project year 2000–2100, valid email, password ≥ 6 chars, department and category must be in the
allowed lists, unique email and roll number.

**Frontend**: each form validates before it calls the API (per-field messages, wizard steps
validate on `Next`), and every server-side message is surfaced through the `Alert` component —
so nothing depends on the browser alone.

---

## 10. Notes on what changed from the prototype

- Navigation moved from a `useState` page string to React Router routes with real URLs.
- All data comes from the backend; `src/data.ts` and its hardcoded arrays are gone.
- Admin is a role on the `users` table, not a hardcoded credential pair.
- `submittedBy` is taken from the JWT, so a client cannot submit on someone else's behalf.
- Editing your own approved project sends it back to `PENDING` for re-review.
