# Tissue Culture Laboratory Management System

A full-stack web application built for Natural Foliage's plant tissue-culture workflow. It connects plant and media master data with bottle-level barcodes, lab operations, inventory, staff access, analytics, and sales.

> **Evaluation branch:** `fullstack-rebuild` contains the current React/Java implementation. The default `main` branch is an earlier Python prototype. Review [the finished branch](https://github.com/DileepaNayanajith/tclab-management/tree/fullstack-rebuild) and its [commit history](https://github.com/DileepaNayanajith/tclab-management/commits/fullstack-rebuild/). The branches do not need to be merged for that history to be visible.

## Features

- Plant catalogue with create, list, edit, and delete operations backed by a database.
- Media compositions and prepared-bottle stock tracking.
- Plant initiation with a unique barcode for each bottle and printable labels.
- Parent-to-child subculture tracking, rooting, discards, and plant exits.
- Dashboard inventory and technician/discard analytics with period filters.
- Administrator and technician accounts with per-tab permissions.
- Price list, rooted-plant sales, and printable invoices.
- Optional ESP32-C3 temperature/humidity reading on the dashboard.
- Responsive desktop and mobile interface.

## Technology and architecture

| Layer | Technology | Responsibility |
| --- | --- | --- |
| Frontend | React 19, Vite, Axios | Forms, navigation, reporting, barcode labels |
| API | Spring Boot 3.5, Java 21 | REST endpoints, validation, access rules, lab workflows |
| Persistence | Spring Data JPA | Entity mapping and database access |
| Database | PostgreSQL in production; H2 for development/tests; optional local MySQL | Persistent production data and isolated local testing |

The browser calls `/api/*`; Vite proxies these requests to the Java server during local development. The production Dockerfile builds the React app and serves it from the Spring Boot application.

The central relationships are `Plant 1:N MotherBottle`, `MediaComposition 1:N MotherBottle`, `MotherBottle 1:N Subculture`, and `MediaComposition 1:N Subculture/MediaPreparation`. Plant/media codes and bottle barcodes are unique. A more detailed ERD is included in the mini-project report.

## Run locally (isolated development database)

Requirements: Java 21, Maven, Node.js/npm, and an internet connection for the first dependency installation.

```bash
git clone --branch fullstack-rebuild --single-branch https://github.com/DileepaNayanajith/tclab-management.git
cd tclab-management
npm run dev
```

Open **http://127.0.0.1:5173/**. The root script starts Spring Boot on port 8080 with the `dev` profile and Vite on port 5173. It installs missing frontend packages on the first run. Stop it with `Ctrl+C`.

The `dev` profile uses an **in-memory H2 database**: it seeds an initial admin and a small set of sample lab records, then resets when the backend stops. Use it for evaluation CRUD demonstrations, never for real laboratory data.

Development-only initial login:

- Username: `admin`
- Password: `ChangeMe123!`

`ADMIN_PASSWORD` can override the password **when the admin row is first created**. Set a strong value before any persistent or public deployment. Never publish real staff passwords, database credentials, or sensor tokens.

### Optional persistent local MySQL

Copy `backend/src/main/resources/application-local-example.properties` to `application-local.properties` in the same directory and enter local-only database settings. The copied file is Git-ignored. Start the backend with `mvn spring-boot:run -Dspring-boot.run.profiles=local` from `backend/`, then run `npm install` and `npm run dev` from `frontend/` in another terminal. Create or grant access to the local database as required by your MySQL setup. Production uses PostgreSQL instead.

## Verify the project

```bash
npm test
npm run build
```

`npm test` runs the Spring Boot tests. `npm run build` creates the production frontend bundle. The Plant catalogue is the primary full-CRUD demonstration: create a new test variety, find it in the list, edit it, then delete that **unlinked test row**. Do not use real laboratory records for a delete demonstration.

## Development history and assessment

The `fullstack-rebuild` history records the incremental implementation of CRUD, bottle workflows, access control, analytics, sensor integration, and the responsive UI. Review it with:

```bash
git log --oneline --reverse
```

For the EC4307 evaluation, submit the branch-specific link above, the report, and a camera-on video of no more than five minutes. The report must not exceed ten pages. An evaluator who needs to try a hosted version should receive a **separate demo account and database**, not the live lab administrator credentials or production data. Disclose AI-assisted work according to the course's academic-integrity rules.
