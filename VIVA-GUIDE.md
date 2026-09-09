# Viva Quick Guide

## Project idea

Natural Foliage Lab is a tissue-culture laboratory management system. It replaces paper/manual records with a web application that tracks plants, culture media, mother bottles, subcultures, and discarded cultures.

## Why this stack?

- React builds a reusable, interactive frontend.
- Spring Boot exposes REST APIs and contains the business logic.
- MySQL stores relational lab data reliably.
- Spring Data JPA maps Java objects to database tables.
- Spring Security protects API endpoints and hashes passwords.

## Architecture answer

The project uses a layered architecture:

1. React sends an HTTP request.
2. A REST controller receives the request.
3. A service applies business rules and transactions.
4. A JPA repository reads or writes an entity.
5. MySQL stores the permanent data.
6. The API returns JSON and React updates the UI.

## Important files

- `frontend/src/main.jsx`: React components, state, API calls, and navigation.
- `frontend/src/styles.css`: responsive design.
- `LabApplication.java`: Spring Boot starting point.
- `SecurityConfig.java`: login and route protection.
- `model/`: database entities.
- `repository/`: JPA database access.
- `controller/`: REST endpoints.
- `LabWorkflowService.java`: discard business transaction.
- `application.properties`: database and application configuration.

## Concepts to explain

### Component

A React component is a reusable UI function. `Dashboard`, `Login`, and `CrudPage` are components.

### State

`useState` stores changing UI data such as the logged-in user, selected page, form values, and API results.

### Effect

`useEffect` runs side effects such as loading records from the backend after a page opens.

### REST API

The frontend uses GET to read, POST to create, PUT to update, and DELETE to remove records.

### Entity and repository

An entity represents a MySQL table. A JPA repository provides common database operations without writing ordinary SQL for every action.

### Dependency injection

Spring creates objects such as repositories and passes them into controllers/services through their constructors. This reduces coupling and makes testing easier.

### Transaction

The discard service changes the bottle status and creates a discard record as one transaction. If an error occurs, both changes roll back together.

### Authentication and authorization

Authentication answers “who is the user?” Authorization answers “what can this user do?” The first MVP uses HTTP Basic authentication and ADMIN/TECHNICIAN roles. JWT is the next security milestone.

## Honest limitations to mention

- HTTP Basic is only the first learning version; JWT will replace it.
- Mother/subculture/discard APIs exist, but their complete frontend forms and barcode printing are a next milestone.
- Automated backend tests need Java and Maven installed locally.
- Database migrations, pagination, charts, audit trail, and deployment are planned improvements.

Explaining limitations honestly is better than claiming the system is finished. Describe what works, what you learned, and what you will improve next.
