# EPMCDMETST-52376 Implementation Plan

## Overview & Scope

This feature adds server-side filtering, sorting, and overdue visual indicators to the Todo dashboard for the authenticated user. The current dashboard in `src/main/java/com/capstone/todo/web/TaskController.java` always fetches all user tasks via `taskService.getUserTasks(username)` and `src/main/resources/templates/tasks.html` renders a single list without any filter or sort controls.

The plan covers the addition of:

- query-parameter-driven dashboard filters for status, date bucket, and priority
- server-side sorting by planned finish date and priority
- overdue classification for open tasks
- Thymeleaf UI controls and visual marking
- test coverage for filtering, sorting, and overdue behavior
- README updates for the new dashboard capabilities

Out of scope: any data storage migration, database introduction, notifications, admin capabilities, or recurring task behavior.

## Repository Context and Affected Components

Verified touchpoints in the current codebase:

- `src/main/java/com/capstone/todo/web/TaskController.java`: entry point for `GET /tasks`, edit, create, and complete flows
- `src/main/java/com/capstone/todo/service/TaskService.java``: service interface currently exposes only raw `getUserTasks` fetching
- `src/main/java/com/capstone/todo/service/impl/DefaultTaskService.java``: task business logic and date validation
- `src/main/java/com/capstone/todo/domain/TodoTask.java``: already contains `status`, `priority`, `taskDate`, and `plannedFinishDate`
- `src/main/resources/templates/tasks.html`: dashboard view that will need filter/sort forms and overdue styling
- `README.md``: needs documentation for the new dashboard behavior
- `src/test/java/com/capstone/todo/web/TaskControllerTest.java``: existing controller-using unit tests for tasks flows
- `src/test/java/com/capstone/todo/service/impl/DefaultTaskServiceTest.java``: existing service-level tests using TestNG + Mockito

No repository or persistence change appears necessary because the ticket explicitly says to keep file-system persistence unchanged and the required fields already exist in the domain model.

## Assumptions / Open Questions

Assumptions that follow directly from the ticket and repo context:

- Filtering and sorting will be implemented on the server side using query parameters on `/tasks` to match the ticket assumption.
- Overdue means `task.status == OPEN` and `task.plannedFinishDate.isBefore(LocalDate.now())`. Completed tasks should not be flagged as overdue.
- The "Today" date bucket in acceptance criteria is interpreted as shown when either taskDate or plannedFinishDate equals today.
- Priority sort will be domain-specific (HIGH before MEDIUM before LOW) rather than default enum or alphabetic order.
- Sorting by planned finish date will be ascending order because the acceptance criteria explicitly call for ascending order.
- The UI can use a simple GET form or query-string links without JavaScript, consistent with the current Thymeleaf approach.

Open questions to capture in the plan but not block the implementation:

- Should the UI allow a combination of multiple filters at once? The ticket implies yes, so the planning assumes combinable status + date + priority filters.
- Should the selected filters be preserved after create, edit, and complete actions? The ticket only requires these flows not to break; preserving query parameters on redirect would be nice-to-have but is not explicitly required.
- What visual treatment is preferred for overdue by UX/design? The repo currently has a simple server-rendered UI, so a label and/or CSS class based highlight is the safest approach.

## Proposed Technical Approach
1. **Introduce explicit dashboard query criteria**
   - Add a small set of query parameters in `TaskController.taskDashboard()` for `status`, `dateFilter`, `priority`, and `sort`.
   - Provide default values such as `ALL` to keep backward compatibility with `/tasks`.
   - Expose the currently selected view state to the model so Thymeleaf can render selected options.
2. **Centralize filtering, sorting, and overdue classification in the service layer**
   - Extend `TaskService` with a new method or a purpose-built query method that accepts filter/sort inputs.
   - Implement the logic in `DefaultTaskService` on top of the existing `getUserTasks` fetch so user isolation continues to be enforced before any filtering occurs.
   - Use Java streams or collection transformation for status filtering, date bucket filtering, priority filtering, and sorting by planned finish date or custom priority rank.
   - Keep overdue calculation in the service or a dedicated view-model helper to avoid duplicating date logic in the template.
3. **Use a view-model or template-support strategy for overdue rendering**
   - Prefer model attributes or a lightweight view DTO that carries both the `TodoTask` and derived flags such as `overdue`.
   - This keeps Thymeleaf expressions simple and makes the overdue rule reusable between filtering and rendering.
4. **Update the dashboard UI to expose controls**
   - Add a small GET form at the top of the task list with selects for status, date bucket, priority, and sort.
   - Render the overdue label or styling on tasks that meet the overdue rule.
   - Ensure edit and complete actions remain available for open tasks under filtered views.
   - Update CSS if necessary under `src/main/resources/static/css/` to support an overdue badge or highlight style.
5. **Preserve existing behavior for create, edit, and complete**
   - Refactor the controller to reuse a single method for populating the dashboard model both for initial page load and error re-render scenarios.
   - This will help avoid inconsistencies since create and update error paths currently manually repopulate `tasks`, `taskForm`, and `editTaskForm`.

## Implementation Steps (Sequenced)
1. **Introduce filter/sort domain model or simple constants**
   - Create enums or controlled-value types for dashboard status filter, dashboard date filter, dashboard priority filter, and dashboard sort option.
   - This reduces string-literal coupling across controller, service, tests, and templates.
2. **Extend the service interface**
   - Add a method such as `List<TodoTask> getUserTasks(String username, DashboardQuery query)` or an equivalent that accepts explicit filter and sort parameters.
   - Keep the existing `getUserTasks(String)` in place if it is already used elsewhere, or implement it as a default to the new overload with ALL filters to minimize breaking changes.
3. *+Implement filtering and sorting in `DefaultTaskService`**
   - Start from `taskRepository.findByUsername(normalizeUsername(username))` so querying remains user-scoped.
   - Apply filters in a deterministic order, for example status, then date bucket, then priority.
   - Apply sorting at the end using comparators, including a custom comparator for priority rank (HIGH = 1, MEDIUM = 2, LOW = 3).
   - Handle null safety if any date fields can be null despite form validation.
4. **Add overdue helper behavior**
   - Implement a method such as `isOverdue(TodoTask task, LocalDate today)` and reuse it both for the OVERDUE filter and for UI markering.
5. **Refactor `TaskController`**
   - Update `taskDashboard()` to accept query parameters and pass them to the service.
   - Introduce a private helper such as `populateDashboardModel(...)` to add the filtered/sorted task list, current selection values, and overdue rendering metadata.
   - Update edit and validation-error flows to recalculate the same model structure.
6. **Update `tasks.html`**
   - Add a GET form above the task list with selects for Status, Date, Priority, and Sort.
   - Render an overdue badge, text, or CSS class for overdue open tasks.
   - Consider showing an empty-state message that reflects current filters.
7. **Update styles**
   - If a stylesheet exists under `static/css/styles.css`, add minimal styles for filter controls, overdue badges, or overdue row bordering.
   - Keep styling consistent with the existing card/button UI language.
8. **Update README**
   - Add the new dashboard capabilities under Features and/or Sample Workflow.
   - Document that tasks can be filtered by status, date, and priority, sorted by planned finish date and priority, and that overdue open tasks are highlighted.
9. *+Add or expand tests**
   - Service-level tests for filtering and sorting logic
   - Controller-level tests for model population and query parameter handling
   - Regression tests for create, edit, and complete flows to ensure no breakage

## Testing Plan
**Unit tests (Service)**
- status filtering returning only OPEN tasks
- status filtering returning only COMPLETED tasks
- date filtering for OVERDUE excluding completed tasks
- date filtering for TODAY matching task date or planned finish date
- date filtering for UPCOMING matching future non-overdue work
- priority filtering for HIGH/MEDIUM/LOW
- sorting by planned finish date ascending
- sorting by priority with HIGH before MEDIUM before LOW
- username normalization remains in effect when filtering is invoked

**Controller-level tests**
- `taskDashboard` passes query parameters to the service or populates model with selected filter state
- filter/error render still includes the required model attributes
- edit dashboard flow retains dashboard metadata
- existing create/update/complete behavior remains passing

**Integration/regression tests**
- If the repo already has integration test patterns, add a lightweight MVC or application context test for `GET /tasks?status=OPEN&dateFilter=OVERDUE&sort=PRIORITY`. If no such pattern exists, service + controller unit tests are sufficient to meet the ticket's DoD.
**Test data and environment needs**
- Use deterministic LocalDate values around a fixed today date for tests; if the code directly calls `LocalDate.now()`, tests may need to build data relative to the runtime date or introduce a testable clock helper.
  - No new external environment, database, or infrastructure is required.

## Deployment / Rollout Plan
- Deploy as a standard application release; no migrations, backfills, or configuration changes are required.
- No feature flag is strictly necessary because the change is server-side and backward-compatible for default `/tasks` access.
- Monitoring should focus on application error logs around task dashboard rendering and any unexpected Thymeleaf expression failures.
- Rollback: revert the controller/service/template/README changes and redeploy. Persisted task data remains unchanged, so rollback risk is low.

## Risks & Mitigations
- **Risk: Thymeleaf template logic becomes cluttered**
  - Mitigation: keep complex classification in Java (service or view-model) and pass simple flags to the view.
- **Risk: Date-dependent tests become flaky**
  - Mitigation: centralize the "today" calculation or use deterministic test data relative to a controllable reference date.
- **Risk: Controller error paths fail to repopulate filter metadata**
  - Mitigation: use a single `populateDashboardModel` method for all dashboard render paths.
- **Risk: Sorting by priority might accidentally use enum natural order**
  - Mitigation: add explicit comparator tests that assert HIGH > MEDIUM > LOW in the rendered result sequence.
- **Risk: Query parameter names or values become inconsistent between view and controller**
   - Mitigation: define constants or enums used by both the controller and the template model.

## Definition of Done & Acceptance Criteria Mapping
- **AC1: Status = Open shows only open tasks**
  - Covered by service filtering implementation, controller parameter handling, and unit tests.
- **AC2: Status = Completed shows only completed tasks**
  - Covered by status filter logic and tests.
- **AC3: Date = Overdue shows only open tasks past due**
  - Covered by overdue helper, date filter logic, and tests.
- **AC4: Date = Today matches task date or planned finish date**
  - Covered by today bucket logic and tests.
- **AC5: Priority filter limits results**
  - Covered by priority filter logic and tests.
- **AC6: Sort by planned finish date ascending**
  - Covered by sort comparator and tests.
- **AC7: Sort by priority with HIGH before MEDIUM before LOW**
  - Covered by custom priority comparator and tests.
- **AC8: Filters and sorting are scoped to the authenticated user only**
  - Covered by continued use of user-scoped repository fetch before any collection filtering.
- **AC9: Overdue open tasks have a clear visual indicator**
  - Covered by template changes and styling updates.
- **AC10: Create/edit/complete flows continue to work**
  - Covered by controller refactor, regression tests, and manual smoke check.
- *+DoD: Unit or controller-level tests exist**
  - Covered by expanded `TaskControllerTest` and `DefaultTaskServiceTest`.
- *+DoD: README is updated**
  - Covered by documentation changes in `README.md`.

## Manual Validation Checklist
- Log in as User A and confirm only User A's tasks appear across all filter combinations
- Create a mix of open, completed, overdue, today, and upcoming tasks
- Verify each filter individually and in combination
- Verify sort by planned finish date and sort by priority order
- Verify an open overdue task shows an overdue badge or highlight
- Verify a completed past-due task is not marked overdue
- Verify create, edit, and mark-completed actions still work and return to the dashboard