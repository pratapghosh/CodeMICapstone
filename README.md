# Todo App (Java + Spring Boot + Thymeleaf)

A production-grade sample Todo application with:

- User management (register + login)
- User-specific task visibility
- Date-based task planning
- Planned finish date tracking
- Dashboard search, quick filters, sorting, and overdue highlighting
- Local file-system persistence (no database)

## Tech Stack

- Java 21
- Spring Boot 3.3.x
- Spring Security
- Spring MVC + Thymeleaf (backend + frontend)
- Jackson (JSON file persistence)
- Maven

## Features

- Secure registration and login (BCrypt password hashing)
- Each user has a private task list
- Add tasks with:
	- Task date
	- Planned finish date
	- Priority
- Search the dashboard by task title or description
- Filter tasks by status, priority, and date bucket:
	- Status: All, Open, Completed
	- Priority: All, High, Medium, Low
	- Date: All, Today, Upcoming, Overdue
- Sort tasks by planned finish date ascending or priority high-to-low
- Open tasks with a planned finish date before today are highlighted as overdue
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
|- service/           # Service abstractions and dashboard query models
|- service/impl/      # Business logic implementations
|- storage/           # File read/write utility
|- web/               # MVC controllers

src/main/resources
|- templates/         # Thymeleaf views
|- static/css/        # Frontend styles
|- application.yml    # Configuration
```

## File-Based Persistence

Runtime data is stored under `storage/`:

- `storage/users.json` for user data
- `storage/tasks/<username>.json` for user-wise tasks

The folder is generated automatically at runtime and is excluded in `.gitignore`.

## Dashboard Search, Filters, and Sorting

After logging in, open `/tasks` to manage your private task list. The dashboard supports optional server-side query parameters and matching UI controls:

- `q` searches task title and description, case-insensitively.
- `status` accepts `ALL`, `OPEN`, or `COMPLETED`.
- `priority` accepts `ALL`, `HIGH`, `MEDIUM`, or `LOW`.
- `dateFilter` accepts `ALL`, `TODAY`, `UPCOMING`, or `OVERDUE`.
- `sort` accepts `NONE`, `PLANNED_FINISH_ASC`, or `PRIORITY`.

Example:

```text
/tasks?q=release&status=OPEN&priority=HIGH&dateFilter=UPCOMING&sort=PLANNED_FINISH_ASC
```

All search, filter, and sort operations are applied only after fetching tasks for the authenticated user, so users cannot see each other's tasks. Open tasks whose planned finish date is before the server-local current date are displayed with an overdue badge and highlighted card.

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

3. Open:

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
3. Add task with task date, planned finish date, and priority
4. Use dashboard search or quick filters to find tasks by keyword, status, priority, or date bucket
5. Sort matching tasks by planned finish date or priority
6. Confirm open overdue tasks are highlighted
7. Mark task as completed
