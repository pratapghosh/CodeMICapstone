# Copilot Instructions for Todo App Codebase

These instructions apply to the entire repository.

## Instruction Layout

- Global defaults live in this file.
- Backend-specific rules are in `.github/instructions/backend.instructions.md`.
- Frontend-specific rules are in `.github/instructions/frontend.instructions.md`.
- Test-specific rules are in `.github/instructions/test.instructions.md`.

## Project Context

- Stack: Java 21, Spring Boot 3.3.x, Spring MVC, Spring Security, Thymeleaf, Maven.
- Package root: `com.capstone.todo`.
- Persistence is file-based under `storage/` (JSON files), not a database.

## Architecture Rules

- Keep a layered structure:
  - `web` for controllers
  - `service` / `service.impl` for business logic
  - `repository` / `repository.impl` for data access
  - `storage` for file read/write concerns
  - `dto` for form/request models and validation
  - `domain` for core entities/models
- Avoid leaking repository or storage logic into controllers.
- Prefer constructor injection and keep classes focused on one responsibility.

## Design Principles

- Follow SOLID principles, especially:
  - Single Responsibility Principle (keep controllers/services/repositories focused)
  - Dependency Inversion Principle (depend on interfaces in service/repository layers)
- Prefer clean separation of concerns across layers.
- Keep code open for extension and closed for modification where practical.

## Design Patterns

- Repository pattern for persistence abstraction.
- Service layer pattern for business orchestration.
- DTO pattern for request/form transfer and validation boundaries.
- Configuration pattern for framework wiring under `config` classes.
- Strategy-style extension via interfaces (e.g., service/repository contracts) when adding alternatives.

## Coding Conventions

- Use clear method and variable names.
- Keep methods small and intention-revealing.
- Add validation in DTOs and service boundaries where relevant.
- Preserve existing package naming and folder structure.
- Do not introduce a database dependency unless explicitly requested.

## Security and Data Handling

- Keep authentication and registration flows aligned with current Spring Security setup.
- Use BCrypt for password handling (consistent with existing implementation).
- Ensure tasks remain user-scoped (no cross-user data exposure).

## UI and Templates

- Maintain existing Thymeleaf template flow (`login`, `register`, `tasks`).
- Reuse current styling patterns from `src/main/resources/static/css/styles.css`.
- Keep server-rendered flow unless SPA conversion is explicitly requested.

## Build, Run, and Test

- Build: `mvn clean package`
- Run: `mvn spring-boot:run`
- Test: `mvn test`
- If Windows terminal PATH is stale, use explicit Maven command:
  - `C:\ExtraSoftwares\maven\apache-maven-3.9.9\bin\mvn.cmd spring-boot:run`

## Change Discipline

- Prefer minimal, targeted changes.
- Do not refactor unrelated files in the same change.
- Update `README.md` when setup or run instructions change.
- Add or update tests for behavior changes when practical.
