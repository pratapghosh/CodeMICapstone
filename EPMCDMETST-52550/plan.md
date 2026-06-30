# EPMCDMETST-52550 Implementation Plan

## Overview & Scope

This feature adds server-side search, quick filters, sorting, and overdue highlighting to the Todo dashboard for the authenticated user. Based on the current repository, `TaskController.taskDashboard` in `src/main/java/com/capstone/todo/web/TaskController.java` always fetches all tasks for the current user via `taskService.getUserTasks(username)`, and `src/main/resources/templates/tasks.html` renders them as a single list without any filter or search controls.

The scope of this plan covers:

- keyword search across task title and description
- query-parameter-driven filters for status, priority, and date bucket
- server-side sorting by planned finish date and priority
- overdue classification and clear visual marking for open tasks
- Thymeleaf UI, model, and CSS updates to support the dashboard controls
- unit and MVC test coverage for search, filters, sorting, and user isolation
- README documentation for the new dashboard capabilities

Out of scope: any database migration, persistence replacement, notifications, calendar integration, recurring tasks, admin/view capabilities, or a frontend SPA rewrite.

## Repository Context and Affected Components

Verified touchpoints in the current codebase:

- `src/main/java/com/capstone/todo/web/TaskController.java` - dashboard entry point for `GET /tasks`, edit, create, and complete flows
- `src/main/java/com/capstone/todo/service/TaskService.java` - service interface currently exposes raw fetching of user tasks
- `src/main/java/com/capstone/todo/service/impl/DefaultTaskService.java` - task business logic, username normalization, and date validation
- `src/main/java/com/capstone/todo/domain/TodoTask.java` - already contains title, description, status, priority, taskDate, and plannedFinishDate
- `src/main/resources/templates/tasks.html` - dashboard view that needs search/filter/sort controls and overdue indicators
- `src/main/resources/static/css/styles.css` - styles layer likely needing minimal updates for filter form and overdue highlighting
- `README.md` - needs documentation updates for the new dashboard behavior
- `src/test/java/com/capstone/todo` - existing test structure for domain, config, dto, service, and web layer will be extended

The ticket explicitly says no persistence format change is needed, and the required filterable fields already exist in the domain model, so the most likely touchpoints are the MVC, service, template, CSS, and testing layers.

## Assumptions / Open Questions

Assumptions supported by the ticket and repository:

- Query-parameter-driven server-side filtering is the expected delivery model for this release.
- Keyword search should be case-insensitive and match against both title and description, as required by the ticket.
- Overdue means `task.status == OPEN` and `task.plannedFinishDate.isBefore(LocalDate.now())`. Completed tasks should not be flagged as overdue.
- The "Today" bucket shows tasks where either `taskDate` or `plannedFinishDate` equals today, based on the acceptance criteria.
- The "Upcoming" bucket should include non-overdue tasks with a relevant date after today; the exact precedence between `taskDate` and `plannedFinishDate` should be clarified in code and tests.
- Priority sort should be explicit domain order (HIGH -> MEDIUM -> LOW), not alphabetic or enum ordinal behavior.
- Default /tasks behavior without parameters should remain backward-compatible by showing all tasks for the authenticated user.

Open questions to capture in the plan:

- Should create, edit, and complete redirects preserve current filter query parameters? The ticket only requires these flows to continue without errors, but preserving state would improve usability.
- Should "Upcoming" be keyed off `taskDate`, `plannedFinishDate`, or either of them analogous to "Today"? The ticket names the bucket but does not fully specify the edge case.
- What visual treatment is preferred for overdue tasks? The safest default is a badge and/or CSS highlight that fits the existing server-rendered UI.

## Proposed Technical Approach

1. **Introduce explicit dashboard query inputs**
   - Update `TaskController.taskDashboard()` to accept optional query parameters for `q`, `status`, `priority`, `dateFilter`, and `sort`.
   - Normalize and validate these values against controlled options so invalid inputs fall back to safe defaults rather than breaking the page.
   - Add the selected query state to the model so Thymeleaf can re-render the current control selections.

2. **Centralize search, filtering, sorting, and overdue derivation in the service layer**
   - Extend `TaskService` with a method that accepts search/filter/sort inputs, or introduce a small query object such as `TaskDashboardQuery`.
   - Implement the logic in `DefaultTaskService` on top of the existing user-scoped repository fetch so user isolation remains enforced before any filters are applied.
   - Use a deterministic pipeline: fetch user tasks -> apply keyword search -> apply status filter -> apply priority filter -> apply date bucket filter -> apply sorting.
   - Keep overdue rules in Java, not in Thymeleaf, to avoid duplicated date logic.

3. **Use a view-model or template-support strategy for derived task state**
   - Prefer a lightweight view DTO or model attribute structure that includes the `TodoTask` and a derived `overdue` flag.
   - This keeps the template simple and makes overdue rules reusable both for filtering and rendering.

4. **Update the dashboard UI using existing Thymeleaf patterns**
   - Add a GET form above the task list with a search input and selects for status, priority, date bucket, and sort.
   - Add a reset/clear option that returns the user to the default `/tasks` view.
   - Render a clear overdue label or CSS class for tasks that meet the overdue rule.
   - Ensure edit and complete actions remain available for open tasks when filters are active.

5. **Refactor controller model population for regression safety**
   - The create, edit, and update error paths currently repopulate the dashboard model manually.
   - Introduce a single private helper to populate tasks, forms, query state, and any derived display metadata so all render paths remain consistent.

## Implementation Steps (Sequenced)

1. **Introduce controlled dashboard query types**
   - Create enums or small controlled-value types for status filter, priority filter, date bucket, and sort option.
   - Consider a `TaskDashboardQuery` object that holds `q`, `status`, `priority`, `dateFilter`, and `sort`.
   - Give default values such as `ALL` and no-sort so existing behavior remains intact for raw `/tasks` requests.
 
2. **Extend the service interface**
   - Add a method such as `List<TodoTask> getUserTasks(String username, TaskDashboardQuery query)`.
   - Keep the existing `getUserTasks(String username)` method as a convenience default or have it delegate to the new overload with default query values.

 3. **Implement search, filtering, and sorting in `DefaultTaskService`**
   - Start from `taskRepository.findByUsername(normalizeUsername(username))` so the core user-isolation rule stays intact.
   - Apply case-insensitive keyword matching on `title` and `description`.
   - Apply status filter for ALL, OPEN, and COMPLETED.
   - Apply priority filter for ALL, HIGH, MEDIUM, and LOW.
   - Apply date bucket logic for ALL, TODAY, UPCOMING, and OVERDUE.
   - Apply sorting with comparators for planned finish date ascending and explicit priority rank.
   - Handle any null-safety needs defensively, even if form validation normally prevents null dates.
 
4. **Implement overdue helper behavior**
   - Create a reusable method such as `isOverdue(TodoTask task, LocalDate today)`.
   - Re-use this method for both the OVERDUE filter and the display-flag set on tasks shown in the dashboard.

5. **Refactor `TaskController` to support query parameters and consistent model building**
   - Update `taskDashboard()` to accept query parameters and pass them to the service.
   - Add a helper such as `populateDashboardModel(...)` that adds the filtered/sorted tasks, selected query state, form objects, and the username.
   - Use this helper in the normal dashboard path, the edit view path, and the create/update validation-error paths.
   - Ensure the mapping for `markTaskCompleted` continues to work without cross-user leakage.

6. **Update `tasks.html` with search and quick filter controls**
   - Add a search input for keyword.
   - Add selects for Status (All, Open, Completed), Priority (All, High, Medium, Low), Date (All, Today, Upcoming, Overdue), and Sort (none, planned finish date asc, priority high-to-low).
   - Add a submit action and clear-filters link.
   - Update the empty-state message to be appropriate for both no tasks and no matching results scenarios.
   - Add overdue badge or CSS indicator on task items if they are open and overdue.
 
7. **Update styles in `styles.css`**
   - Add minimal styles for the filter bar, search input, overdue badge, and any task highlight state.
   - Keep the visual treatment consistent with the existing card/button style.

8. **Update README**
   - Document that the dashboard now supports keyword search, quick filters by status, priority, and date bucket, sorting by planned finish date and priority, and overdue highlighting.
   - If the README includes sample flows, add an example of using the new tasks dashboard controls.
 
9. **Add or expand tests**
   - Service-level tests for search, filters, sort, and overdue classification.
   - Controller-level MVC tests for query parameter handling, model population, and regression safety on create/edit/complete flows.
   - If the repo already has an integration test pattern for web flows, include a lightweight end-to-end MVC scenario for combined filters.

## Testing Plan

**Unit tests - Service layer**

- keyword search returns only tasks where title or description match
- keyword search is user-scoped because filtering starts after `repository.findByUsername`
- status filter returns only OPEN tasks
- status filter returns only COMPLETED tasks
- priority filter returns only HIGH, MEDIUM, or LOW tasks as selected
- date bucket OVERDUE returns only open tasks with planned finish date before today
- date bucket TODAY matches tasks where task date or planned finish date equals today
- date bucket UPCOMING returns only future-aligned tasks per the implemented rule
- sort by planned finish date orders ascending
- sort by priority orders HIGH before MEDIUM before LOW
- overdue classification excludes completed tasks
- username normalization still applies under the new query method

**Controller-level tests**

- `GET /tasks?q=refile&status=OPEN&priority=HIGH&dateFilter=OVERDUE&sort=PRIORITY` populates the model with selected query state
- invalid query parameters fall back to safe defaults without erroring
- dashboard edit view still populates tasks, edit form, and query metadata
- create validation error render still includes the filtered panel data
- update validation error render still includes edit state and query state
- complete action still resolves only for the authenticated user

**Regression / manual smoke tests**

- log in as User A and verify that no User B tasks appear under any search or filter combination
- create tasks covering open, completed, today, upcoming, and overdue scenarios
- verify each filter individually and in combination
- verify both sort modes
- verify overdue open tasks are marked clearly
- verify completed past-due tasks are not marked overdue
- verify create, edit, and mark-completed still behave correctly

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
  - Mitigation: keep complex search/filter/overdue logic in Java and pass simple model attributes or view-model flags to the template.

- **Risk: Date-dependent tests become flaky.**
  - Mitigation: centralize the "today" calculation or introduce a testable clock pattern for service-level logic.

- **Risk: Controller error paths fail to repopulate search and filter metadata.**
  - Mitigation: use a single dashboard-model-population helper for all render paths.

- **Risk: Priority sort accidentally uses enum order or alphabetic order.**
  - Mitigation: add explicit comparators and assert the order in tests.

- **Risk: Query parameter names/allowed values drift between controller, service, and UI.**
  - Mitigation: define controlled types or constants and use them across layers.

## Definition of Done & Acceptance Criteria Mapping

- **DoD: `/tasks` dashboard accepts optional query parameters for keyword, status, priority, date bucket, and sort order.**
  - Covered by controller parameter handling, service query method, and template controls.

- **AC: Users can search by task title and description.**
  - Covered by keyword search logic and service tests.

- **AC: Users can filter by status: All, Open, Completed.**
  - Covered by status filter implementation, UI options, and tests.

- **AC: Users can filter by priority: All, High, Medium, Low.**
  - Covered by priority filter implementation, UI options, and tests.

- **AC: Users can filter by date bucket: All, Today, Upcoming, Overdue.**
  - Covered by date-bucket logic, UI options, and tests.

- **AC: Users can sort by planned finish date ascending and by priority High-to-Low.**
  - Covered by explicit comparators in service logic and order-verifying tests.

- **AC: Open overdue tasks are visually marked.**
  - Covered by overdue detection, template rendering, and styling.

- **AC: Filtering, searching, sorting, create, edit, and complete actions remain scoped to the authenticated user only.**
  - Covered by user-scoped repository fetch before filtering and regression testing.

- **AC: Tests cover search, filters, sorting, overdue classification, and user isolation.**
  - Covered by expanded service and controller tests.

- **DoD: README is updated with the new dashboard search/filter capabilities.**
  - Covered by documentation updates in `README.md`.

## Manual Validation Checklist

- Log in as User A and confirm only User A's tasks appear across search, filters, and sort combinations
- Create a mix of open, completed, today, upcoming, and overdue tasks
- Verify each filter individually and in combination
- Verify both sort options yield the expected order
- Verify an open overdue task shows a clear overdue label or highlight
- Verify a completed past-due task is not marked overdue
- Verify create, edit, and mark-completed flows still work without error
