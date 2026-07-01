const { expect } = require('@playwright/test');

class LoginPage {
    constructor(page) {
        this.page = page;
        this.usernameInput = page.getByLabel('Username');
        this.passwordInput = page.getByLabel('Password');
        this.signInButton = page.getByRole('button', { name: 'Sign In' });
    }

    async goto() {
        await this.page.goto('/login');
        await expect(this.page).toHaveTitle(/Login \| Todo App/);
    }

    async login(username, password) {
        await this.usernameInput.fill(username);
        await this.passwordInput.fill(password);
        await this.signInButton.click();
    }

    async expectInvalidCredentials() {
        await expect(this.page.getByText('Invalid username or password.')).toBeVisible();
    }
}

module.exports = {
    LoginPage
};