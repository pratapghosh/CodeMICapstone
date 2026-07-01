const { test, expect } = require('@playwright/test');
const { buildUser } = require('../../fixtures/credentials');
const { LoginPage } = require('../../pages/login-page');
const { RegisterPage } = require('../../pages/register-page');
const { TasksPage } = require('../../pages/tasks-page');

test.describe('authentication', () => {
    test('user can register and sign in', async ({ page }) => {
        const user = buildUser();
        const registerPage = new RegisterPage(page);
        const loginPage = new LoginPage(page);
        const tasksPage = new TasksPage(page);

        await registerPage.goto();
        await registerPage.register(user);

        await expect(page).toHaveURL(/\/login\?registered$/);
        await expect(page.getByText('Registration successful. Please sign in.')).toBeVisible();

        await loginPage.login(user.username, user.password);
        await tasksPage.expectLoaded(user.username);
    });

    test('shows an error for invalid credentials', async ({ page }) => {
        const loginPage = new LoginPage(page);

        await loginPage.goto();
        await loginPage.login('unknown-user', 'wrong-password');
        await loginPage.expectInvalidCredentials();
    });
});