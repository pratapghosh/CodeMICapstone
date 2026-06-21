---
description: "Frontend development rules for Thymeleaf templates, server-rendered flows, and project CSS patterns."
applyTo: "src/main/resources/templates/**,src/main/resources/static/css/**"
---

# Frontend Instructions

## Scope

Apply these rules for Thymeleaf templates and CSS in this project.

## UI Architecture

- Keep server-rendered Thymeleaf pages as the default approach.
- Preserve current template flow and routes:
  - `login.html`
  - `register.html`
  - `tasks.html`
- Do not convert to SPA architecture unless explicitly requested.

## Design Principles

- Keep views simple, readable, and task-focused.
- Follow separation of concerns: structure in templates, presentation in CSS, behavior primarily on server side.
- Prefer consistency and reusability over one-off visual tweaks.

## Design Patterns

- Component-like template sections for repeated UI blocks (forms, cards, task rows) while keeping templates easy to scan.
- BEM-like class naming discipline for CSS changes where practical.
- Progressive enhancement: pages should remain functional without client-heavy scripting.

## Template Rules

- Keep form field names and bindings compatible with backend DTO/controller expectations.
- Keep authentication-related pages simple and consistent.
- Avoid introducing client-only logic that duplicates backend validation.

## Styling Rules

- Reuse and extend styles from `src/main/resources/static/css/styles.css`.
- Prefer consistent spacing, typography, and component structure across pages.
- Make changes responsive for desktop and mobile where layout changes are introduced.

## UX and Accessibility

- Keep labels, placeholders, and button text explicit and action-oriented.
- Ensure forms remain keyboard-friendly and readable.
- Preserve error/success message visibility where applicable.

## Change Discipline

- Prefer targeted edits over broad visual rewrites.
- Keep markup clean and readable.
- If a UI change impacts behavior, ensure backend/template integration still works.
