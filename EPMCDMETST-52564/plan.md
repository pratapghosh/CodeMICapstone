# EPMCDMETST-52564 Implementation Plan

## Overview & Scope

This feature adds recurring task scheduling to the Todo dashboard for authenticated users. Based on the current repository, `TaskForm` in `src/main/java/com/capstone/todo/dto/TaskForm.java` currently captures only title, description, task date, planned finish date, and priority; `DefaultTaskService.createTask` in `src/main/java/com/capstone/todo/service/impl/DefaultTaskService.java` creates a single `TodoTask` and persists it through `taskRepository.save(task)`, and `tasks.html` only renders the current task fields without recurrence controls or any identifier for generated occurrences.

The scope of this plan covers:

- domain and DTO extensions to capture recurrence type and optional recurrence end date
- service-layer occurrence generation for daily, weekly, and monthly schedules
- file-based persistence compatible changes so generated tasks survive restart
- dashboard and form updates to configure recurrence and label generated occurrences
- unit and/or MVC test coverage for no recurrence, daily, weekly, monthly, invalid date ranges, and user isolation
- README documentation for creating recurring tasks

Out of scope: notifications, reminders, calendar integrations, custom recurrence rules, timezone-aware scheduling, auto-regeneration beyond the selected end date, or any move away from the existing JSON file storage approach.

## Repository Context and Affected Components

Verified touchpoints in the current codebase:

- `src/main/java/com/capstone/todo/dto/TaskForm.java` - create/edit form DTO that needs recurrence fields and validation
- `src/main/java/com/capstone/todo/domain/TodoTask.java` - domain model that will likely need metadata for identifying generated recurring occurrences
- `src/main/java/com/capstone/todo/domain/TaskStatus.java` - existing OPEN/COMPLETED status rules should remain unchanged
- `src/main/java/com/capstone/todo/domain/Priority.java` - existing HIGH/MEDIUM/LOW quality order will continue to be copied to generated tasks
- `src/main/java/com/capstone/todo/service/TaskService.java`- service interface for task creation likely needs complex behavior behind the existing createTask contract
- `src/main/java/com/capstone/todo/service/impl/DefaultTaskService.java`- current creation logic, date validation, username normalization, and task update logic
- `src/main/java/com/capstone/todo/web/TaskController.java` - dashboard and creation flows that must support new form fields and validation errors without breaking existing edit/complete behaviors
- `src/main/resources/templates/tasks.html` - Thymeleaf dashboard that needs recurrence controls and a clear indicator for generated occurrences
- `src/main/resources/static/css/styles.css` - possible styling touchpoint for recurring task badges/hints
- `src/main/java/com/capstone/todo/repository/impl/FileTaskRepository.java` - JSON persistence layer that must remain compatible while storing any new task fields
- `README.md` - needs usage documentation for the new recurrence capability
- `src/test/java/com/capstone/todo` - existing test structure for domain, service, and web layer coverage

The current project uses a Spring Boot + Thymeleaf + Json file persistence architecture, so the least-risk approach is to add recurrence behavior in the service layer and keep the repository as a generic persistence mechanism for the resulting task records.

## Assumptions / Open Questions

Assumptions supported by the ticket and repository:

- Recurrence is applied only during task creation, not when editing existing tasks, because the ticket definition of done explicitly scopes the feature to creation and not bulke re-generation.
- The initial `taskDate` is the first occurrence and the application should create a single task when recurrence is none.
- `recurrenceEndDate` should be optional only when recurrence is none; for daily, weekly, and monthly, the end date is functionally required to bound generation and align with the acceptance criteria.
- generated occurrences copy title, description, priority, and status, and shift both `taskDate` and `plannedFinishDate` by the same interval.
- Monthly behavior should use `Java LocalDate.plusMonths` semantics as explicitly called out in the ticket.
- User isolation continues to be enforced by storing generated tasks under the current authenticated username only.
- Any new fields added to `TodoTask` must remain backward-compatible with existing JSON files that do not contain them.

Open questions to capture in the plan:

- Should recurring occurrences be identified by a simple boolean/badge, or should there also be a grouping key such as a recurrence series id? The ticket only requires clear identification, not series-level operations, so a minimal metadata approach is likely best.
- Should the edit form display recurrence fields for existing generated tasks, or should edit remain occurrence-agnostic? The ticket does not require editing of recurrence metadata, so the safest approach is to limit new controls to the create form only.
- Should `recurrenceEndDate` be mandatory for all non-none recurrence types? The ticket says there is an optional end date when recurrence is enabled, but it also states that the system should not auto-regenerate beyond the selected end date. If truly left optional, the implementation must choose a default bound, which the ticket does not define. The plan should therefore call out this assumption explicitly and prefer end date as required when recurrence is not none.

## Proposed Technical Approach

1. **Introduce explicit recurrence types and form validation**
   - Add a controlled enum such as `RecurrenceType` with values `NONE`, `DAILY`, `WEEKLY`, and `MONTHLY` under the domain package or a closely related type location.
   - Extend `TaskForm` with `recurrence` and `recurrenceEndDate` fields, and keep them aligned with existing Spring MVC form binding using `@DateTimeFormat` for the date field.
   - Add DTO-level validation for rules that can be enforced before service execution, especially requiring an end date when recurrence is daily, weekly, or monthly, and rejecting an end date before the initial `taskDate`.
   - If existing custom validation patterns are preferred, introduce a form-level custom validator/annotation rather than pushing all recurrence validation into the controller.
2. **Model recurring occurrence metadata in a backward-compatible way**
   - Extend `TodoTask` with minimal metadata that can support the requirement that the dashboard clearly identifies generated recurring occurrences.
   - The simplest structure is including a boolean such as `recurringOccurrence` and optionally a `seriesId` to allow future extensions without changing the persistence model again.
   - Keep new fields optional so older JSON tasks without them deserialize cleanly. The existing Jackson configuration should already handle missing fields as null/defaults, so the change should be additive only.
3. **Generate occurrences in the service layer at creation time**
   - Keep `TaskController.createTask` thin and continue delegating to `taskService.createTask`.
   - Updat `DefaultTaskService.createTask` to build a list of occurrences based on the selected recurrence type:
     - NONE: generate exactly one task, preserving current behavior.
     - DAILY: iterate by plusDays(1) until the end date inclusive.
     - WEEKLY: iterate by plusWeeks(1) until the end date inclusive.
     - MONTHLY: iterate by plusMonths(1) until the end date inclusive.
   - For each occurrence, shift both `taskDate` and `plannedFinishDate` by the same temporal interval between the base occurrence and the generated occurrence. This aligns with the ticket's assumption that planned finish date moves in parallel with task date.
   - Create each generated task with its own unique id, the normalized username, copied title/description/priority, `TaskStatus.OPEN`, and a createdAt timestamp.
   - If repository capabilities allow only single-record saves, loop through saves in a single service method with reasonable error handling and logging. If the file repository has an internal bulk-update pattern, consider a bulk save helper to reduce re-writes.
4. **Keep service-level validation as the final guard**
   - Retain the existing validation that `recurrenceEndDate` cannot be before `taskDate` and `plannedFinishDate` cannot be before `taskDate`.
   - Add service-level checks for any rule that should not be trusted to form binding alone, including handling missing or invalid recurrence values defensively.
   - Consider a small helper such as `generateOccurrenceDates(TaskForm form)` or `buildTasksForForm()` to keep `createTask` readable and testable.
5. **Update the Thymeleaf dashboard and the create form**
   - Add recurrence controls to the create task form in `tasks.html`, including a select for recurrence type and a date input for `recurrenceEndDate` with field-level error rendering.
   - Keep the edit form unchanged unless repository context shows that editing recurrence metadata is necessary. This reduces scope and avoids implying series-edit behaviors not required by the ticket.
   - Update the task list rendering to clearly identify generated occurrences, such as a "Recurring" badge, icon, or subtext label that is driven by the new task metadata.
   - If the UI includes conditional display for recurrence end date, keep it server-rendered and simple; JavaScript is not required to satisfy the ticket.
6. **Keep persistence and restart behavior intact**
   - Verify that `FileTaskRepository` persists the generated list of tasks using the existing per-user JSON files under `storage/tasks/<username>.json`.
   - Avoid any change that requires a data migration. Any new fields should default cleanly when missing from eligible older records.
   - If multiple saves are performed per recurring creation, check that the repository's file locking/append-update behavior prevents corruption. If necessary, explore a repository-level saveMany(...) or single-read/single-write implementation for the new flow.

## Implementation Steps (Sequenced)

1. **Inspect and align with existing validation and persistence patterns**
   - Review `FileTaskRepository` and any tests around task persistence to confirm the best place to support multi-occurrence saves.
   - Review existing test patterns for service and MVC layers to keep new tests consistent with the repository's conventions.

2. **Add recurrence type and form fields**
   - Create a `RecurrenceType` type with `NONE`, `DAILY`, `WEEKLY`, and `MONTHLY`.
   - Extend `TaskForm` with `recurrence` and `recurrenceEndDate` with a safe default of `NONE`.
   - Implement JSR or custom annotation-validation that enforces:
     - planned finish date must not be before task date
     - when recurrence is daily, weekly, or monthly, a recurrence end date is required
     - when recurrence is enabled, recurrence end date must not be before task date

3. **Extend the domain model for recurring occurrence identification**
   - Add minimal optional metadata to `TodoTask` such as `recurringOccurrence` and possibly `recurrenceSeriesId` or `recurrenceType`.
   - Update constructors, getters/setters, and domain tests as needed, keeping equals/hashCode behavior id-based to avoid changing existing collection semantics.

4. **Refactor service-layer creation logic**
   - Extract task building and date validation helpers in `DefaultTaskService` for readability.
   - Implement occurrence generation for NONE, DAILY, WEEKLY, and MONTHLY, using inclusive end-date logic.
   - Ensure each generated task:
     - has unique id
     - uses normalized username
     - copies title, description, and priority
     - sets status to `OPEN`
     - shifts both dates consistently
     - marks the task as a recurring occurrence when applicable
   - If needed, add a bulk save path or internal repository helper to reduce multiple file rewrites for a single recurring create operation.

5. **Update MVC controller for new form behavior**
   - Ensure `TaskController.taskDashboard` continues to initialize a `TaskForm` with default recurrence selection.
   - Update `TaskController.createTask` error paths so the new fields and validation messages re-render correctly.
   - Keep edit and complete flows functional without expanding scope to recurrence-aware bulku updates.

6. **Update the Thymeleaf template and styles**
   - Add create-form recurrence inputs to `tasks.html`.
   - Add field-error rendering for `recurrence` and `recurrenceEndDate`.
   - Add task-list indicators for recurring occurrences, keeping non-recurrence task rendering unchanged.
   - Update `styles.css` only as needed to maintain visual clarity.

7. **Update README**
   - Document how to create a recurring task, the available recurrence options, and how recurring occurrences appear on the dashboard.
   - Note any constraints, such as one-time generation between the initial task date and the recurrence end date.

8. **Add tests**
   - Expand service tests to cover occurrence generation, date shifting, and username isolation.
   - Add or expand MVC tests for form submission, validation failures, and dashboard rendering of recurrence indicators.

## Testing Plan

**Unit tests - Service layer**

- no-recurrence creation saves exactly one task and preserves existing behavior
- daily recurrence with end date three days after task date creates four tasks with daily date increments
- weekly recurrence creates occurrences spaced by seven days
- monthly recurrence uses `plusMonths` behavior and preserves the task-finish date offset
- invalid recurrence end date before task date rejects the creation and persists nothing
- planned finish date before task date remains invalid
- generated occurrences are saved with user-normalized ownership only
- generated occurrences are marked as open and carry the expected recurrence metadata

**Domain/persistence tests**

- `TodoTask` serialization/deserialization includes any new recurrence fields while remaining compatible with older json payloads
- file repository tests (or integration-style tests) confirm that generated occurrences survive read/write cycles

**Controller/MVC tests**

- `POST /tasks` with recurrence = NONE redirects successfully
- `POST /tasks` with daily/weekly/monthly input submits valid new fields
- invalid recurrence end date re-renders the form with clear validation errors
- dashboard rendering shows recurrence identifiers on generated tasks
- another user's tasks are not shown on the current user's dashboard

**Regression / manual smoke tests**

- register two users and confirm recurring tasks are only visible to their creator under `/tasks`
- create a daily recurrence series, restart the application, and confirm all generated tasks still appear
- create weekly and monthly examples, confirming date spacing and copied fields.
- ensure existing non-recurrence task creation, edit, and complete flows remain functional.

## Test Data / Environment Needs

- No new infrastructure, database, or external services are required.
- Tests should use stable, explicit LocalDate values to avoid flaky date-math assertions.
- File-persistence tests should use temporary directories or test-specific storage configuration so that runtime data is not polluted.

## Deployment / Rollout Plan

- Deploy as a standard application release; no database migration or configuration changes are expected.
- No feature flag is required; the default `none`)recurrence option keeps backward-compatible behavior for users who do not need the feature.
- Monitor application logs for task-creation failures, validation path issues, and JSON persistence errors after release.
- Rollback: revert domain, dto, service, template, test, and README changes and redeploy. Because new JSON fields should be additive and optional, rollback risk is low.

## Risks & Mitigations

- **Risk: Recurring creation causes partial persists if one occurrence save fails.**
  - Mitigation: prefer a single repository write path for multiple generated tasks or, at minimum, centralize the loop so error handling and logging are consistent.

- **Risk: Recurrence validation is duplicated across controller and service layers.**
  - Mitigation: keep user-facing form validation in JSR custom validators and keep service-layer guard checks as the authoritative backstop.

- **Risk: Month-end behavior is construed incorrectly.**
   - Mitigation: use and test `LocalDate.plusMonths` semantics directly rather than hand-rolling calendar logic.

- **Risk: UI scope grows into series editing or complete bulk operations.**
  - Mitigation: keep the CRUD changes explicitly limited to creation-time recurrence and display-only identification of generated occurrences.

- **Risk: Existing non-recurring behavior regresses.**
  - Mitigation: keep `NONE` as default, retain existing controller flow, and add explicit tests for the single-task creation path.

## Definition of Done & Acceptance Criteria Mapping

- **DoD: Task creation supports recurrence options: none, daily, weekly, and monthly.**
  - Covered by adding `RecurrenceType` and new form controls in `TaskForm` and `tasks.html`.

- **DoD: Users can provide an optional recurrence end date when recurrence is enabled.**
  - Covered by the new date field and the documented assumption/validation approach for non-none recurrence types.

- **Acceptance: Creating a recurring task generates the expected user-owned task occurrences with copied title, description, priority, status OPEN, and calculated task/planned finish dates.**
  - Covered by service-layer occurrence generation and unit tests for each recurrence type.

- **DoD: Generated occurrences are persisted in the existing file-based task storage and survive application restart.**
  - Covered by JSON compatibility checks and file-persistence tests/smoke tests.

- **DoD: The dashboard clearly identifies generated recurring occurrences without changing existing non-recurring task behavior.**
  - Covered by domain metadata, template indicators, and UI/MVC tests.

- **DoD: Validation prevents invalid recurrence inputs, including an end date before the initial task date.**
   - Covered by JSR custom validation and service guard checks, with controller tests for error rendering.

- **DoD: Unit and/or MVC tests cover daily, weekly, monthly, no-recurrence, invalid date range, and user isolation scenarios.**
  - Covered by the test plan above for service, domain/persistence, and MVC layers.

- **DoD: README is updated to document recurring task creation.**
  - Covered by documentation updates in `README.md`.

- **AC 1: Given I am logged in on the task dashboard, when I create a task with recurrence = none, then exactly one task is created and existing create behavior is unchanged.**
   - Covered by the NONE path and regression tests.

- **AC 2: Given I create a task with daily recurrence and an end date three days after the task date, then four OPEN task occurrences are created for the task date and each following day through the end date.**
   - Covered by daily generation tests.

- **AC 3: Given I create a task with weekly recurrence, then generated occurrences are spaced seven days apart through the recurrence end date.**
  - Covered by weekly generation tests.

- **AC 4: Given I create a task with monthly recurrence, then generated occurrences are spaced one month apart through the recurrence end date using LocalDate month arithmetic.**
  - Covered by monthly generation tests using `plusMonths` semantics.

- **AC 5: Given recurrence is daily, weekly, or monthly, when the recurrence end date is before the task date, then the form is rejected with a clear validation error and no tasks are saved.**
  - Covered by JSR and MVC tests for validation failure.

- **AC 6: Given recurring occurrences are generated, when I view the dashboard, then each occurrence is visible only to my user account and includes the copied title, description, priority, and OPEN status.**
  - Covered by username-normalization, repository scoping, and dashboard tests.

- **AC 7: Given recurrence occurrences are generated, when the application restarts, then the generated tasks are still available from the existing file-based storage.**
  - Covered by persistence/integration tests and manual smoke verification.

- **AC 8: Given another user creates recurring tasks, when I view my dashboard, then those generated occurrences are not displayed to me.**
  - Covered by user-isolation tests across service and MVC layers.

## Manual Validation Checklist

- Create a standard non-recurrence task and confirm only one task appears
- Create a daily recurrence series and verify inclusive end-date behavior
- Create a weekly recurrence series and verify 7-day spacing
- Create a monthly recurrence series for a month-end start date and verify LocalDate month behavior
- Attempt to submit recurrence with an end date before the task date and confirm the form is rejected
- Restart the application and confirm generated occurrences remain visible
- Log in as a second user and confirm the first user's recurring tasks are not visible
- Confirm the dashboard clearly marks recurring occurrences while existing non-recurring tasks remain unchanged
