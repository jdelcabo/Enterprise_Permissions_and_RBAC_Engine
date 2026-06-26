# Enterprise Permissions & RBAC Engine

A backend service that answers a single, focused question: **"Does User X have permission to do Action Y?"**

This project implements a Role-Based Access Control (RBAC) engine with **role inheritance**, modeled as a directed graph and resolved with a depth-first search traversal. It was built end-to-end using **Test-Driven Development**, a clean **layered architecture**, and a modern Java backend stack — as a portfolio project demonstrating production-grade backend engineering practices.

---

## Table of Contents

- [Overview](#overview)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [The Core Algorithm: Permission Resolution via DFS](#the-core-algorithm-permission-resolution-via-dfs)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
- [API Reference](#api-reference)
- [Running Tests](#running-tests)
- [Caching Strategy](#caching-strategy)
- [Database Schema](#database-schema)
- [Development Workflow](#development-workflow)
- [Future Improvements](#future-improvements)

---

## Overview

In most organizations, permissions aren't flat — they're hierarchical. A `Manager` should have every permission an `Employee` has, plus their own. An `Employee` should have every permission an `Intern` has, plus their own. Modeling this naively (copying permissions onto every role) doesn't scale and becomes inconsistent the moment a permission changes.

This engine instead models roles as **vertices in a directed graph**, where an edge represents "inherits from." Checking whether a role has a permission means traversing that graph outward from the role in question — a textbook graph traversal problem applied to a real backend concern.

```
        CEO            → "fire_anybody"
         ↑ inherits
      Manager           → "approve_budget"
         ↑ inherits
      Employee          → "submit_expenses"
         ↑ inherits
       Intern            → "read_wiki"
```

A `CEO` resolves `"read_wiki"` as `true` by climbing the inheritance chain down to `Intern`. An `Intern` resolves `"fire_anybody"` as `false`, because no path exists upward from `Intern` to `CEO`.

---

## Tech Stack

| Concern | Technology |
|---|---|
| Language | Java 17 |
| Build Tool | Gradle (Kotlin-free, Groovy DSL) |
| Testing | Spock Framework 2.3 (Groovy 3.0.21) |
| Web Framework | SparkJava 2.9.4 |
| Database | PostgreSQL 15 |
| ORM / Query Builder | jOOQ 3.18.7 (type-safe SQL, code-generated from schema) |
| Caching | Redis 7 (via Jedis 5.1.0) |
| JSON Serialization | Gson 2.10.1 |
| Containerization | Docker & Docker Compose |
| Packaging | Shadow plugin (fat JAR) |

---

## Architecture

The project follows a strict **layered architecture**. Each layer depends only on the layer directly beneath it, and no layer skips ahead:

```
┌─────────────────────────────────────────┐
│  web/         HTTP routes (SparkJava)     │  → translates HTTP requests into service calls
├─────────────────────────────────────────┤
│  service/     Business logic               │  → PermissionService, RoleHierarchy, caching decorator
├─────────────────────────────────────────┤
│  repository/  Data access (jOOQ)           │  → translates SQL rows into domain objects
├─────────────────────────────────────────┤
│  model/       Domain objects                │  → Role (pure data, no framework dependencies)
└─────────────────────────────────────────┘
              ↓                    ↓
        PostgreSQL              Redis
```

**Why this matters:** the `service` layer has zero knowledge of HTTP or SQL. It was built and fully tested in Phase 1 using nothing but plain Java objects — no database, no web server. When the `repository` layer was added in Phase 2, and the `web` layer in Phase 3, **not a single line of `service` code changed.** That's the architectural payoff being demonstrated here.

---

## The Core Algorithm: Permission Resolution via DFS

`PermissionService` answers permission questions using a recursive **Depth-First Search** over the role inheritance graph:

```java
public boolean hasPermission(Role role, String permission) {
    return dfs(role, permission, new HashSet<>());
}

private boolean dfs(Role current, String permission, Set<Role> visited) {
    if (visited.contains(current)) return false;   // cycle guard
    visited.add(current);

    if (current.hasPermission(permission)) return true;   // base case

    for (Role parent : current.getInheritsFrom()) {
        if (dfs(parent, permission, visited)) return true;  // recursive case
    }
    return false;   // exhausted all paths
}
```

Two design details worth highlighting:

- **Cycle protection** — the `visited` set guards against accidentally circular inheritance (e.g., A inherits from B, B inherits from A), preventing infinite recursion.
- **A second traversal, `getAllPermissions()`**, reuses the identical pattern but accumulates results instead of short-circuiting — used to answer "what can this role do, in total?"

---

## Project Structure

```
src/
├── main/java/com/rbac/
│   ├── Main.java                  ← composition root: wires every layer together
│   ├── model/
│   │   └── Role.java
│   ├── service/
│   │   ├── PermissionChecker.java       (interface — enables the caching decorator)
│   │   ├── PermissionService.java       (DFS traversal)
│   │   ├── CachedPermissionService.java (Redis-backed decorator)
│   │   └── RoleHierarchy.java           (graph storage & wiring)
│   ├── repository/
│   │   └── RoleRepository.java          (jOOQ-based data access)
│   └── web/
│       └── App.java                     (SparkJava routes)
├── test/groovy/com/rbac/
│   ├── model/RoleSpec.groovy
│   ├── service/PermissionServiceSpec.groovy
│   ├── service/RoleHierarchySpec.groovy
│   ├── service/CachedPermissionServiceSpec.groovy
│   ├── repository/RoleRepositorySpec.groovy
│   └── web/CheckPermissionEndpointSpec.groovy
sql/
└── init.sql                        ← schema + seed data, auto-run by Postgres on first boot
docker-compose.yml                  ← Postgres + Redis + App, fully networked
Dockerfile                          ← multi-stage build (Gradle build → slim JRE runtime)
```

---

## Getting Started

### Prerequisites
- Docker Desktop installed and running
- JDK 17 (only needed if running outside Docker)

### Run the Entire Stack (Recommended)

```bash
git clone <your-repo-url>
cd Enterprise_Permissions_and_RBAC_Engine
docker-compose up --build
```

This single command builds and starts PostgreSQL, Redis, and the application together, fully networked. The API will be available at `http://localhost:4567`.

### Run Locally (Development Mode)

```bash
docker-compose up -d postgres redis   # only the dependencies
./gradlew generateJooq                # regenerate jOOQ classes against the live schema
./gradlew run                         # or run Main.java directly from your IDE
```

### Verify It's Working

```bash
curl "http://localhost:4567/check-permission?role=manager&permission=read_wiki"
```

Expected response:
```json
{"role":"manager","permission":"read_wiki","hasPermission":true}
```

---

## API Reference

### `GET /check-permission`

Checks whether a role has a specific permission, directly or via inheritance.

| Query Param | Required | Description |
|---|---|---|
| `role` | yes | Role name to check |
| `permission` | yes | Permission string to check |

```bash
curl "http://localhost:4567/check-permission?role=intern&permission=approve_budget"
```
```json
{"role":"intern","permission":"approve_budget","hasPermission":false}
```

**Responses:** `200 OK` · `400` missing parameters · `404` unknown role

### `GET /roles/:name/permissions`

Returns every permission a role has, including all inherited permissions.

```bash
curl "http://localhost:4567/roles/manager/permissions"
```
```json
{"role":"manager","permissions":["approve_budget","submit_expenses","read_wiki"]}
```

**Responses:** `200 OK` · `404` unknown role

All unhandled server errors are caught by a global exception handler and returned as consistent JSON with a `500` status — no leaked stack traces.

---

## Running Tests

This project was built test-first throughout, using **Spock** for expressive, behavior-driven specifications.

```bash
./gradlew test
```

Test coverage spans every layer, in isolation:

| Spec | What it verifies |
|---|---|
| `RoleSpec` | Direct permissions & parent relationships on a single `Role` |
| `PermissionServiceSpec` | DFS traversal correctness, including multi-parent inheritance |
| `RoleHierarchySpec` | Graph construction & an end-to-end in-memory chain |
| `RoleRepositorySpec` | Real PostgreSQL data correctly mapped into domain objects |
| `CachedPermissionServiceSpec` | The Redis decorator calls the real service exactly once per unique question (verified via Spock mocks) |
| `CheckPermissionEndpointSpec` | Full HTTP request/response behavior, including error status codes |

---

## Caching Strategy

`PermissionChecker` is an interface implemented by both `PermissionService` (the real logic) and `CachedPermissionService` (a Redis-backed decorator wrapping it):

```
Request → CachedPermissionService
              ├─ cache hit  → return from Redis instantly
              └─ cache miss → delegate to PermissionService → store in Redis (60s TTL) → return
```

This is the **Decorator Pattern** combined with **Dependency Inversion** — the caching layer was added without modifying a single line of the original `PermissionService`, because both depend on the shared `PermissionChecker` abstraction rather than on each other directly.

---

## Database Schema

```
roles                 (id, name)
permissions           (id, name)
role_permissions      (role_id, permission_id)        — junction table
role_inheritance      (child_role_id, parent_role_id)  — junction table, models the graph edges
```

Schema and seed data are defined in `sql/init.sql` and applied automatically the first time the PostgreSQL container starts.

---

## Development Workflow

This project follows a **Git Flow**–style branching model:

```
main      → stable, tagged releases only
dev       → integration branch
feature/* → one branch per unit of work, merged into dev via PR
```

Commit messages follow the **Conventional Commits** standard (`feat:`, `fix:`, `test:`, `refactor:`, `docs:`) to keep history readable as a development log.

---

## Future Improvements

- **Kubernetes manifests** — `Deployment` + `Service` definitions for horizontal scaling and self-healing, intentionally scoped out of this version to keep the deliverable focused.
- **Cloud deployment** — packaged for either Cloud Run (simplest, serverless) or GKE, depending on scale requirements.
- **`Optional<Role>`** instead of `null` returns in the repository layer, for stricter null-safety.
- **Single-query hierarchy load** — `RoleRepository.loadFullHierarchy()` currently runs three separate queries for clarity; a single optimized join is a natural next iteration.
- **Cache invalidation on write** — currently relies purely on TTL expiry; a write-through invalidation strategy would keep cached data fresher when roles change.

---

## License

This project was built as a personal portfolio piece for backend internship applications.