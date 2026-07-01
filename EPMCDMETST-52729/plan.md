# EPMCDMETST-52729 Implementation Plan

## Overview & Scope

This feature adds a CSV export capability to the existing Spring Boot + Thymeleaf Todo app so authenticated users can download their own tasks from the dashboard. Based on verified repository context, the current application already has user-scoped task retrieval via `TaskController`that calls `TaskService.getUserTasks(username)`, renders the dashboard in `tasks.html`, and persists data in user-specific JSON files under `storage/tasks/<username>.json`. The app does not yet provide any download or export mechanism.

The scope of this plan covers:

- adding an authenticated, user-scoped export endpoint for CSV download
- adding CSV serialization logic for existing task fields with proper escaping
- updating the dashboard Thymeleaf template to include an Export CSV action
- preserving user isolation so only the currently authenticated user's tasks are exported
- adding unit and/or MVC tests for success, empty export, escaping, and access control
- updating `README.md` to document the CSV export capability

Out of scope: CSV import, client-side generation, bulk/admin exports, any move away from the current file-based storage model, and any changes to authentication or authorization beyond reusing the existing Spring Security configuration.

## Repository Context & Affected Components

Verified touchpoints in the current codebase:

- `src/main/java/com/capstone/todo/web/TaskController.java` - MVC controller that currently renders the task dashboard and handles create, edit, and complete flows but has no export download endpoint
- `src/main/java/com/capstone/todo/service/TaskService.java` - service interface that already exposes `getUserTasks` and is the most likely place to add a specific export-oriented method or reuse the existing retrieval method
- `src/main/resources/templates/tasks.html` - Thymeleaf dashboard template that currently has no export action
- `src/main/java/com/capstone/todo/domain/TodoTask.java` - domain model that defines the fields the ticket requires in the CSV (id, title, description, taskDate, plannedFinishDate, priority, status, createdAt)
- `src/test/java/com/capstone/todo/web/TaskControllerTest.java`- existing controller unit test patterns for Authentication, model population, and service delegation
- `README.md` - documentation that already describes file-based persistence and the current sample workflow

The lowest-risk approach is to build the export on top of the current server-rendered MVC pattern and reuse the existing user-scoped task retrieval behavior.

## Assumptions / Open Questions

Assumptions derived from the ticket and verified repository context:

- All tasks for the authenticated user should be exported using the existing `taskService.getUserTasks(username)` behavior; no new visibility or filtering rules are introduced in this ticket.
- Both open and completed tasks should be included, since the ticket specifies export of the user's task list and the assumption section explicitly states that completed and open tasks are both included.
- Server-side CSV generation is acceptable for this release, and the app does not require a new downstream storage format or a client-side export implementation.
- The filename can use server-local date with the documented pattern `tasks-<username>-<yyyyMMdd>.csv`.
- Null or blank descriptions should be exported as empty CSV cells, not the string `null`.
- The application can rely on existing Spring Security to protect the new endpoint, so unauthenticated requests should follow the same redirect/deny behavior as other protected task routes.

Open questions to capture in the plan (rather than fabricating details):

- Should the CSV row order match the current getUserTasks order or be explicitly sorted? In the absence of a ticket requirement, the safest assumption is to preserve the existing service return order.
- Should date/timestamp fields use their current default Java string format (likely ISO-like values) or be custom-formatted? The ticket does not require a custom format, so the plan assumes stable, readable serialization using existing field values unless product guidance says otherwise.
- Should the CSV serialization live inside `TaskService`, in a new supporting export service, or as a private controller helper? To keep the controller thin and testable, the preferred approach is a small dedicated CSV serialization component rather than embedding string-assembly logic directly in the controller.

## Proposed Technical Approach

1. **Design around the existing layered pattern**
  - Reuse the current authenticated MVC entrypoint to obtain the current username.
  - Reuse `taskService.getUserTasks` for data scoping so the export cannot bypass existing user isolation.
  - Keep the CSV response server-generated and returned as an HTTP attachment with no changes to persistence.

2. **Add a user-scoped export download endpoint**
  - Extend `TaskController` with a new authenticated route for example `@GetMapping("/tasks/export")` or `GetMapping("/tasks/export/csv")`.
  - Have the endpoint read the authenticated username from `Authentication`, load tasks via the service, build the CSV body, and return a `ResponseEntity<ByteArrayResource>` or similar download-rich response with `Content-Type: text/csv` and `Content-Disposition: attachment` set.
  - Keep the endpoint read-only with respect to persistence because export should not modify tasks.

3. **Introduce dedicated CSV serialization logic**
  - Create a focused component (for example a `CsvTaskExportService` or package-private utility) that accepts `List<TodoTask>` and returns a CSV string or byte array.
  - Encode a fixed header row in this order: `id,title,description,taskDate,plannedFinishDate,priority,status,createdAt`.
  - Implement CSV escaping for commas, double quotes, CR/LF line breaks, and null/blank values. A standard approach is to wrap each cell in quotes when needed and double any embedded quotes.

4. **Keep user isolation explicit**
  - Build the export solely from the task list retrieved for the current authenticated user.
  - Do not accept a username or user id as a request parameter, which would create an unnecessary access-control risk.
  - If the service layer already normalizes usernames, let that behavior remain the single source of truth.

5. **Update the dashboard UI**
  - Add an Export CSV action in `tasks.html`, placed near the existing dashboard header controls so it is clearly available to logged-in users.
  - Use a simple anchor or button linking to the new export route, consistent with the app's current server-rendered nature.
  - Keep the change minimal and avoid any dashboard redesign, since the ticket explicitly rules out a SPA or broad UI refresh.

6. **Document the feature and test it through the existing test style**
  - Update `README.md` to mention task export under features and the sample workflow.
  - Extend `TaskControllerTest` with controller-level tests for export logic and return types.
  - Add unit tests for the CSV serialization component to catch escaping and empty-list behavior early.

## Implementation Steps (Sequenced)

0. **Verify the domain field sources and consumer boundaries**
  - Confirm in `TodoTask` the getters available for `id`, `title`, `description`, `taskDate`, `plannedFinishDate`, `priority`, `status`, and `createdAt`.
  - Decide whether the controller will call taskService directly for data and a new CSV serialization component for serialization, or whether a single service method will return export-ready bytes. The plan recommends separating data retrieval from CSV formatting for testability.

1. **Add the CSV serialization component**
  - Create a new class (under a verified package such as `service` or a new `export`/`util` package if that structure is aligned with the codebase) that:
    - writes the header row
    - maps each `TodoTask` to one CSV row
    - escapes values correctly for commas, quotes, and line breaks
    - renders null descriptions as empty
  - Prefer an implementation that is deterministic and easy to unit test, e.g. building the text via `StringBuilder` or a standard Java writer rather than introducing a new dependency unless the repository already includes one.

2. **Add the controller export endpoint**
  - Extend `TaskController` with a new method that:
    - reads the authenticated username
    - loads the user's tasks from `taskService.getUserTasks`
    - delegates CSV building to the new component
    - returns a header-correct file download response
  - Set content type to `text/csv`.
  - Set `Content-Disposition` to attachment with a filename like `tasks-<username>-<yyyyMMdd>.csv`.
  - Ensure the endpoint behaves correctly when the task list is empty by returning just the header row.

3. **Update the dashboard template**
  - In `tasks.html`, add an Export CSV Link or button in the dashboard header near the logout action.
  - If the current CSS classes are sufficient, reuse existing `btn` or `secondary` styling; otherwise, note the minimal CSS touchpoint required to keep the action consistent with the current UI.

4. **Update documentation**
  - Add the CSV self-service export capability to the README feature list.
  - Update the sample workflow to include exporting tasks from the dashboard.
  - If the README documents endpoints or capabilities around authenticated tasks, include the download behavior and filename convention.

5. **Add tests in the existing style**
  - Extend the controller test suite for the new export endpoint and service delegation.
  - Add dedicated unit tests for the CSV serializer, especially for escaping and empty-list behavior.
  - If the repository has Spring MVC integration tests, add an unauthenticated-access scenario to confirm the new route follows existing Security behavior.

## Testing Plan

**Unit tests - CSV serialization component**

- Should cover header generation in the exact required order.
- Should cover multiple rows with one row per task.
- Should cover escaping for:
  - commas in description or title
  - quotes in text
  - multi-line descriptions
  - null descriptions rendered as empty cells
- Should cover empty task list behavior (export contains header only).

**Controller tests **

- Extend `TaskControllerTest` to cover:
  - successful export returning a download-style response
  - content type ``text/csv``
  - Content-Disposition filename pattern containing username and date
  - delegation to `taskService.getUserTasks` with the authenticated username
- If the test suite includes MVc/security tests, cover unauthenticated access to confirm redirect or deny behavior matches existing configuration.

**Regression / manual smoke tests**

- Log in as a user with several tasks and confirm the dashboard shows an Export CSV action.
- Click Export CSV with tasks present and confirm a file downloads.
  - Open the file in a text editor or spreadsheet and confirm the header and rows are correct.
- Use descriptions containing commas, quotes, and line breaks to confirm the CSV remains valid.
- Log in as a user with no tasks and confirm the downloaded file contains only the header row.
- Log in as a different user and confirm that exported data includes only that user's tasks.
- Request the export route without authentication and confirm the app behaves according to existing Spring Security rules.

## Test Data / Environment Needs

- No new infrastructure, database, migration, or external service is required.
- Test data should include:
  - a user with multiple tasks
  - at least one task with special characters in text fields
  - a user with no tasks
  - a second user to verify isolation
- Controller-level tests can continue to mock `Authentication` and `TaskService` as the current tests do.
- Encoding should remain UTf-8 compliant to align with the ticket constraint.

## Deployment / Rollout Plan

- Deploy as a normal application release; no feature flag, config change, or data migration is required.
- Post-deployment verification:
  - log in and done manual export for a real user
  - confirm the filename and content type are correct
  - confirm empty and non-empty exports both work
- Monitor logs for any unintended exceptions around the new route or CSV generation.
- Rollback: revert the controller, template, documentation, and any new CSV supporting components and redeploy. There is no schema or data-format risk because export is read-only.

## Risks & Mitigations

- **Risk: CSV escaping is incorrect, leading to broken downloads in Excel or other spreadsheet tools.**
  - Mitigation: centralize escaping logic in one testable component and add explicit tests for commas, quotes, and newlines.

- **Risk: Export accidentally includes tasks from other users.**
  - Mitigation: drive export exclusively from `taskService.getUserTasks(authenticatedUsername)` and add tests that assert user scoping.

- **Risk: Empty task lists produce blank files or server errors.**
  - Mitigation: explicitly generate the header row even when the rows list is empty and cover this with a dedicated test.

- **Risk: Filename or response headers are inconsistent with the ticket.**
  - Mitigation: build a single helper for filename construction and assert content type and Content-Disposition in tests.

- **Risk: Unauthenticated export access behavior differs from existing security rules.**
  - Mitigation: register the route under the same protected controller context and if possible add an MVC/security test to confirm the behavior.

## Definition of Done & Acceptance Criteria Mapping

- **DoD: The task dashboard includes an Export CSV action for authenticated users.**
  - Covered by `tasks.html` updates and manual UI validation.

- **DoD: A new authenticated endpoint downloads only the current user's tasks as a CSV file.**
  - Covered by controller implementation and user-scoping tests.

- **DoD: CSV output includes a header row and one row per task with id, title, description, taskDate, plannedFinishDate, priority, status, and createdAt.**
  - Covered by the CSV serializer unit tests and manual export validation.

- **DoD: CSV values are safely escaped for commas, quotes, line breaks, and blank/null descriptions.**
  - Covered by deterministic escaping tests.

- **DoD: The response uses an appropriate `text/csv` content type and attachment filename convention.**
  - Covered by controller response tests.

- **DoD: Exported data remains scoped to the authenticated user.**
  - Covered by user-isolation tests and manual validation with multiple logins.

- **DoD: Empty task lists still export a valid CSV file containing the header row.**
  - Covered by empty-list unit tests and manual smoke tests.

- **DoD: Unit and/or MVC tests cover successful export, escaping, empty list behavior, unauthenticated access, and user isolation.**
  - Covered by the testing plan above.

- **DoD: `README.md` is updated to document the CSV export capability.**
  - Covered by documentation updates.

- **AC1: Given I am logged in on the task dashboard, when I click Export CSV, then a CSV file is downloaded.**
  - Covered by controller route, template link, and manual smoke validation.

- **AC2: Given I export my tasks, then the file contains a header row with id, title, description, taskDate, plannedFinishDate, priority, status, and createdAt.**
  - Covered by CSV content tests.

- **AC3: Given I have multiple tasks, when I export, then each of my tasks appears as exactly one CSV row.**
  - Covered by multi-row unit tests.

- **AC4: Given a task description contains commas, quotes, or line breaks, when I export, then the CSV remains valid and those values are correctly escaped.**
  - Covered by escaping unit tests.

- **AC5: Given I have no tasks, when I export, then the downloaded CSV contains only the header row and no error occurs.**
  - Covered by empty-list behavior tests.

- **AC6: Given another user has tasks, when I export, then their tasks are not present in my CSV.**
  - Covered by user-scoping tests.

- **AC7: Given I am not authenticated, when I request the export endpoint, then I am redirected to login or denied according to the existing Spring Security configuration.**
  - Covered by a security/MVC test if present, otherwise captured as manual validation.

- **AC8: Given the export completes, then the HTTP response has content type `text/csv` and an attachment filename following the documented naming convention.**
  - Covered by controller response tests.
