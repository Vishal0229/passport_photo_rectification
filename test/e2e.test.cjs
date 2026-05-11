/**
 * E2E tests for calorie-tracker.html — runs in a real Chromium browser.
 * Run: npm run test:e2e
 */
const { test, expect } = require('@playwright/test');
const path = require('path');

const APP_FILE = path.resolve(__dirname, '..', 'calorie-tracker.html');
const APP_URL  = `file:///${APP_FILE.replace(/\\/g, '/')}`;

// Clear localStorage before each test so tests are isolated
test.beforeEach(async ({ page }) => {
  await page.goto(APP_URL);
  await page.evaluate(() => localStorage.clear());
  await page.reload();
  await page.waitForLoadState('domcontentloaded');
});

/* ══════════════════════════════════════════════════════════
   PAGE LOAD
══════════════════════════════════════════════════════════ */
test.describe('Page load', () => {
  test('shows header title', async ({ page }) => {
    await expect(page.locator('header h1')).toHaveText('Calorie Tracker');
  });

  test('shows food input, gram input and add button', async ({ page }) => {
    await expect(page.locator('#food-input')).toBeVisible();
    await expect(page.locator('#gram-input')).toBeVisible();
    await expect(page.locator('#add-btn')).toBeVisible();
  });

  test('starts with 0 items and empty state message', async ({ page }) => {
    await expect(page.locator('#item-count')).toHaveText('0 items');
    await expect(page.locator('#meal-list')).toContainText('No items yet');
  });

  test('results panel is hidden on load', async ({ page }) => {
    await expect(page.locator('#results-panel')).toBeHidden();
  });

  test('history panel shows empty state', async ({ page }) => {
    await expect(page.locator('#history-list')).toContainText('No saved meals yet');
  });
});

/* ══════════════════════════════════════════════════════════
   AUTOCOMPLETE
══════════════════════════════════════════════════════════ */
test.describe('Autocomplete', () => {
  test('typing shows dropdown suggestions', async ({ page }) => {
    await page.fill('#food-input', 'chi');
    await expect(page.locator('#autocomplete-list')).toBeVisible();
    await expect(page.locator('#autocomplete-list li')).not.toHaveCount(0);
  });

  test('"chi" dropdown includes chicken breast', async ({ page }) => {
    await page.fill('#food-input', 'chi');
    await expect(page.locator('#autocomplete-list')).toContainText('chicken breast');
  });

  test('clicking a suggestion fills the food input', async ({ page }) => {
    await page.fill('#food-input', 'app');
    await page.locator('#autocomplete-list li').filter({ hasText: 'apple' }).first().click();
    await expect(page.locator('#food-input')).toHaveValue('apple');
  });

  test('clicking suggestion moves focus to gram input', async ({ page }) => {
    await page.fill('#food-input', 'app');
    await page.locator('#autocomplete-list li').first().click();
    await expect(page.locator('#gram-input')).toBeFocused();
  });

  test('Escape key closes the dropdown', async ({ page }) => {
    await page.fill('#food-input', 'bro');
    await expect(page.locator('#autocomplete-list')).toBeVisible();
    await page.keyboard.press('Escape');
    await expect(page.locator('#autocomplete-list')).toBeHidden();
  });

  test('ArrowDown highlights the first item', async ({ page }) => {
    await page.fill('#food-input', 'chi');
    await page.keyboard.press('ArrowDown');
    await expect(page.locator('#autocomplete-list li.highlighted')).toHaveCount(1);
  });

  test('Enter on highlighted item selects it', async ({ page }) => {
    await page.fill('#food-input', 'salmon');
    await page.keyboard.press('ArrowDown');
    await page.keyboard.press('Enter');
    await expect(page.locator('#food-input')).toHaveValue('salmon');
    await expect(page.locator('#autocomplete-list')).toBeHidden();
  });

  test('clearing input hides the dropdown', async ({ page }) => {
    await page.fill('#food-input', 'chi');
    await expect(page.locator('#autocomplete-list')).toBeVisible();
    await page.fill('#food-input', '');
    await expect(page.locator('#autocomplete-list')).toBeHidden();
  });

  test('unknown food shows no suggestions', async ({ page }) => {
    await page.fill('#food-input', 'xyznotafood');
    await expect(page.locator('#autocomplete-list')).toBeHidden();
  });
});

/* ══════════════════════════════════════════════════════════
   ADDING ITEMS
══════════════════════════════════════════════════════════ */
test.describe('Adding food items', () => {
  test('adding a valid item appears in the meal list', async ({ page }) => {
    await page.fill('#food-input', 'apple');
    await page.fill('#gram-input', '150');
    await page.click('#add-btn');
    await expect(page.locator('#meal-list')).toContainText('apple');
    await expect(page.locator('#meal-list')).toContainText('150g');
  });

  test('item count badge updates', async ({ page }) => {
    await page.fill('#food-input', 'banana');
    await page.fill('#gram-input', '120');
    await page.click('#add-btn');
    await expect(page.locator('#item-count')).toHaveText('1 item');
  });

  test('inputs clear after adding', async ({ page }) => {
    await page.fill('#food-input', 'apple');
    await page.fill('#gram-input', '100');
    await page.click('#add-btn');
    await expect(page.locator('#food-input')).toHaveValue('');
    await expect(page.locator('#gram-input')).toHaveValue('');
  });

  test('adding three items shows all and correct count', async ({ page }) => {
    for (const [name, grams] of [['chicken breast','200'],['brown rice','150'],['broccoli','100']]) {
      await page.fill('#food-input', name);
      await page.fill('#gram-input', grams);
      await page.click('#add-btn');
    }
    await expect(page.locator('#item-count')).toHaveText('3 items');
  });

  test('Enter in gram input adds the item', async ({ page }) => {
    await page.fill('#food-input', 'egg');
    await page.fill('#gram-input', '60');
    await page.press('#gram-input', 'Enter');
    await expect(page.locator('#meal-list')).toContainText('egg');
  });

  test('decimal grams (22.5) are accepted', async ({ page }) => {
    await page.fill('#food-input', 'oats');
    await page.fill('#gram-input', '22.5');
    await page.click('#add-btn');
    await expect(page.locator('#meal-list')).toContainText('22.5g');
  });
});

/* ══════════════════════════════════════════════════════════
   VALIDATION ERRORS
══════════════════════════════════════════════════════════ */
test.describe('Input validation', () => {
  test('unknown food shows error', async ({ page }) => {
    await page.fill('#food-input', 'notrealfood');
    await page.fill('#gram-input', '100');
    await page.click('#add-btn');
    await expect(page.locator('#food-error')).toContainText('not found');
  });

  test('empty food name shows error', async ({ page }) => {
    await page.fill('#gram-input', '100');
    await page.click('#add-btn');
    await expect(page.locator('#food-error')).toContainText('Please enter a food name');
  });

  test('empty grams shows error', async ({ page }) => {
    await page.fill('#food-input', 'apple');
    await page.click('#add-btn');
    await expect(page.locator('#gram-error')).toContainText('Please enter a quantity');
  });

  test('zero grams shows error', async ({ page }) => {
    await page.fill('#food-input', 'apple');
    await page.fill('#gram-input', '0');
    await page.click('#add-btn');
    await expect(page.locator('#gram-error')).toContainText('greater than 0');
  });

  test('negative grams shows error', async ({ page }) => {
    await page.fill('#food-input', 'apple');
    await page.fill('#gram-input', '-50');
    await page.click('#add-btn');
    await expect(page.locator('#gram-error')).toContainText('greater than 0');
  });
});

/* ══════════════════════════════════════════════════════════
   REMOVE ITEMS
══════════════════════════════════════════════════════════ */
test.describe('Removing items', () => {
  test('X button removes the item', async ({ page }) => {
    await page.fill('#food-input', 'apple');
    await page.fill('#gram-input', '100');
    await page.click('#add-btn');
    await page.locator('#meal-list .btn-danger').first().click();
    await expect(page.locator('#meal-list')).toContainText('No items yet');
  });

  test('removing one item keeps the others', async ({ page }) => {
    for (const [n, g] of [['apple','100'],['banana','120'],['egg','60']]) {
      await page.fill('#food-input', n);
      await page.fill('#gram-input', g);
      await page.click('#add-btn');
    }
    await page.locator('#meal-list .btn-danger').first().click();
    await expect(page.locator('#meal-list')).not.toContainText('apple');
    await expect(page.locator('#meal-list')).toContainText('banana');
    await expect(page.locator('#item-count')).toHaveText('2 items');
  });

  test('removing an item hides results panel', async ({ page }) => {
    await page.fill('#food-input', 'apple');
    await page.fill('#gram-input', '100');
    await page.click('#add-btn');
    await page.click('#calculate-btn');
    await expect(page.locator('#results-panel')).toBeVisible();
    await page.locator('#meal-list .btn-danger').first().click();
    await expect(page.locator('#results-panel')).toBeHidden();
  });
});

/* ══════════════════════════════════════════════════════════
   NUTRITIONAL VALUE CALCULATION
══════════════════════════════════════════════════════════ */
test.describe('Nutritional Value calculation', () => {
  test('calculate with no items shows warning toast', async ({ page }) => {
    await page.click('#calculate-btn');
    await expect(page.locator('#toast')).toBeVisible();
    await expect(page.locator('#toast')).toContainText('Add at least one food item');
  });

  test('results panel appears after calculating', async ({ page }) => {
    await page.fill('#food-input', 'apple');
    await page.fill('#gram-input', '150');
    await page.click('#add-btn');
    await page.click('#calculate-btn');
    await expect(page.locator('#results-panel')).toBeVisible();
  });

  test('200g chicken breast shows 330 calories', async ({ page }) => {
    await page.fill('#food-input', 'chicken breast');
    await page.fill('#gram-input', '200');
    await page.click('#add-btn');
    await page.click('#calculate-btn');
    await expect(page.locator('#results-tbody')).toContainText('330');
  });

  test('TOTAL row appears at the bottom', async ({ page }) => {
    await page.fill('#food-input', 'apple');
    await page.fill('#gram-input', '100');
    await page.click('#add-btn');
    await page.fill('#food-input', 'banana');
    await page.fill('#gram-input', '100');
    await page.click('#add-btn');
    await page.click('#calculate-btn');
    await expect(page.locator('#results-tbody tr.totals-row')).toContainText('TOTAL');
  });

  test('four macro summary cards are shown', async ({ page }) => {
    await page.fill('#food-input', 'salmon');
    await page.fill('#gram-input', '150');
    await page.click('#add-btn');
    await page.click('#calculate-btn');
    await expect(page.locator('.macro-card')).toHaveCount(4);
  });

  test('multi-item total: chicken(200g)+rice(150g)+broccoli(100g) = 532 kcal', async ({ page }) => {
    for (const [n, g] of [['chicken breast','200'],['brown rice','150'],['broccoli','100']]) {
      await page.fill('#food-input', n);
      await page.fill('#gram-input', g);
      await page.click('#add-btn');
    }
    await page.click('#calculate-btn');
    await expect(page.locator('#results-tbody tr.totals-row')).toContainText('532');
  });
});

/* ══════════════════════════════════════════════════════════
   NEW MEAL
══════════════════════════════════════════════════════════ */
test.describe('New Meal button', () => {
  test('clears items, results and meal name', async ({ page }) => {
    await page.fill('#food-input', 'apple');
    await page.fill('#gram-input', '100');
    await page.click('#add-btn');
    await page.click('#calculate-btn');
    await page.fill('#meal-name', 'Test');
    await page.click('#new-btn');
    await expect(page.locator('#item-count')).toHaveText('0 items');
    await expect(page.locator('#meal-list')).toContainText('No items yet');
    await expect(page.locator('#results-panel')).toBeHidden();
    await expect(page.locator('#meal-name')).toHaveValue('');
  });
});

/* ══════════════════════════════════════════════════════════
   SAVE & HISTORY
══════════════════════════════════════════════════════════ */
test.describe('Save and history', () => {
  async function buildAndSaveMeal(page, label) {
    await page.fill('#food-input', 'chicken breast');
    await page.fill('#gram-input', '200');
    await page.click('#add-btn');
    await page.fill('#food-input', 'brown rice');
    await page.fill('#gram-input', '150');
    await page.click('#add-btn');
    if (label) await page.fill('#meal-name', label);
    await page.click('#save-btn');
  }

  test('saving a meal adds it to history', async ({ page }) => {
    await buildAndSaveMeal(page, 'My Lunch');
    await expect(page.locator('#history-list')).toContainText('My Lunch');
  });

  test('history entry shows item count and calories', async ({ page }) => {
    await buildAndSaveMeal(page, 'Dinner');
    await expect(page.locator('#history-list')).toContainText('2 items');
    await expect(page.locator('#history-list')).toContainText('kcal');
  });

  test('save without prior calculate auto-calculates first', async ({ page }) => {
    await page.fill('#food-input', 'apple');
    await page.fill('#gram-input', '100');
    await page.click('#add-btn');
    await page.click('#save-btn'); // no calculate clicked
    await expect(page.locator('#results-panel')).toBeVisible();
    await expect(page.locator('#history-list')).not.toContainText('No saved meals yet');
  });

  test('save with no items shows warning toast', async ({ page }) => {
    await page.click('#save-btn');
    await expect(page.locator('#toast')).toContainText('Add food items before saving');
  });

  test('blank meal name generates an auto-label', async ({ page }) => {
    await page.fill('#food-input', 'apple');
    await page.fill('#gram-input', '100');
    await page.click('#add-btn');
    await page.click('#save-btn');
    await expect(page.locator('#history-list')).toContainText('Meal —');
  });

  test('deleting a meal removes it from history', async ({ page }) => {
    await buildAndSaveMeal(page, 'Delete Me');
    await page.locator('.history-delete').first().click();
    await expect(page.locator('#history-list')).not.toContainText('Delete Me');
  });

  test('loading a saved meal restores its items and results', async ({ page }) => {
    await buildAndSaveMeal(page, 'Load Me');
    await page.click('#new-btn');
    await page.locator('.history-item').filter({ hasText: 'Load Me' }).click();
    await expect(page.locator('#meal-list')).toContainText('chicken breast');
    await expect(page.locator('#meal-list')).toContainText('brown rice');
    await expect(page.locator('#results-panel')).toBeVisible();
  });

  test('loaded meal is highlighted as active in history', async ({ page }) => {
    await buildAndSaveMeal(page, 'Active');
    await page.click('#new-btn');
    await page.locator('.history-item').filter({ hasText: 'Active' }).click();
    await expect(page.locator('.history-item.active')).toHaveCount(1);
  });

  test('re-saving a loaded meal overwrites instead of duplicating', async ({ page }) => {
    await buildAndSaveMeal(page, 'Single Entry');
    await page.fill('#food-input', 'broccoli');
    await page.fill('#gram-input', '100');
    await page.click('#add-btn');
    await page.click('#save-btn');
    await expect(page.locator('.history-item')).toHaveCount(1);
    await expect(page.locator('#history-list')).toContainText('3 items');
  });
});

/* ══════════════════════════════════════════════════════════
   SESSION PERSISTENCE
══════════════════════════════════════════════════════════ */
test.describe('Session persistence across reload', () => {
  test('last meal is auto-restored after page reload', async ({ page }) => {
    await page.fill('#food-input', 'salmon');
    await page.fill('#gram-input', '200');
    await page.click('#add-btn');
    await page.fill('#meal-name', 'Salmon Dinner');
    await page.click('#save-btn');

    await page.reload();
    await page.waitForLoadState('domcontentloaded');

    await expect(page.locator('#meal-list')).toContainText('salmon');
    await expect(page.locator('#meal-name')).toHaveValue('Salmon Dinner');
    await expect(page.locator('#results-panel')).toBeVisible();
  });

  test('history panel shows saved meals after reload', async ({ page }) => {
    await page.fill('#food-input', 'egg');
    await page.fill('#gram-input', '100');
    await page.click('#add-btn');
    await page.fill('#meal-name', 'Quick Snack');
    await page.click('#save-btn');

    await page.reload();
    await page.waitForLoadState('domcontentloaded');
    await expect(page.locator('#history-list')).toContainText('Quick Snack');
  });
});
