# Natural Foliage Lab — Full Stack Portfolio Project

React + Spring Boot + MySQL rewrite of a tissue-culture laboratory workflow.

## Stack

- React 19 + Vite
- Spring Boot 3.5 + Java 21
- Spring Data JPA + MySQL
- Spring Security (HTTP Basic for the first learning milestone)
- Maven

## Current MVP

- Login with admin/technician roles
- Dashboard totals
- Plant CRUD
- Media composition CRUD
- Mother bottle registration and status tracking
- Subculture creation with a parent bottle
- Discard registration and status updates
- Responsive dashboard and management tables
- Layered backend: controller → service → repository → entity

## Run locally

### Configure MySQL

- Create a database named `natural_foliage_lab`.
- Copy `backend/src/main/resources/application-local-example.properties` to `application-local.properties` in the same folder and set your credentials.
- Never commit `application-local.properties`.

Start backend:

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Start frontend in another terminal:

```bash
cd frontend
npm install
npm run dev
```

Open http://localhost:5173 and use the seeded development account:

- Username: `admin`
- Password: `ChangeMe123!`

Change this password before any deployment.

## Request flow for the viva

```text
React page → Axios request → REST Controller
                              ↓
                           Service
                              ↓
- MySQL ← JPA Repository ← Entity
```

The controller handles HTTP, the service contains business rules, the repository talks to MySQL, and React renders the returned JSON.

## Next milestones

1. Add validation and centralized error responses.
2. Replace HTTP Basic with JWT access/refresh tokens.
3. Add barcode generation and printing.
4. Add inventory transactions and bottle lineage.
5. Add tests, GitHub Actions, charts, pagination, and deployment.
