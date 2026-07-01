# EPMCDMETST-52800 Implementation Plan

## Overview & Scope

This feature adds a user-scoped task deletion capability to the existing Spring Boot + Thymeleaf Todo app. Based on the Jira ticket and verified repository context, the application already supports creating, editing, listing, and marking tasks completed for the authenticated user, persisting them in user-specific JSON files under `storage/tasks/<username>.json`. However, there is no delete operation in the current controller, service, repository, or Thymeleaf task list UI.

The scope of this plan covers:

- adding a user-scoped delete operation through the repository, service, and controller layers
- adding a POST `/tasks/{taskId}/delete` endpoint that uses the authenticated principal and never accepts a username parameter
- updating the Thymeleaf task list to include a Delete action with a browser confirmation prompt
- removing deleted tasks from the corresponding `storage/tasks/<username>.json` file
- preserving strict user isolation so that only the owning user's tasks can be deleted
- adding unit, repository, and/or controller tests for success, missing task, and cross-user safety
- updating `README.md` to document the delete-task capability

Out of scope: bulk delete, archive, soft delete, undo/restore, admin deletion of other users' tasks, database migration, or any changes to the existing authentication/authorization model.

## Repository Context & Affected Components

Verified touchpoints in the current codebase:

- `README.md` - describes the app as a Spring Boot + Thymeleaf Todo app with clean controller/service/repository/storage layering, file-based persistence, and strict user data isolation
- `src/main/java/com/capstone/todo/web/TaskController.java` - referenced in the ticket as the current MVC controller with `/tasks`, `/tasks/{taskId}/edit`, and `/tasks/{taskId}/complete` but no delete endpoint
- `src/main/java/com/capstone/todo/service/TaskService.java` - referenced in the ticket as existing service abnstraction with no delete operation today
- `src/main/java/com/capstone/todo/repository/TaskRepository.java` and file-backed implementations - referenced in the ticket as persistence touchpoints that currently lack delete support
- `src/main/resources/templates/tasks.html` - referenced in the ticket as the task list view that currently renders Edit and Mark Completed actions but no Delete action
- `src/test/java` and existing test structure - README confirms unit and integration test capability already exists under `src/test/java`
- `storage/tasks/<username>.json` - verified by the ticket and README as the user-wise persistence location

The lowest-risk approach is to extend the existing layered pattern, reuse the current file-locking/write utility, and keep user scoping derived exclusively from `Authentication.getName()`.

## Assumptions / Open Questions

Assumptions derived from the ticket and verified repository context:

- Hard deletion is acceptable for this feature because the ticket explicitly excludes soft delete, archive, and undo/restore behavior.
- Existing CSRF protection for POST forms is already enabled and should continue to apply to the new delete action without special cases.
- Missing or cross-user task deletion should follow the application's existing not-found/or error-handling conventions rather than introducing a new error model.
- Task ids are stable enough to be used for deletion lookup within the user-scoped task collection.
- The existing file locking/storage utility can be reused to perform safe read-modify-write operations on the user's task JSON file.

Open questions to capture in the plan:

- When a delete on a missing or cross-user task fails, does the current convention redirect with a flash message, return a tmperror page, or surface a 404? The plan assumes the implementation should match the existing pattern once verified in code.
- Should the Delete action be hidden for completed tasks or available for all owned tasks? The ticket does not restrict this, so the plan assumes all owned tasks remain deletable.
- If the repository currently locates a task by ID within a user file, should deletion return a boolean, an Optional, or throw the same domain exception used by the edit/complete flows? The preferred approach is to reuse the existing service-level convention for not-found handling.

## Proposed Technical Approach
1. **Extend the existing layered pattern rather than introducing a new one**
   - Add a delete-oriented method to the repository interface and its file-backed implementation.
   - Add a corresponding business method to TaskService that takes only the authenticated user's username and the task id.
   - Extend TaskController with a POST delete action that reads the username from `Authentication`, calls the service, and redirects back to `/tasks`.
2. **Keep user isolation as the first-class constraint**
   - Never accept a username, user id, or owner identifier as a request parameter for deletion.
   - Resolve the user solely from the authenticated principal and scope repository lookup to that user's JSON file.
   - If the task is not present in that user's file, fail safely using the existing not-found/error convention without mutating any other file.
3. *+Implement file-backed deletion as a read-modify-write operation**
   - Load the user's current task list from `storage/tasks/<username>.json` using the existing storage utility.
   - Locate and remove the matching task by id in memory.
   - Persist the updated list back to the same file using the existing file locking/write mechanism, so the operation remains consistent with current storage patterns.
4 . **Update the Thymeleaf UI with a confirmed destructive action**
   - Add a Delete button or form for each task in `tasks.html`.
   - Use a POST form so CSRF protection continues to apply naturally.
   - Add a browser-native confirmation prompt (such as `onsubmit="return confirm('...')"`) or equivalent pattern aligned with the current template style.
5. **Preserve current error handling and test strategy**
   - Match the existing error handling pattern used by edit and complete flows for missing tasks.
   - Add tests at the repository, service, and controller levels to cover success, failure, and user isolation.
   - Update README to include delete as a supported part of the task lifecycle.

## Implementation Steps (Sequenced)
0. **Verify existing conventions before coding**
   - Inspect the current TaskController, TaskService implementation, TaskRepository, and file-backed repository class to confirm method naming, not-found conventions, and storage utility usage.
   - Inspect `tasks.html` to match existing form/CSRF/button patterns when adding Delete.
   - Inspect existing tests (especially controller and repository tests) to align new tests with the repo's current style.

1. **Extend the repository abstraction and implementation**
   - Add a delete method to `TaskRepository` that operates within a user-scoped collection, for example deleting by `username` and `taskId`.
   - Implement the method in the file-backed repository by:
      - reading the user's task list
      - locating the task by ID
      - removing it from the list
      - writing the updated list back to the same json file
   - Return a meaningful outcome (boolean, Optional, exception, etc.) consistent with existing repo conventions so upper layers can handle missing tasks correctly.
2. **Extend the service layer with user-scoped deletion**
   - Add a delete method to `TaskService` that takes `username` and `taskId`.
   - In the service implementation, delegate to the repository using the authenticated username so user scoping is centralized in the service boundary.
   - If existing service methods throw a not-found exception for missing tasks, reuse that same pattern for deletion so controller behavior remains consistent.
3. **Add the MVC controller endpoint**
   - Implement `@VostMapping("/tasks/{taskId}/delete")`  in `TaskController`.
   - Read the username from `Authentication.getName()`.
   - Call the new service delete method with the authenticated username and path variable `taskId`.
   - Return a redirect to `/tasks` on success.
   - For missing/cross-user task, follow the current controller error-handling convention verified in step 0.

4 . **Update the Thymeleaf task list**
   - Add a Delete action alongside existing Edit and Mark Completed actions for each task.
   - Implement it as a POST form so CSRF token handling matches the application's existing form patterns.
   - Add a confirmation prompt that clearly communicates that the action is permanent.
   - Keep the markup and styling changes minimal and consistent with the current dashboard layout.

5. **Update documentation**
   - Add delete-task capability to the README features list.
   - Update the sample workflow to include deleting a task from the dashboard.
   - If the documentation mentions core task lifecycle capabilities, reflect that delete is now part of that lifecycle.

#b# Testing Plan

**Unit and repository tests**

- Should cover that deleting a valid task id removes exactly one task from the authenticated user's data set.
- Should cover that after deletion the updated `storage/tasks/<username>.json` file no longer contains the task.
- Should cover missing task behavior (return value or exception, depending existing convention).
- Should cover cross-user isolation by setting up tasks for two users and verifying that deleting for user A does not change user B's data.
- Should cover that deletion of a completed task behaves the same as deletion of an open task, unless existing business rules explicitly prevent it.

**Service tests**

- Verify the service invokes the repository with the authenticated username rather than any client-supplied identifier.
- Verify missing-task behavior is exposed to the controller in the same way as edit/complete behavior.

**Controller / MVC tests**

- Extend `TaskControllerTest` (or equivalent MVC test) to cover:
  - successful POST `/tasks/{taskId}/delete` redirects to `/tasks`
  - delegation to the service using `Authentication.getName()`
  - safe behavior on missing tasks according to existing error handling
  - confirmation that the route is POST-only and follows existing Security configuration (if covered by existing MVC/security tests)

**Regression / manual smoke tests**

- Log in as a user with multiple tasks and confirm each task shows a Delete action.
- Click Delete and cancel the browser confirmation to confirm no request is submitted.
- Click Delete and confirm the prompt to verify the task disappears after redirecting back to `/tasks`.
- Inspect the user's JSON file locally to confirm the task entry was removed.
- Attempt to submit another user's task id while authenticated as the current user and confirm the other user's data remains unchanged.
- Submit a non-existent task id and confirm the app handles it without data loss or unexpected server failure.

## Test Data / Environment Needs

- No new infrastructure, database, migration, or external service is required.
- Test data should include:
  - a user with several tasks
  - a user with no tasks as another baseline scenario
  - at least two users to verify isolation
   - tasks in different states (expected open and completed)
- Repository tests may need temporary file-system directories or the repo's existing storage test utilities to avoid touching real runtime data.
- Controller tests can continue to mock `Authentication` and the service layer as in existing tests.

## Deployment / Rollout Plan

- Deploy as a normal application release; no feature flag, config change, or data migration is required.
- Post-deployment verification:
  - log in as a test user and delete a task
   - confirm the task disappears from the dashboard
   - confirm the corresponding JSON file is updated correctly
   - confirm missing/invalid deletion requests fail safely
- Monitor application logs for unexpected exceptions around the new delete flow.
- Rollback: revert controller, service, repository, template, tests, and documentation changes and redeploy. There is no schema or migration risk because the feature mutates only existing user task files through normal app behavior.

## Risks & Mitigations

- **Risk: Cross-user deletion due to improper scoping.**
  - Mitigation: drive the entire flow from `Authentication.getName()` and user-scoped repository lookups, and add explicit tests for cross-user inputs.

- **Risk: Delete metadata or file writes can corrupt the user's JSON file.**
  - Mitigation: reuse the existing file locking and writing utility, keep the operation as a single read-modify-write cycle, and add repository tests for persisted outcomes.

- **Risk: The delete action circumvents CSRF protection or uses the wrong HTTP method.**
  - Mitigation: use a POST form in Thymeleaf rather than a get link, and reuse existing form patterns.

- **Risk: Missing-task behavior is inconsistent with existing edit/complete flows.**
  - Mitigation: verify current error-handling conventions before implementing and align deletion to them rather than introducing a new pattern.

## Definition of Done & Acceptance Criteria Mapping

- **DoD: Add a user-scoped delete operation to TaskRepository, TaskService, and the file-backed repository implementation.**
   - Covered by repository and service interface/implementation changes and tests.

- *+DoD: Add a POST `/tasks/{taskId}/delete` controller action that uses `Authentication.getName()` and never accepts username/user-id parameters.**
   - Covered by controller implementation and MVC tests.

- *+DoD: Add a Delete button for each task in the Thymeleaf task list with a browser confirmation prompt.**
   - Covered by `tasks.html` update and manual smoke validation.

- *+DoD: Deleted tasks are removed from the corresponding `storage/tasks/<username>.json` file and no longer appear after redirecting to `/tasks`.**
   - Covered by repository tests, controller redirect tests, and manual smoke validation.

- *+DoD: Attempts to delete a missing or cross-user task fail safely using existing not-found/error handling conventions.**
   - Covered by service/controller tests and manual validation.

- *+DoD: Unit/controller tests cover successful deletion, missing task behavior, and user isolation.**
   - Covered by the testing plan above.

- *+DoD: `README.md`  is updated to document the delete-task capability.**
   - Covered by documentation updates.

- **AC1: Given an authenticated user with one or more tasks, when they view `/tasks`, each task displays a Delete action alongside existing task actions.**
   - Mapped to Thymeleaf template changes and manual smoke testing.

- **AC2: Given the user clicks Delete, when the browser confirmation is accepted, the app submits POST `/tasks/{taskId}/delete` and redirects back to `/tasks`.**
   - Mapped to controller endpoint, form submission, and MVC tests.

- **AC3: Given the task belongs to the authenticated user, when deletion completes, the task is removed from the user's JSON storage file and is no longer shown on the dashboard.**
   - Mapped to repository implementation, service/controller flow, integration-type tests, or manual verification.

- **AC4: Given the task id does not exist for the authenticated user, when POST `/tasks/{taskId}/delete` is submitted, the application handles it safely without deleting any other task.**
   - Mapped to missing-task tests, error-handling alignment, and manual validation.

- **AC5: Given another user's task id is submitted, when POST `/tasks/{taskId}/delete` is called by the authenticated user, the other user's task remains unchanged and inaccessible.**
   - Mapped to user-isolation tests across repository and service layers.

- **AC6: Automated tests verify repository deletion behavior, service user scoping, controller redirect behavior, and missing-task/cross-user safety.**
   - Mapped directly to the testing plan and new test implementations.
