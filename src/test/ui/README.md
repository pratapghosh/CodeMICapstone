# UI Tests

This folder contains Playwright-based UI tests for the Todo application.

Structure:

- `fixtures/` for generated test data and reusable inputs
- `pages/` for Playwright page objects
- `specs/` for end-to-end scenarios grouped by feature

Run from the repository root:

```bash
npm install
npx playwright install chromium
npm run ui:test
```

Assumption: the application is already running at `http://localhost:8080` before the UI suite starts.