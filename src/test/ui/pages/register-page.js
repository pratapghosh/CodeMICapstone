const { expect } = require('@playwright/test');

class RegisterPage {
    constructor(page) {
        this.page = page;
        this.usernameInput = page.getByLabel('Username');
        this.fullNameInput = page.getByLabel('Full Name');
        this.passwordInput = page.getByLabel('Password', { exact: true });
        this.confirmPasswordInput = page.getByLabel('Confirm Password');
        this.registerButton = page.getByRole('button', { name: 'Register' });
    }

    async goto() {
        await this.page.goto('/register');
        await expect(this.page).toHaveTitle(/Register \| Todo App/);
    }

    async register(user) {
        await this.usernameInput.fill(user.username);
        await this.fullNameInput.fill(user.fullName);
        await this.passwordInput.fill(user.password);
        await this.confirmPasswordInput.fill(user.password);
        await this.registerButton.click();
    }
}

module.exports = {
    RegisterPage
};