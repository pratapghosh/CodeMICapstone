# Todo App (Java + Spring Boot + Thymeleaf)

A production-grade sample Todo application with:

- User management (register + login)
- User-specific task visibility
- Date-based task planning
- Planned finish date tracking
- Local file-system persistence (no database)

## Tech Stack

- Java 21
- Spring Boot 3.3.x
- Spring Security
- Spring MVC + Thymeleaf (backend + frontend)
- Jackson (JSON file persistence)
- Maven
- Playwright for UI end-to-end testing

## Features

- Secure registration and login (BCrypt password hashing)
- Each user has a private task list
- Add tasks with:
	- Task date
	- Planned finish date
- Mark tasks as completed
- Clean layered architecture:
	- Controller
	- Service
	- Repository
	- Storage utility

## Project Structure

```
src/main/java/com/capstone/todo
|- config/            # App, Jackson, Security configuration
|- domain/            # Core domain models
|- dto/               # Form DTOs and validation
|- repository/        # Repository abstractions
|- repository/impl/   # File-system repository implementations
|- service/           # Service abstractions
|- service/impl/      # Business logic implementations
|- storage/           # File read/write utility with file locks
|- web/               # MVC controllers

src/main/resources
|- templates/         # Thymeleaf views
|- static/css/        # Frontend styles
|- application.yml    # Configuration

src/test
|- java/              # Unit and integration tests
|- ui/
	|- fixtures/       # UI test data builders
	|- pages/          # Playwright page objects
	|- specs/auth/     # Authentication UI scenarios
	|- playwright.config.js
```

## File-Based Persistence

Runtime data is stored under `storage/`:

- `storage/users.json` for user data
- `storage/tasks/<username>.json` for user-wise tasks

The folder is generated automatically at runtime and is excluded in `.gitignore`.

## Run Locally

### Prerequisites

- Java 21 installed
- Maven 3.9+ installed

### Windows Environment Setup

If Java or Maven is installed but commands are not recognized, set user environment variables once.

PowerShell:

```powershell
[Environment]::SetEnvironmentVariable('JAVA_HOME', 'C:\ExtraSoftwares\java\jdk-21.0.11+10', 'User')
[Environment]::SetEnvironmentVariable('MAVEN_HOME', 'C:\ExtraSoftwares\maven\apache-maven-3.9.9', 'User')

$userPath = [Environment]::GetEnvironmentVariable('Path', 'User')
$entries = ($userPath -split ';' | Where-Object { $_ -and $_.Trim() -ne '' })
if ($entries -notcontains 'C:\ExtraSoftwares\java\jdk-21.0.11+10\bin') {
	$entries += 'C:\ExtraSoftwares\java\jdk-21.0.11+10\bin'
}
if ($entries -notcontains 'C:\ExtraSoftwares\maven\apache-maven-3.9.9\bin') {
	$entries += 'C:\ExtraSoftwares\maven\apache-maven-3.9.9\bin'
}
[Environment]::SetEnvironmentVariable('Path', ($entries -join ';'), 'User')
```

Close and reopen terminal after this step.

1. Build the application:

```bash
mvn clean package
```

2. Run the application:

```bash
mvn spring-boot:run
```

3. Or run the packaged JAR generated under `target/artifact`.
Other build outputs such as classes and test-classes remain directly under `target`:

```powershell
java -jar .\target\artifact\todo-app-1.0.0.jar
```

4. Open:

`http://localhost:8080`

### Troubleshooting (Windows)

If `mvn` still fails in an old terminal session, use:

```powershell
& 'C:\ExtraSoftwares\maven\apache-maven-3.9.9\bin\mvn.cmd' spring-boot:run
```

## Test

```bash
mvn test
```

## UI Test

Playwright UI tests live under `src/test/ui` and target the application running at `http://localhost:8080`.

Prerequisites for UI tests:

- Node.js 18+ installed
- the Todo app already running locally on port 8080

Install Playwright dependencies:

```bash
npm install
npm run ui:install
```

Run the UI suite in headless mode:

```bash
npm run ui:test
```

Run the UI suite in headed mode:

```bash
npm run ui:test:headed
```

Debug a UI test run:

```bash
npm run ui:test:debug
```

Generated Playwright outputs:

- HTML report: `target/playwright-report/index.html`
- Screenshots, videos, traces, and raw results: `target/playwright-results`

## Design Principles and Standards

### Core Principles

- Follow SOLID, especially:
	- Single Responsibility Principle across controller/service/repository/storage layers
	- Dependency Inversion Principle through interface-driven service and repository design
- Keep separation of concerns strict between web, business, and persistence layers.
- Prefer small, cohesive classes and intention-revealing methods.

### Design Patterns Used

- Repository Pattern for persistence abstraction.
- Service Layer Pattern for business workflows.
- DTO Pattern for form binding and validation boundaries.
- Configuration Pattern for framework wiring under `config` classes.

### Coding Standards

- Constructor injection over field injection.
- Validation at DTO level and business-rule checks at service level.
- Avoid leaking file I/O concerns outside repository/storage components.
- Keep user data isolation strict (no cross-user task access).
- Do not add database dependencies unless explicitly required.

### Testing Standards

- Use behavior-focused tests with Arrange-Act-Assert structure.
- Add or update tests in the same change when behavior is modified.
- Keep tests deterministic and independent.

## Sample Workflow

1. Register a new user
2. Login
3. Add task with task date and planned finish date
4. See only your own tasks
5. Mark task as completed
