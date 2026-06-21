---
description: "Backend development rules for Spring Boot, security, service/repository layers, validation, and file-based persistence."
applyTo: "src/main/java/**,pom.xml,src/main/resources/application.yml"
---

# Backend Instructions

## Scope

Apply these rules for backend Java and Spring Boot configuration.

## Architecture

- Preserve layered boundaries:
  - `web` for controllers and request/response handling
  - `service` and `service.impl` for business logic
  - `repository` and `repository.impl` for persistence abstraction and implementation
  - `storage` for file I/O utilities only
  - `dto` for form/request objects and validation
  - `domain` for core models
- Do not move persistence or file I/O logic into controllers.
- Prefer constructor injection over field injection.

## Design Principles

- Apply SOLID in backend code.
- Keep high cohesion within classes and low coupling between layers.
- Prefer interface-driven design for services and repositories.
- Keep domain logic in services/domain models, not in controllers.

## Design Patterns

- Repository pattern: expose persistence through `repository` interfaces and keep file details in `repository.impl`/`storage`.
- Service layer pattern: keep business workflows in `service.impl` behind `service` interfaces.
- DTO pattern: map web input to DTOs for validation and boundary clarity.
- Factory/Configuration pattern: define framework beans in `config` classes.
- Template Method style in tests/services when shared workflow is needed, but avoid over-abstraction.

## Persistence and Data Rules

- Keep file-based storage under `storage/` as the source of persistence.
- Do not introduce database dependencies unless explicitly requested.
- Ensure user data isolation: never expose one user's tasks to another user.

## Security Rules

- Keep compatibility with the existing Spring Security flow.
- Use BCrypt for password handling.
- Keep authentication and authorization checks explicit in the web/service boundaries.

## API and Validation

- Validate incoming form data in DTOs and enforce business constraints in services.
- Prefer clear, descriptive method names and small focused methods.
- Avoid broad refactors when making targeted feature or bug changes.

## Testing

- Add or update tests in `src/test/java/**` for behavior changes.
- Prefer focused unit tests for service/repository logic.
- Keep test names descriptive of expected behavior.

## Build and Run

- Build: `mvn clean package`
- Run: `mvn spring-boot:run`
- Test: `mvn test`
