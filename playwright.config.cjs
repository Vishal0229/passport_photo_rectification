const { defineConfig } = require('@playwright/test');
const path = require('path');

module.exports = defineConfig({
  testDir: './test',
  testMatch: 'e2e.test.cjs',
  timeout: 15000,
  use: {
    browserName: 'chromium',
    headless: false,
    slowMo: 300,
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },
  reporter: [['list'], ['html', { outputFolder: 'test/report', open: 'never' }]],
});
