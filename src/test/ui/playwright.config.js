const path = require('path');
const { defineConfig, devices } = require('@playwright/test');

const targetDir = path.join(__dirname, '..', '..', '..', 'target');

module.exports = defineConfig({
    testDir: path.join(__dirname, 'specs'),
    timeout: 30_000,
    fullyParallel: true,
    forbidOnly: !!process.env.CI,
    retries: process.env.CI ? 2 : 0,
    reporter: [
        ['list'],
        ['html', { outputFolder: path.join(targetDir, 'playwright-report'), open: 'never' }]
    ],
    outputDir: path.join(targetDir, 'playwright-results'),
    use: {
        baseURL: 'http://localhost:8080',
        trace: 'on-first-retry',
        screenshot: 'only-on-failure',
        video: 'retain-on-failure'
    },
    projects: [
        {
            name: 'chromium',
            use: {
                ...devices['Desktop Chrome']
            }
        }
    ]
});