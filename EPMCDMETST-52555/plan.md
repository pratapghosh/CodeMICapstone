# EPMCDMETST-52555 Implementation Plan

## Overview & Scope

This feature adds server-side task filtering, sorting, and overdue indicators to the Todo dashboard for the authenticated user. Based on the current repository, `TaskController.taskDashboard` in `src/main/java/com/capstone/todo/web/TaskController.java` always fetches all tasks for the current user via `taskService.getUserTasks(username)`, `TaskService` currently exposes only `getUserTasks` behavior, and `src/main/resources/templates/tasks.html` renders the full tasks collection without filter controls or sorting options.

The scope of this plan covers:

- dashboard filter controls for status and priority
- server-side sorting by planned finish date and priority
- overdue classification and clear visual marking for open tasks
- Thymeleaf UI, controller model, and CSS updates to support the new dashboard behavior
- unit and MVC test coverage for filtering, sorting, and overdue classification
- README documentation updates if dashboard usage changes

Out of scope: any database migration, persistence replacement, cross-user visibility, notifications, recurring tasks, or a frontend SPA rewrite.

## Repository Context and Affected Components

Verified touchpoints in the current codebase:

- `src/main/java/com/capstone/todo/web/TaskController.java` - dashboard entry point for `GET /tasks`, edit, create, and complete flows
- `src/main/java/com/capstone/todo/service/TaskService.java` - service interface currently exposes raw fetching of user tasks
- `src/main/java/com/capstone/todo/service/impl/DefaultTaskService.java` - task business logic, username normalization, and date validation
- `src/main/java/com/capstone/todo/domain/TaskStatus.java` - status enum used for open/completed filtering
- `src/main/java/com/capstone/todo/domain/Priority.java` - priority enum used for high/medium/low filtering and custom sorting
- `src/main/java/com/capstone/todo/domain/TodoTask.java` - already contains title, description, status, priority, taskDate, plannedFinishDate, and createdAt
- `src/main/resources/templates/tasks.html` - dashboard view that needs filter controls and overdue indicators
- `src/main/resources/static/css/styles.css` - styles layer likely needing minimal updates
- `README.md` - needs documentation updates for the new dashboard capabilities
- `src/test/java/com/capstone/todo` - existing test structure for domain, config, dto, service, and web layers will be extended

The ticket explicitly says no persistence format change is needed, and the required filterable fields already exist in the domain model, so the most likely touchpoints are the MVC, service, template, CSS, and testing layers.

## Assumptions / Open Questions

Assumptions supported by the ticket and repository:

- Server-side filtering and sorting via Spring MVC query parameters is acceptable for this Thymeleaf application.
- Overdue means an `OPEN` task with `plannedFinishDate.isBefore(LocalDate.now())`.
- Priority sort should be explicit domain order (HIGH before MEDIUM before LOW), not alphabetic or implicit enum order.
- A no-filter `/tasks` request should remain backward-compatible by showing all tasks for the authenticated user.

- Overdue indication can be implemented without persisting any new field by computing it at render/test time.

Open questions to capture in the plan:

- Should create, edit, and complete redirects preserve current filter query parameters? The ticket only requires that these flows remain functional and redirect to a valid dashboard view, but preserving state would be a usability improvement.
- Should invalid query values be rejected or safely coerced to defaults? The safest approach for this SSE app is to fall back to default filter states.
- What visual treatment is preferred for overdue tasks? The safest default is a badge and/or CSS highlight that fits the existing server-rendered UI.

## Proposed Technical Approach

1. **Introduce explicit dashboard query inputs**
   - Update `TaskController.taskDashboard()` to accept optional query parameters for `status`, `priority`, and `sort`.
   - Normalize and validate these values against controlled options so invalid inputs fall back to safe defaults rather than breaking the page.
   - Add the selected query state to the model so Thymeleaf can re-render the current control selections.

2. **Centralize filtering, sorting, and overdue derivation in the service layer**
   - Extend `TaskService` with a method that accepts filter/sort inputs, or introduce a small query object such as `TaskDashboardQuery`.
   - Implement the logic in `DefaultTaskService` on top of the existing user-scoped repository fetch so user isolation remains enforced before any filters are applied.
   - Use a deterministic pipeline: fetch user tasks -> apply status filter -> apply priority filter -> apply sorting.
   - Keep overdue rules in Java, not in Thymeleaf, to avoid duplicated date logic.

3. **Use a view-model or template-support strategy for derived task state**
   - Prefer a lightweight view DTO or model attribute structure that includes the `TodoTask` and a derived `overdue` flag.
   - This keeps the template simple and makes overdue rules reusable both for filtering and rendering.

4. **Update the dashboard UI using existing Thymeleaf patterns**
   - Add a GET form above the task list with selects for status, priority, and sort.
   - Add a reset/clear option that returns the user to the default `/tasks` view.
   - Render a clear overdue label or CSS class for tasks that meet the overdue rule.
   - Ensure edit and complete actions remain available for open tasks when filters are active.

5. **Refactor controller model population for regression safety**
   - The create, edit, and update error paths currently repopulate the dashboard model manually.
   - Introduce a single private helper to populate tasks, forms, query state, and any derived display metadata so all render paths remain consistent.

## Implementation Steps (Sequenced)

1. **Introduce controlled dashboard query types**
   - Create enums or small controlled-value types for status filter, priority filter, and sort option.
   - Consider a `TaskDashboardQuery` object that holds `status`, `priority`, and `sort`.
   - Give default values such as `ALL` and no-sort so existing behavior remains intact for raw `/tasks` requests.

 2. **Extend the service interface**
   - Add a method such as `List<TodoTask> getUserTasks(String username, TaskDashboardQuery query)`.
   - Keep the existing getUserTasks(String username) method as a convenience delegating to the new overload with default query values.

 3. **Implement filtering, sorting, and overdue helper behavior in `DefaultTaskService`**
   - Start from `taskRepository.findByUsername(normalizeUsername(username))` so the core user-isolation rule stays intact.
   - Apply status filter for ALL, OPEN, and COMPLETED.
   - Apply priority filter for ALL, HIGH, MEDIUM, and LOW.
   - Compute a reusable isOverdue(TodoTask task, LocalDate today) helper and re-use it for rendering metadata.
   - Apply sorting with comparators for planned finish date ascending and explicit priority rank.
   - Handle any null-safety needs defensively, even if form validation normally prevents null dates.

4. **Refactor `TaskController` to support query parameters and consistent model building**
   - Update `taskDashboard()` to accept optional query parameters and pass them to the service.
   - Add a helper such as `populateDashboardModel(...)` that adds the filtered/sorted tasks, selected query state, form objects, overdue metadata, and the username.
   - Use this helper in the normal dashboard path, the edit view path, and the create/update validation-error paths.
   - Ensure the mapping for `markTaskCompleted` continues to work without cross-user leakage.

5. **Update `tasks.html` with filter and sort controls**
   - Add a GET form with selects for Status (All, Open, Completed), Priority (All, High, Medium, Low), and Sort (None, Planned Finish Date, Priority).
   - Add a submit action and clear-filters link.
   - Update the empty-state message to be appropriate for both no tasks and no matching results scenarios.
   - Add an overdue badge or CSS indicator on task items if they are open and overdue.

6. **Update styles in `styles.css`**
   - Add minimal styles for the filter bar, control clusters, overdue badge, and any task highlight state.
   - Keep the visual treatment consistent with the existing card/button style.

.7. **Update README**
   - Document that the dashboard now supports filters by status and priority, sorting by planned finish date and priority, and overdue highlighting.
   - If the README includes sample flows, add an example of using the new tasks dashboard controls.

8. **Add or expand tests**
   - Service-level tests for status filters, priority filters, sort order, and overdue classification.
   - Controller-level MVC tests for query parameter handling, model population, and regression safety on create/edit/complete flows.

## Testing Plan

**Unit tests - Service layer**

- status filter returns only OPEN tasks
- status filter returns only COMPLETED tasks
- priority filter returns only HIGH, MEDIUM, or LOW tasks as requested
- sort by planned finish date orders ascending with a deterministic secondary sort such as createdAt or title
- sort by priority orders HIGH before MEDIUM before LOW
- overdue classification returns true only for OPEN tasks with planned finish date before today
- overdue classification excludes completed past-due tasks
- username normalization and user-scoped repository fetch still apply

**Controller-level tests**

- `GET /tasks?status=OPEN&priority=HIGH&sort=PRIORITY` populates the model with selected query state
- invalid query parameters fall back to safe defaults without erroring
- dashboard edit view still populates tasks, edit form, and query metadata
- create validation error render still includes the filtered panel data
- update validation error render still includes edit state and query state
- complete action still resolves only for the authenticated user

**Regression / manual smoke tests**

- log in as User A and verify that no User B tasks appear under any filter or sort combination
- create tasks covering open, completed, and overdue scenarios
- verify each filter individually and in combination
- verify both sort modes
- verify overdue open tasks are marked clearly
- verify completed past-due tasks are not marked overdue
- verify create, edit, and mark-completed flows still behave correctly

## Test Data / Environment Needs

- No new infrastructure, database, or external services are required.
- Tests should use deterministic date values relative to a controllable reference date where possible.
- If implementation calls `LocalDate.now()` directly, tests should either build data relative to runtime or introduce a small testable clock abstraction.

## Deployment / Rollout Plan

- Deploy as a standard application release; no migrations or configuration changes are expected.
- No feature flag is strictly required because the change is backward-compatible for default `/tasks` access.
- Monitor application logs for dashboard rendering errors, query-parameter handling issues, and any unexpected Thymeleaf expression failures.
- Rollback: revert controller, service, template, CSS, test, and README changes and redeploy. Persisted task data remains unchanged, so rollback risk is low.

## Risks & Mitigations

- **Risk: Thymeleaf template logic becomes cluttered.**
  - Mitigation: keep complex filter/sort/overdue logic in Java and pass simple model attributes or view-model flags to the template.

- **Risk: Date-dependent tests become flaky.**
  - Mitigation: centralize the "today" calculation or introduce a testable clock pattern for service-level logic.

- **Risk: Controller error paths fail to repopulate filter metadata.**
  - Mitigation: use a single dashboard-model-population helper for all render paths.

- **Risk: Priority sort accidentally uses enum order or alphabetic order.**
  - Mitigation: add explicit comparators and assert the order in tests.

- **Risk: Query parameter names/allowed values drift between controller, service, and UI.**
  - Mitigation: define controlled types or constants and use them across layers.

## Definition of Done & Acceptance Criteria Mapping

- **DoD: The tasks dashboard provides filter controls for status and priority.**
  - Covered by controller parameter handling, service query method, and template controls.

- **DoD: The tasks dashboard provides sorting by planned finish date and priority.**
  - Covered by explicit comparators in service logic and order-verifying tests.

- **DoD: Overdue open tasks are visually indicated.**
  - Covered by overdue detection, template rendering, and styling.

- **AC: Given I am logged in and have tasks with different statuses, when I select a status filter, then only my tasks matching that status are displayed.**
  - Covered by status filter implementation and tests.

- **AC: Given I am logged in and have tasks with different priorities, when I select a priority filter, then only my tasks matching that priority are displayed.**
  - Covered by priority filter implementation and tests.

- **AC: Given I apply both status and priority filters, then the dashboard displays only tasks matching both selected values.**
  - Covered by combined filter pipeline logic and tests.

- **AC: Given I choose sort by planned finish date, then tasks are ordered by plannedFinishDate with a deterministic secondary sort such as createdAt or title.**
   - Covered by sort comparator implementation and order asserting tests.

- **AC: Given I choose sort by priority, then tasks are ordered HIGH before MEDIUM before LOW.**
  - Covered by explicit priority ranking and tests.

- **AC: Given an OPEN task has plannedFinishDate before today, when the task list is rendered, then it is clearly marked as overdue.**
   - Covered by overdue flag computation, template indicators, and style updates.

- **AC: Given a task belongs to another user, when I filter or sort my dashboard, that task is never displayed.**
  - Covered by user-scoped repository fetch before any filtering and by regression testing.

- **AC: Given filters are applied, when I create, edit, or complete a task, then existing workflows remain functional and redirect to a valid task dashboard view.**
  - Covered by controller refactoring, model-population helpers, and MVC regression tests.

- **DoD: Unit or controller/service tests cover filtering, sorting, and overdue classification behavior.**
   - Covered by expanded service and controller tests.

- **DoD: README or user-facing documentation is updated if dashboard usage changes.**
  - Covered by documentation updates in `README.md`.

## Manual Validation Checklist

- Log in as User A and confirm only User A's tasks appear across filter and sort combinations
- Create a mix of open, completed, and overdue tasks
- Verify each filter individually and in combination
- Verify both sort options yield the expected order
- Verify an open overdue task shows a clear overdue label or highlight
- Verify a completed past-due task is not marked overdue
- Verify create, edit, and mark-completed flows still work without error
