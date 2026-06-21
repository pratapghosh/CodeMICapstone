---
description: "Test development rules for unit and integration tests, naming, and behavior-focused assertions in the Todo app."
applyTo: "src/test/java/**"
---

# Test Instructions

## Scope

Apply these rules to all test code under `src/test/java/**`.

## Test Design

- Prefer behavior-focused tests with clear Arrange-Act-Assert flow.
- Keep each test targeted to one behavior.
- Use descriptive test names that state expected outcomes.
- Avoid unnecessary coupling to implementation details.

## Design Principles

- Test behavior, not private implementation details.
- Keep tests isolated, deterministic, and easy to diagnose.
- Keep test setup minimal and explicit.

## Test Patterns

- Arrange-Act-Assert for method-level tests.
- Given-When-Then naming/readability style where it improves clarity.
- Use test doubles (mocks/stubs) at service boundaries when isolating units.
- Prefer focused integration tests at controller/repository boundaries.

## Coverage Priorities

- Validate user-scoped task behavior to prevent cross-user access.
- Cover service-level business rules and validation paths.
- Cover repository/storage edge cases for file-based persistence.
- Include controller tests for auth and request validation paths where relevant.

## Reliability

- Keep tests deterministic and independent.
- Avoid hidden ordering dependencies between tests.
- Use realistic but minimal fixtures/test data.

## When Changing Production Code

- Add or update tests in the same change when behavior is modified.
- Prefer extending existing test classes in the same package area.
- Ensure `mvn test` passes before considering changes complete.

## Style

- Keep assertions explicit and readable.
- Keep setup concise; extract helpers only when reused.
- Keep test code as maintainable as production code.
