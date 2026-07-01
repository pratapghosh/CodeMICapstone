# EPMCDMETST-52726 Implementation Plan

## Overview & Scope

This feature adds task deletion to the existing Spring Boot + Thymeleaf Todo app, with an explicit browser-level confirmation step and safe update of the file-backed JSON persistence. Based on verified repository context, the current codebase already has a layered task flow via `TaskService`, `DefaultTaskService`, `TaskController`, `TaskRepository`, `FileTaskRepository`, and `tasks.html`, but it only supports create, list, edit, and mark-completed operations.

The scope of this plan covers:

- adding a user-scoped delete operation to the service and repository layers
- extending the file-backed task storage to remove a task and persist the updated JSON file safely
- adding a POST-only controller endpoint for deletion that follows the existing Spring Security/CSRF form pattern
- updating the Thymeleaf dashboard to show a Delete control for each task with explicit confirmation
- preserving user isolation so that deletion only affects the authenticated user's normalized task file
- adding unit, repository, and controller tests for success, error handling, cross-user isolation, and file persistence
- updating README documentation for the new deletion workflow

Out of scope: bulk delete, archive/soft-delete, restore, recycle bin behavior, authentication changes, and any migration away from the current file-backed storage model.

## Repository Context & Affected Components

Verified touchpoints in the current codebase:

- `src/main/java/com/capstone/todo/service/TaskService.java` - service interface that currently exposes create, get, update, and markCompleted but no delete operation
- `src/main/java/com/capstone/todo/service/impl/DefaultTaskService.java` - business logic layer that already normalizes usernames, enforces task ownership via user-scoped lookups, and is the correct place to add deletion rules
- `src/main/java/com/capstone/todo/repository/TaskRepository.java`- repository interface that needs a new delete-capable method
- `src/main/java/com/capstone/todo/repository/impl/FileTaskRepository.java` - JSON/file persistence layer that currently supports read, save, findById, and update, and writes to `storage/tasks/<username>.json`
- `src/main/java/com/capstone/todo/web/TaskController.java` - MVC entrypoint that currently exposes `/tasks`, `/tasks/{taskId}/edit`, and `/tasks/{taskId}/complete` but no delete endpoint
- `src/main/resources/templates/tasks.html` - task dashboard template that currently renders Edit and Mark Completed controls but no Delete action
- `README.md` - feature and usage documentation that should be extended to mention task deletion
- `src/test/java/com/capstone/todo/service/impl/DefaultTaskServiceTest.java`- existing service-layer test patterns for normalization, error paths, and storage failure handling
- `src/test/java/com/capstone/todo/repository/impl/FileTaskRepositoryTest.java` - existing file-persistence test patterns using temporary directories
- `src/test/java/com/capstone/todo/web/TaskControllerTest.java` - existing controller-unit test patterns for redirects, binding, and service delegation

The application uses a simple layered architecture with Spring MVC, user-scoped file storage, and Thymeleaf templates, so the lowest-risk approach is to extend the existing patterns without changing architecture choices.

## Assumptions / Open Questions

Assumptions derived from the ticket and verified repository context:

- Hard delete is the intended behavior. The ticket explicitly rules out soft-delete, archive, and restore.
- Deletion must remain scoped to the authenticated user's normalized username, consistent with the existing service layer.
- Current Thymeleaf POST forms already rely on Spring Security CSRF protection, so the delete control should follow the same pattern.
- File persistence should continue using the existing read-modify-write approach via `FileStorageManager` without a new storage technology.
 - The controller keeps the current pattern of redirecting to `/tasks` after state-changing operations.

Open questions to capture in the plan (if product owners have preferences later):

- Should delete be available for both open and completed tasks? The ticket use case and acceptance criteria imply yes, so the plan assumes the Delete control will be shown for any task owned by the current user.
- Should the controller catch delete reclaims and re-render the dashboard, or should they be left to a global error handler? Based on current patterns, the most consistent approach is to handle clear business/storage errors locally in the controller and return to the dashboard with a global error message.
- Should the JSON file be deleted when the last task is removed, or should it remain an empty array? The safest and most consistent assumption for this codebase is to persist an empty list to the existing path rather than introducing conditional file deletion logic.

## Proposed Technical Approach

1. **Extend the service contract and business logic**
   - Add a `deleteTask(String username, String taskId)` method to `TaskService`.
   - Implement it in `DefaultTaskService` by normalizing the username, looking up the task in the current user's scope, and failing with `IllegalArgumentException("Task not found")` when the task is absent.
   - Delegate actual persistence to the repository after ownership is validated.
   - Keep the existing logging/storage-failure pattern consistent with `createTask` and `updateTask`.

2. **Extend the repository contract for hard delete**
   - Add a repository method such as `delete(String username, String taskId)` or `delete(TodoTask task)`.
   - Implement the method in `FileTaskRepository` by reading the user-task list, removing exactly one matching id, and writing the updated list back to `storage/tasks/<username>.json`.
   - Design the method to throw a clear `IllegalArgumentException` when the id is not found, matching existing update-style behavior.
   - Keep file rewrite behavior atomic as far as it is already abstracted by `FileStorageManager`; avoid partial in-memory mutations that aren't persisted.

3. **Add a POST-only MVC delete flow**
   - Add `@PostMapping("/tasks/{taskId}/delete")` to `TaskController`.
   - Delegate to `taskService.deleteTask(authentication.getName(), taskId)` and redirect to `/tasks` on success.
   - Handle `IllegalArgumentException` and `IllegalStateException` consistently with the existing create/update patterns: reload tasks, reset form models, return the `tasks` view, and surface a clear global error message.

4. **Update Thymeleaf UI with explicit confirmation**
   - Add a Delete form next to the existing Edit and Mark Completed controls in `tasks.html`.
   - Keep the control POST-based so it continues to work with SSRC/CSRF form protection.
   - Add a browser-native confirmation step using `onsubmit="return confirm('Are you sure you want to delete this task?')"` or the equivalent on the Delete form/button.
   - Ensure the Delete control is available for both open and completed tasks if the taging is user-owned, which matches the ticket scope.

5. **Preserve audit-safe file persistence behavior**
   - Reuse the existing read-modify-write flow in `FileTaskRepository`.
   - Verify that only the target task is removed and other tasks remain byte/data-intact after persistion.
   - Ensure no file corruption path occurs if a missing id is requested or a write fails.

6. **Update documentation and tests**
   - Extend `README.md` feature list and example workflow to include task deletion.
   - Add service, repository, and controller tests for happy paths, missing tasks, cross-user isolation, and file update verification.

## Implementation Steps (Sequenced)

 1. **Update contracts for deletion support**
   - Add a `deleteTask` method to `TaskService`.
   - Add a corresponding delete method to `TaskRepository`.
   - Keep method signatures user-scoped (`username`, `taskId`) so ownership checking remains explicit.

2. **Implement service-layer deletion logic**
   - In `DefaultTaskService`, normalize the username with the existing logic.
   - Use `taskRepository.findById(normalizedUsername, taskId)` to validate that the task exists in that user's scope.
   - If the task is not found, throw `IllegalArgumentException("Task not found")` to match the existing update/markCompleted behavior.
   - Call the repository delete method and catch/log `IllegalStateException` consistently with `createTask` and `updateTask`.

 3. **Implement file-backed repository deletion**
   - In `FileTaskRepository`, read the user's current task list.
   - Remove the task whose id matches the requested id.
   - If no task is removed, throw `IllegalArgumentException` with a clear "Task not found: <taskId>" or equivalent message.
   - Write the updated list back to the same user JSON file, even if the list becomes empty, to keep persistence behavior simple and consistent.

 4. **Add the controller endpoint and error handling**
   - In `TaskController`, add a new `@PostMapping("/tasks/{taskId}/delete")` method.
   - On success, redirect to `redirect:/tasks`.
   - On `IllegalArgumentException`, reload the tasklist, reinitialize `taskForm` and `editTaskForm`, add `username`, and populate a global error message before returning `tasks`.
   - On `IllegalStateException`, follow the same pattern but show a user-friendly persistence error, e.g. "Could not delete task. Please try again."

5. **Update the Thymeleaf dashboard**
   - In `tasks.html`, add a Delete form for each task with th:action pointing to `/tasks/{task.id}/delete`.
   - Use `method="post"` so Spring Security can apply the existing CSRF form handling.
   - Add a confirmation attribute on the form or button, for example: `onsubmit="return confirm('Are you sure you want to delete this task?')"`.
   - Extend the action control layout if needed so the new control fits cleanly next to Edit and Mark Completed.

6. **Update README documentation**
   - Add deletion to the feature list.
   - Update the sample workflow to mention that users can delete tasks from the dashboard after confirming.
   - If the README has a routes or main capabilities section, add the delete endpoint/behavior at a feature-level description (even if not listing raw URLs).

 7. **Add test coverage**
   - Extend `DefaultTaskServiceTest` with tests for successful deletion, missing task behavior, cross-user isolation via normalized username, and storage-failure rethrow logging patterns.
   - Extend `FileTaskRepositoryTest` with tests that confirm:
     - a task id is removed from the user JSON file
      - other tasks remain unchanged after deletion
      - deleting a missing id throws a clear exception
      - deletion of one user's task does not affect another user's file
   - Extend `TaskControllerTest` with tests for successful POST delete redirect, service-error model rendering, and delegation to taskService.

## Testing Plan

**Unit tests - Service layer**

- `DefaultTaskServiceTest` should cover:
  - deleteTaskShouldRemoveTaskForNormalizedUsername
  - deleteTaskShouldFailWhenTaskDoesNotExist
  - deleteTaskShouldRethrowStorageFailure
  - cross-user isolation via lookus in the normalized user scope

**Repository tests - File persistence**

- `FileTaskRepositoryTest` should cover:
  - deleteShouldRemoveOnlyTargetedTaskFromUserFile
  - deleteShouldLeaveOtherTasksUnchanged
  - deleteShouldFailWhenTaskDoesNotExist
  - file content after delete remains valid JSON
  - deletion in alice.json does not modify bob.json (cross-user isolation)

**Controller/web tests**

- `TaskControllerTest` should cover:
  - deleteTaskShouldRedirectWhenSuccessful
  - deleteTaskShouldReturnTasksViewWithGlobalErrorWhenServiceReportsTaskNotFound
  - deleteTaskShouldReturnTasksViewWithStorageErrorMessageWhenWriteFails
  - deleteTaskShouldDelegateToServiceWithAuthenticatedUsername

**Regression / manual smoke tests**

- Create multiple tasks for a user, delete one, and confirm the dashboard refreshes with the remaining tasks only
- Delete a completed task and confirm the same behavior works for non-open items
- Attempt to delete a non-existent id and confirm the app shows a clear error without damaging storage
- Log in as a second user and confirm one user's deletion does not acfect another user's tasks
- Run `mvn test` and confirm the full test suite still passes

## Test Data / Environment Needs

- No new infrastructure, database, or external services are required.
- Repository tests should continue to use temporary filesystem directories to verify JSON changes safely.
 - Test data should include at least:
  - one user with multiple tasks to verify selective deletion
  - an alternate user to verify cross-user isolation
  - a missing task id scenario for error-path testing
- Controller tests can continue to mock `Authentication` and `TaskService` as they already do.

## Deployment / Rollout Plan

- Deploy as a normal application release; no feature flag, migration, or configuration changes are required.
- Because the change is limited to application logic and the existing file format, rollout risk is low if tests pass.
- Post-deployment verification:
  - confirm task deletion works in the UI for a real user
  - confirm the corresponding `storage/tasks/<username>.json` file updates correctly
  - monitor logs for `Task not found` or write-level errors that might indicate regressions
- Rollback: revert the service, repository, controller, template, test, and README changes and redeploy. The rollback risk is low because deletion does not change schemas or external integrations.

## Risks & Mitigations

- **Risk: Deletion accidentally bypasses user ownership.**
  - Mitigation: keep username-normalized lookup and deletion in the service layer and add specific cross-user tests.

- **Risk: File persistence gets corrupted during delete.**
  - Mitigation: reuse the existing read/modify/write path and add repository tests that assert the removed id is gone while untargeted records remain intact.

- **Risk: Delete endpoint is implemented as GET or bypasses CSRF protection.**
  - Mitigation: use a POST-only controller method and a Thymeleaf form consistent with the existing mark-completed action.

- **Risk: Missing-task errors cause unhandled failures or poor UX.**
   - Mitigation: match the existing controller pattern by rendering the dashboard again with a clear message for business or storage errors.

- **Risk: UI Clutter from an additional action button.**
  - Mitigation: align the Delete control with the existing action button pattern and apply minimal styling adjustments only if the current layout cannot accommodate it cleanly.

## Definition of Done & Acceptance Criteria Mapping

- **DoD: `TaskService` exposes a delete operation that accepts `username` and `taskId`.**
  - Covered by updating `TaskService` and its tests.

- **DoD: `DefaultTaskService` validates task ownership with normalized username and throws a clear error when the task does not exist.**
  - Covered by service-layer deletion logic and missing-task/cross-user tests.

- **DoD: `TaskRepository` and the file-backed implementation support deleting a task and persisting the updated user task file safely.**
  - Covered by repository contract changes and file-persistence tests.

- **DoD: `TaskController` exposes a POST-only delete endpoint protected by existing form/CSRF conventions.**
  - Covered by controller and template updates.

 - **DoD: `tasks.html` shows a Delete action for each task and requires explicit user confirmation before submission.**
  - Covered by Thymeleaf form updates and manual/visual verification.

- **DoD: Unit/controller/repository tests cover successful deletion, missing task handling, cross-user isolation, and file persistence update.**
  - Covered by the testing plan above.

- **DoD: `README.md` documents task deletion in the feature list and sample workflow.**
  - Covered by documentation updates.

- **AC1: Given an authenticated user has a task on the dashboard, when they choose Delete and confirm, then the task is removed from the dashboard after redirect.**
  - Covered by controller, UI, and manual smoke tests.

- **AC2: Given an authenticated user deletes a task, when the corresponding `storage/tasks/<username>.json` file is read, then the deleted task id is no longer present and other tasks remain unchanged.**
  - Covered by repository persistence tests.

- **AC3: Given user A attempts to delete a task id that belongs to user B, when the delete operation runs with user A's username, then the task is not deleted and a not-found/error path is used.**
  - Covered by service and repository isolation tests.

 - **AC4: Given a delete request references a non-existent task id, when the service handles it, then a clear `IllegalArgumentException` or equivalent error is produced and no file corruption occurs.**
  - Covered by service and repository error-path tests.

- **AC5: Given the dashboard renders tasks, then each task has a POST-based Delete control with browser confirmation text before submission.**
  - Covered by template updates and manual ui inspection.

- **AC6: Given the implementation is complete, then `mvn test` passes with added tests for service, repository, and controller deletion behavior.**
  - Covered by the full test suite execution.

## Manual Validation Checklist

- Log in as a user with multiple tasks and confirm each task rows/card has a Delete control
- Click Delete and cancel the browser confirmation to ensure no request is submitted
- Click Delete and confirm to ensure the task disappears after redirect
- Verify the corresponding `storage/tasks/<username>.json` file no longer contains the deleted id, but still contains other tasks
- Attempt to delete a missing task id and confirm a clear, non-corrupting error path
- Verify that deleting a task for one user does not modify another user's tasks
- Run `mvn test` and confirm all existing and new tests pass
