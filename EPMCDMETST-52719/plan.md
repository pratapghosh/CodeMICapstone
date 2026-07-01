# EPMCDMETST2-52719 Implementation Plan

## Overview & Scope

This feature adds in-app reminder dates and dashboard overdue/reminder alerts to the existing Spring Boot + Thymeleaf Todo app. Based on verified repository context, `TodoTask` in `src/main/java/com/capstone/todo/domain/TodoTask.java` currently persists `taskDate` and `plannedFinishDate` but has no `reminderDate`, `TaskForm` in `src/main/java/com/capstone/todo/dto/TaskForm.java` only captures title/description/dates/priority, `TaskController` in `src/main/java/com/capstone/todo/web/TaskController.java` only lists, creates, edits, and completes tasks, and `src/main/resources/templates/tasks.html` renders dates and status but has no reminder or overdue bindings.

The scope of this plan covers:

- adding an optional `reminderDate` to the task domain and form model
- ensuring JSON file-backed persistence remains backward-compatible when older task records omit the new field
- adding create/edit validation that rejects a reminder date after the planned finish date
- updating the dashboard to highlight open tasks when reminders are due or tasks are overdue
- keeping completed tasks from showing active alerts - no notification delivery
- adding unit, MVC, and/or integration tests for persistence, validation, dashboard behavior, and user isolation
- updating README documentation for the new reminder behavior

Out of scope: email, SMS, push, or background notifications; calendar integration; database migrations; multi-user sharing; and changes to authentication/authorization.

## Repository Context & Affected Components

Verified touchpoints in the current codebase:

- `src/main/java/com/capstone/todo/domain/TodoTask.java` - core domain model that needs an additive `reminderDate` field and possibly derived helper methods or UI model attributes
- `src/main/java/com/capstone/todo/dto/TaskForm.java` - create/edit form DTO that needs optional reminder date binding and validation
- `src/main/java/com/capstone/todo/web/TaskController.java` - dashboard model population, create flow, edit flow, and validation error rendering
- `src/main/resources/templates/tasks.html` - create/edit forms and task-list UI that need reminder input and alert/badge rendering
- `src/main/resources/static/css/styles.css` - likely styling touchpoint for reminder-due and overdue indicators
- `src/main/java/com/capstone/todo/service/TaskService.java` and `src/main/java/com/capstone/todo/service/impl/DefaultTaskService.java` - business rules for task creation/update and the best place to centralize reminder/overdue determination if not kept in view-model logic
- `src/main/java/com/capstone/todo/repository/impl/FileTaskRepository.java` - JSON persistence layer that must continue loading older records without reminderDate
- `README.md` - documentation that needs new reminder behavior sections
- `src/test/java/com/capstone/todo` - existing test packages for domain, dto, service, repository, integration, and web layers

The architecture is a Spring Boot MVC application with user-scoped file based storage, so the least-risk approach is an additive domain/DTO/view update plus business-rule validation in the existing service layer.

## Assumptions / Open Questions

Assumptions derived from the ticket and verified repo context:

- Reminders are in-app dashboard indicators only; no asynchronous or out-of-band delivery is required.
- `reminderDate` should use `LocalDate`, not time-of-day, because the ticket explicitly scopes to date-only behavior.
- Existing JSON task records may omit `reminderDate` and must continue to deserialize as no-reminder without migration.
- Overdue and reminder-due behavior applies only to open tasks; completed tasks should not render active alerts.
- User isolation remains enforced via existing service/repository boundaries and must not be broadened by any dashboard logic.

Open questions to capture in the plan:

- Should a task show both reminder-due and overdue indicators if both conditions are true? The ticket demands both highlights, but the UI/UX may need a clear priority or combined presentation.
- Should validation for `reminderDate <= plannedFinishDate` be implemented at DTO level, service level, or both? The safest plan is both: user-friendly DTO validation plus a service guard.
- Should the edit form allow clearing a previously set reminder date? The ticket implies that reminder date is optional, so the plan should preserve that behavior.
- Proposed plan assumption: active overdue indicator should take visual precedence over reminder-due because it is a stronger state, but the task can still show both labels if the existing UI can support it clearly.

## Proposed Technical Approach

1. **Extend the domain model additively**
   - Add a nullable LocalDate `reminderDate` field to `TodoTask`.
   - Keep the change backward-compatible by making it optional and not requiring any JSON migration. Jackson should default missing fields to null for older records.
   - Update constructors, getters/setters, and any domain tests that build `TodoTask` instances.

2. **Extend form binding and validation**
   - Add nullable `reminderDate` to `TaskForm` with `@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)` so Thymeleaf can bind the date input on create and edit.
   - Implement user-facing validation for the new business rule: if `reminderDate` is present and `plannedFinishDate` is present, reject the form when `reminderDate` is after plannedFinishDate`.
   - Consider a level-class custom validator/annotation in `src/main/java/com/capstone/todo/dto/validation` so related date-relationship validation stays out of the controller.

3. **Enforce the rules in the service layer as the final guard**
   - Review `TaskService` and its implementation to keep task creation and update behaviors centralized.
   - Add service-level checks so invalid reminder dates cannot slip through if form validation is bypassed.
   - Ensure both create and update flows map `taskForm.getReminderDate()` to the persisted domain model.
   - If the current service already computes sorting or task list presentation, consider adding derived helper methods or model attributes for `reminderDue` and `overdue`; if not, keep the computation in the controller or Thymeleaf expressions where it is readable.

4. **Update the MVC controller and view model**
   - Ensure `taskDashboard`  in `TaskController` provides all model data needed to render reminder/overdue state for the current user only.
   - Update existing error paths in create and edit flows so the new `reminderDate` input and validation messages re-hydrate correctly.
   - Avoid any change that exposes other users' tasks when computing alert states. All evaluation should continue after retrieving the existing user-scoped task list.

5. **Update Thymeleaf templates and styles**
   - Add a reminder date input to the create task form and edit task form in `tasks.html`.
   - Render user-friendly field-level and/or global error messages for invalid reminder date input.
   - Update task list items to display the reminder date when present and to highlight:
     - open tasks with reminder date == today or earlier as reminder-due
      - open tasks with planned finish date < today as overdue
      - completed tasks with no active alert styling even if dates match those conditions
   - Add minimal CSS classes for clear visual differentiation (e.g. reminder badge, overdue badge, row border/background changes) while keeping the existing layout stable.

6. **Keep JSON persistence and backward compatibility intact**
   - Verify if file task repository reads and writes `TodoTask` with the new `reminderDate` field without requiring migrations.
   - Confirm old JSON records that lack `reminderDate` load cleanly and behave as "no reminder."
   - Avoid format changes that would break read-rollback compatibility.

## Implementation Steps (Sequenced)

1. **Inspect existing service, repository, and test patterns**
   - Review `DefaultTaskService`, `TaskService`, and `FileTaskRepository` to confirm how task creation, update, sorting, and persistence currently work.
   - Review existing tests under service, repository, and web packages to keep new coverage consistent with current style.

2. **Add the new reminder field to the domain and form model**
   - Extend `TodoTask` with `reminderDate`.
   - Extend `TaskForm` with `reminderDate` and basic date binding annotations.
   - Update any mapping helpers such as `TaskController.mapToForm` to prepopulate reminder date during edit.

3. **Implement the validation rules**
   - Add a DTO-level custom validation that ensures reminder date is not after plannedFinishDate.
   - Retain or extend existing service-level date rule checks so both create and update are protected even without mosting validation.
   - Ensure error messages are clear and align with the ticket's "reminder date cannot be after planned finish date" requirement.
n4. **Update task create/edit business logic**
   - Modify `taskService.createTask` and `updateTask` implementations to persist `reminderDate`.
   - Keep existing normalization, user scoping, priority mapping, and completion behavior untouched.
   - If task list retrieval is the best place to centralize alert state, add deterministic helpers that compute for `LocalDate.now()` or an injectable clock for testability; otherwise keep alert logic in the web model/template.

5. **Update controller and Thymeleaf views**
   - Add `reminderDate` input elements to the create and edit forms in `tasks.html`.
   - Display the reminder date on task cards/rows when present.
   - Add powerful indicators for:
      - reminder due: open task and reminder date <= today
      - overdue: open task and planned finish date < today
    - Ensure completed tasks don't show active reminder/overdue indicators.
   - Keep the edit experience aligned with the create form so reminder settings are editable.

6. **Add or update styles**
   - Introduce classes for reminder-due and overdue visual treatment in `styles.css`.
   - Keep styles accessible and consistent with existing color usage.

7. **Update documentation**
   - Expand `README.md` to describe how reminder dates work.
   - Document that older JSON task files without reminder dates remain supported.
   - Describe that overdue and reminder alerts are in-app indicators only.

8. **Add test coverage**
   - Add unit tests for deserialization/building of `TodoTask` with missing and present `reminderDate` values.
   - Add DTO/validation tests for valid and invalid reminder date relationships.
   - Add service tests for create/update persistence and user scoping.
   - Add MVC/template or integration tests for dashboard indicators, completed task suppression, and user isolation.

## Testing Plan

**Unit tests - Domain/DTO/validation**

- `TodoTask` serialization/deserialization with `reminderDate` present
- older JSON payloads without `reminderDate` deserialize without failure and default to null
- `TaskForm` validation accepts empty reminder date
- `TaskForm` validation rejects `reminderDate > plannedFinishDate` with a clear message

**Service layer tests**

- creating a task with a valid reminder date persists the value correctly
- updating a task can add, change, or clear a reminder date
- service rejects a reminder date after the planned finish date even if DTO validation is bypassed
- user isolation is kept intact when loading and updating tasks

**Controller/MVC tests**

- `POST /tasks` with a valid reminder date redirects successfully
- `POST /tasks` with an invalid reminder date re-renders the dashboard with validation error
- `POST /tasks/{taskId}/edit` upholds the same rules
- dashboard rendering shows reminder due indicators only for open tasks whose reminder date is today or earlier
- dashboard rendering shows overdue indicators only for open tasks whose planned finish date is past
- completed tasks with matching dates do not show active alert
- another user's tasks are not evaluated or rendered on the current user's dashboard

**Regression / manual smoke tests**

- create a task without a reminder date and confirm existing flows remain unchanged
- create a task with reminder date = today and confirm the reminder-due indicator appears
- create a task with planned finish date in the past and confirm the overdue indicator appears
- mark the task completed and confirm active alerts disappear
- restart the app and confirm old and new task files load correctly

## Test Data / Environment Needs

- No new infrastructure, background jobs, or external services are required.
- Tests should use fixed `LocalDate` values or an injectable clock where necessary to avoid flaky "today" assertions.
- File-system persistence tests should use temporary test directories so they do not pollute runtime storage.

## Deployment / Rollout Plan

- Deploy as a normal application release; no feature flag, database migration, or configuration changes are expected.
- Because the change is additive, existing task JSON files should continue to load without backfill.
- Look at application logs after release for validation rejections, JSON read/write errors, or template rendering issues.
 - Rollback: revert the domain, dto, controller, template, style, test, and README changes and redeploy. Additive JSON fields keep rollback risk low.

## Risks & Mitigations

- **Risk: Validation is implemented only at the UI.**
  - Mitigation: enforce the same rule in the service layer so non-MVC callers or tests cannot persist invalid data.

- **Risk: "Today" based logic creates flaky tests.**
  - Mitigation: centralize date comparison logic and use a testable clock or explicit test dates where possible.

- **Risk: Older JSON tasks fail to load after the model change.**
  - Mitigation: keep the new field optional and add integration/serialization tests for missing-field compatibility.

- **Risk: Overdue and reminder-due UI become visually confusing.**
  - Mitigation: use clear, consistent badges or styles and document intended priority in the implementation.

- **Risk: User isolation could be accidentally broken by new dashboard logic.**
  - Mitigation: compute indicators only on the eligible tasks already returned for the current authenticated user and add tests for cross-user scenarios.

## Definition of Done & Acceptance Criteria Mapping

- **DoD: `TodoTask` supports an optional `reminderDate` persisted backward-compatibly in existing JSON files.**
  - Covered by an additive domain model change, repository compatibility verification, and serialization/test coverage.

- **DoD: `TaskForm` and validation support optional reminder date entry on create and edit.**
  - Covered by DTO, template, and MVC updates.

- **DoD: Reminder date cannot be after plannedFinishDate, and invalid input shows a user-friendly validation error.**
  - Covered by custom form validation, service guards, and MVC error-path tests.

- **DoD: Task dashboard visually highlights open tasks whose reminder date is today or earlier.**
  - Covered by controller/template/style updates and tests for reminder-due indicator rendering.

- **DoD: Task dashboard visually highlights open tasks whose planned finish date is before today as overdue.**
  - Covered by dashboard conditional logic and tests.

- **DoD: Completed tasks do not show active reminder/overdue alerts.**
  - Covered by template conditionals and regression tests.

- **DoD: Unit/controller tests cover reminder persistence, validation, dashboard behavior, and user isolation.**
  - Covered by the testing plan above.

- **DoD: README documents reminder behavior and file-format compatibility notes.**
  - Covered by README documentation updates.

- **AC: Given I create a task with a reminder date on or before the planned finish date, when the task is saved, then the reminder date is stored and displayed with the task.**
  - Covered by domain/persistence changes, template updates, and service/MVC tests.

- **AC: Given I create or edit a task with a reminder date after the planned finish date, when I submit the form, then the task is not saved and a validation error is shown.**
  - Covered by DTO validation, service guards, and MVC error-path tests.

- **AC: Given an open task has `reminderDate` equal to today or earlier, when I view the dashboard, then the task shows a reminder-due indicator.**
  - Covered by dashboard conditionals and rendering tests.

- **AC: Given an open task has `plannedFinishDate` before today, when I view the dashboard, then the task shows an overdue indicator.**
  - Covered by dashboard conditionals and rendering tests.

- **AC: Given a completed task has a reminder date or an overdue planned finish date, when I view the dashboard, then no active reminder/overdue alert is shown.**
  - Covered by conditional UI logic and regression tests.

- **AC: Given existing task JSON records do not contain `reminderDate`, when the app loads them, then they remain readable and default to no reminder.**
  - Covered by JSON compatibility tests.

- **AC: Given users A and B have tasks, when user A views reminders and overdue alerts, then only user A's tasks are evaluated and displayed.**
  - Covered by service/MVC user-isolation tests.

## Manual Validation Checklist

- Create a task without a reminder date and confirm the dashboard behaves as before
- Create a task with a valid reminder date and confirm it displays on the task
- Edit the task to change or clear the reminder date
- Attempt to save a task with `reminderDate > plannedFinishDate` and confirm a clear error is shown
- View open tasks with reminder date = today or in the past and confirm reminder-due highlighting
- View open tasks with planned finish date in the past and confirm overdue highlighting
- Mark these tasks completed and confirm the active alerts are removed
- Load pre-existing task JSON data without `reminderDate` and confirm it still works
- Log in as a second user and confirm only that user's tasks are evaluated and displayed
