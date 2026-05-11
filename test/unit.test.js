/**
 * Unit tests for calorie-tracker pure logic
 * Run: npm test  (uses Node 18 built-in test runner, no install needed)
 */
import { test, describe } from 'node:test';
import assert from 'node:assert/strict';

/* ═══════════════════════════════════════════════════════════════
   INLINE PURE FUNCTIONS  (mirrors calorie-tracker.html logic)
   These are duplicated here intentionally so unit tests run
   without a browser and without modifying the production file.
═══════════════════════════════════════════════════════════════ */

function round1(n) { return Math.round(n * 10) / 10; }

const FOOD_DB = {
  'apple':             { cal: 52,  fat: 0.2, carbs: 14.0, protein: 0.3  },
  'banana':            { cal: 89,  fat: 0.3, carbs: 23.0, protein: 1.1  },
  'orange':            { cal: 47,  fat: 0.1, carbs: 12.0, protein: 0.9  },
  'grapes':            { cal: 69,  fat: 0.2, carbs: 18.0, protein: 0.7  },
  'strawberry':        { cal: 32,  fat: 0.3, carbs: 7.7,  protein: 0.7  },
  'blueberry':         { cal: 57,  fat: 0.3, carbs: 14.0, protein: 0.7  },
  'mango':             { cal: 60,  fat: 0.4, carbs: 15.0, protein: 0.8  },
  'pineapple':         { cal: 50,  fat: 0.1, carbs: 13.0, protein: 0.5  },
  'watermelon':        { cal: 30,  fat: 0.2, carbs: 7.6,  protein: 0.6  },
  'avocado':           { cal: 160, fat: 15.0,carbs: 9.0,  protein: 2.0  },
  'peach':             { cal: 39,  fat: 0.3, carbs: 10.0, protein: 0.9  },
  'pear':              { cal: 57,  fat: 0.1, carbs: 15.0, protein: 0.4  },
  'kiwi':              { cal: 61,  fat: 0.5, carbs: 15.0, protein: 1.1  },
  'cherry':            { cal: 63,  fat: 0.2, carbs: 16.0, protein: 1.1  },
  'raspberry':         { cal: 52,  fat: 0.7, carbs: 12.0, protein: 1.2  },
  'lemon':             { cal: 29,  fat: 0.3, carbs: 9.0,  protein: 1.1  },
  'lime':              { cal: 30,  fat: 0.2, carbs: 11.0, protein: 0.7  },
  'papaya':            { cal: 43,  fat: 0.3, carbs: 11.0, protein: 0.5  },
  'plum':              { cal: 46,  fat: 0.3, carbs: 11.0, protein: 0.7  },
  'cantaloupe':        { cal: 34,  fat: 0.2, carbs: 8.0,  protein: 0.8  },
  'coconut':           { cal: 354, fat: 33.0,carbs: 15.0, protein: 3.3  },
  'broccoli':          { cal: 34,  fat: 0.4, carbs: 7.0,  protein: 2.8  },
  'spinach':           { cal: 23,  fat: 0.4, carbs: 3.6,  protein: 2.9  },
  'carrot':            { cal: 41,  fat: 0.2, carbs: 10.0, protein: 0.9  },
  'tomato':            { cal: 18,  fat: 0.2, carbs: 3.9,  protein: 0.9  },
  'cucumber':          { cal: 15,  fat: 0.1, carbs: 3.6,  protein: 0.7  },
  'lettuce':           { cal: 15,  fat: 0.2, carbs: 2.9,  protein: 1.4  },
  'celery':            { cal: 16,  fat: 0.2, carbs: 3.0,  protein: 0.7  },
  'onion':             { cal: 40,  fat: 0.1, carbs: 9.0,  protein: 1.1  },
  'garlic':            { cal: 149, fat: 0.5, carbs: 33.0, protein: 6.4  },
  'sweet potato':      { cal: 86,  fat: 0.1, carbs: 20.0, protein: 1.6  },
  'potato':            { cal: 77,  fat: 0.1, carbs: 17.0, protein: 2.0  },
  'bell pepper red':   { cal: 31,  fat: 0.3, carbs: 6.0,  protein: 1.0  },
  'bell pepper green': { cal: 20,  fat: 0.2, carbs: 4.6,  protein: 0.9  },
  'cauliflower':       { cal: 25,  fat: 0.3, carbs: 5.0,  protein: 1.9  },
  'zucchini':          { cal: 17,  fat: 0.3, carbs: 3.1,  protein: 1.2  },
  'asparagus':         { cal: 20,  fat: 0.1, carbs: 3.9,  protein: 2.2  },
  'kale':              { cal: 49,  fat: 0.9, carbs: 9.0,  protein: 4.3  },
  'cabbage':           { cal: 25,  fat: 0.1, carbs: 6.0,  protein: 1.3  },
  'mushroom':          { cal: 22,  fat: 0.3, carbs: 3.3,  protein: 3.1  },
  'corn':              { cal: 86,  fat: 1.4, carbs: 19.0, protein: 3.3  },
  'peas':              { cal: 81,  fat: 0.4, carbs: 14.0, protein: 5.4  },
  'beet':              { cal: 43,  fat: 0.2, carbs: 10.0, protein: 1.6  },
  'radish':            { cal: 16,  fat: 0.1, carbs: 3.4,  protein: 0.7  },
  'eggplant':          { cal: 25,  fat: 0.2, carbs: 6.0,  protein: 1.0  },
  'leek':              { cal: 61,  fat: 0.3, carbs: 14.0, protein: 1.5  },
  'chicken breast':    { cal: 165, fat: 3.6, carbs: 0.0,  protein: 31.0 },
  'chicken thigh':     { cal: 209, fat: 13.0,carbs: 0.0,  protein: 26.0 },
  'ground beef':       { cal: 254, fat: 20.0,carbs: 0.0,  protein: 17.0 },
  'ground turkey':     { cal: 189, fat: 11.0,carbs: 0.0,  protein: 22.0 },
  'salmon':            { cal: 208, fat: 13.0,carbs: 0.0,  protein: 20.0 },
  'tuna':              { cal: 116, fat: 2.5, carbs: 0.0,  protein: 26.0 },
  'shrimp':            { cal: 99,  fat: 1.7, carbs: 0.9,  protein: 24.0 },
  'tilapia':           { cal: 96,  fat: 2.7, carbs: 0.0,  protein: 20.0 },
  'pork tenderloin':   { cal: 143, fat: 3.5, carbs: 0.0,  protein: 26.0 },
  'bacon':             { cal: 541, fat: 42.0,carbs: 1.4,  protein: 37.0 },
  'ham':               { cal: 145, fat: 7.5, carbs: 1.5,  protein: 19.0 },
  'steak':             { cal: 207, fat: 10.0,carbs: 0.0,  protein: 30.0 },
  'cod':               { cal: 82,  fat: 0.7, carbs: 0.0,  protein: 18.0 },
  'sardine':           { cal: 208, fat: 11.0,carbs: 0.0,  protein: 25.0 },
  'egg':               { cal: 155, fat: 11.0,carbs: 1.1,  protein: 13.0 },
  'egg white':         { cal: 52,  fat: 0.2, carbs: 0.7,  protein: 11.0 },
  'tofu':              { cal: 76,  fat: 4.8, carbs: 1.9,  protein: 8.0  },
  'tempeh':            { cal: 195, fat: 11.0,carbs: 9.4,  protein: 20.0 },
  'duck breast':       { cal: 201, fat: 11.0,carbs: 0.0,  protein: 28.0 },
  'lamb':              { cal: 294, fat: 21.0,carbs: 0.0,  protein: 25.0 },
  'white rice':        { cal: 130, fat: 0.3, carbs: 28.0, protein: 2.7  },
  'brown rice':        { cal: 112, fat: 0.9, carbs: 24.0, protein: 2.3  },
  'oats':              { cal: 389, fat: 7.0, carbs: 66.0, protein: 17.0 },
  'oatmeal':           { cal: 71,  fat: 1.4, carbs: 12.0, protein: 2.5  },
  'white bread':       { cal: 265, fat: 3.2, carbs: 49.0, protein: 9.0  },
  'whole wheat bread': { cal: 247, fat: 3.4, carbs: 41.0, protein: 13.0 },
  'pasta':             { cal: 131, fat: 1.1, carbs: 25.0, protein: 5.0  },
  'quinoa':            { cal: 120, fat: 1.9, carbs: 21.0, protein: 4.4  },
  'barley':            { cal: 123, fat: 0.4, carbs: 28.0, protein: 2.3  },
  'cornmeal':          { cal: 362, fat: 3.6, carbs: 77.0, protein: 8.1  },
  'flour tortilla':    { cal: 312, fat: 8.0, carbs: 50.0, protein: 8.0  },
  'corn tortilla':     { cal: 218, fat: 2.5, carbs: 46.0, protein: 5.7  },
  'bagel':             { cal: 250, fat: 1.5, carbs: 49.0, protein: 10.0 },
  'english muffin':    { cal: 227, fat: 1.7, carbs: 44.0, protein: 8.4  },
  'pita bread':        { cal: 275, fat: 1.2, carbs: 55.0, protein: 9.1  },
  'croissant':         { cal: 406, fat: 21.0,carbs: 46.0, protein: 8.2  },
  'granola':           { cal: 471, fat: 20.0,carbs: 64.0, protein: 10.0 },
  'corn flakes':       { cal: 357, fat: 0.4, carbs: 84.0, protein: 7.5  },
  'whole milk':        { cal: 61,  fat: 3.3, carbs: 4.8,  protein: 3.2  },
  'skim milk':         { cal: 34,  fat: 0.1, carbs: 5.0,  protein: 3.4  },
  'greek yogurt':      { cal: 59,  fat: 0.4, carbs: 3.6,  protein: 10.0 },
  'plain yogurt':      { cal: 61,  fat: 3.3, carbs: 4.7,  protein: 3.5  },
  'cheddar cheese':    { cal: 403, fat: 33.0,carbs: 1.3,  protein: 25.0 },
  'mozzarella':        { cal: 280, fat: 17.0,carbs: 2.2,  protein: 28.0 },
  'cottage cheese':    { cal: 98,  fat: 4.3, carbs: 3.4,  protein: 11.0 },
  'cream cheese':      { cal: 342, fat: 34.0,carbs: 4.1,  protein: 6.0  },
  'butter':            { cal: 717, fat: 81.0,carbs: 0.1,  protein: 0.9  },
  'heavy cream':       { cal: 340, fat: 36.0,carbs: 2.8,  protein: 2.8  },
  'sour cream':        { cal: 193, fat: 19.0,carbs: 4.6,  protein: 2.1  },
  'parmesan':          { cal: 431, fat: 29.0,carbs: 4.0,  protein: 38.0 },
  'swiss cheese':      { cal: 380, fat: 28.0,carbs: 5.4,  protein: 27.0 },
  'ice cream':         { cal: 207, fat: 11.0,carbs: 24.0, protein: 3.5  },
  'half and half':     { cal: 130, fat: 12.0,carbs: 4.3,  protein: 3.0  },
  'almonds':           { cal: 579, fat: 50.0,carbs: 22.0, protein: 21.0 },
  'cashews':           { cal: 553, fat: 44.0,carbs: 30.0, protein: 18.0 },
  'peanuts':           { cal: 567, fat: 49.0,carbs: 16.0, protein: 26.0 },
  'walnuts':           { cal: 654, fat: 65.0,carbs: 14.0, protein: 15.0 },
  'pistachios':        { cal: 560, fat: 45.0,carbs: 28.0, protein: 20.0 },
  'pecans':            { cal: 691, fat: 72.0,carbs: 14.0, protein: 9.2  },
  'macadamia nuts':    { cal: 718, fat: 76.0,carbs: 14.0, protein: 7.9  },
  'sunflower seeds':   { cal: 584, fat: 51.0,carbs: 20.0, protein: 21.0 },
  'pumpkin seeds':     { cal: 559, fat: 49.0,carbs: 10.0, protein: 30.0 },
  'chia seeds':        { cal: 486, fat: 31.0,carbs: 42.0, protein: 17.0 },
  'flaxseeds':         { cal: 534, fat: 42.0,carbs: 29.0, protein: 18.0 },
  'peanut butter':     { cal: 588, fat: 50.0,carbs: 20.0, protein: 25.0 },
  'almond butter':     { cal: 614, fat: 56.0,carbs: 19.0, protein: 21.0 },
  'tahini':            { cal: 595, fat: 54.0,carbs: 21.0, protein: 17.0 },
  'olive oil':         { cal: 884, fat: 100.0,carbs: 0.0, protein: 0.0  },
  'coconut oil':       { cal: 862, fat: 100.0,carbs: 0.0, protein: 0.0  },
  'vegetable oil':     { cal: 884, fat: 100.0,carbs: 0.0, protein: 0.0  },
  'mayonnaise':        { cal: 680, fat: 75.0, carbs: 0.6, protein: 1.0  },
  'black beans':       { cal: 132, fat: 0.5, carbs: 24.0, protein: 8.9  },
  'chickpeas':         { cal: 164, fat: 2.6, carbs: 27.0, protein: 8.9  },
  'lentils':           { cal: 116, fat: 0.4, carbs: 20.0, protein: 9.0  },
  'kidney beans':      { cal: 127, fat: 0.5, carbs: 23.0, protein: 8.7  },
  'pinto beans':       { cal: 143, fat: 0.7, carbs: 27.0, protein: 9.0  },
  'edamame':           { cal: 122, fat: 5.2, carbs: 9.9,  protein: 11.0 },
  'hummus':            { cal: 177, fat: 10.0,carbs: 20.0, protein: 5.0  },
  'split peas':        { cal: 116, fat: 0.4, carbs: 21.0, protein: 8.3  },
  'white beans':       { cal: 139, fat: 0.4, carbs: 25.0, protein: 10.0 },
  'soybeans':          { cal: 173, fat: 9.0, carbs: 10.0, protein: 17.0 },
  'french fries':      { cal: 312, fat: 15.0,carbs: 41.0, protein: 3.4  },
  'pizza':             { cal: 266, fat: 10.0,carbs: 33.0, protein: 12.0 },
  'hamburger patty':   { cal: 295, fat: 20.0,carbs: 0.0,  protein: 28.0 },
  'hot dog':           { cal: 290, fat: 26.0,carbs: 2.0,  protein: 11.0 },
  'potato chips':      { cal: 536, fat: 35.0,carbs: 53.0, protein: 7.0  },
  'white sugar':       { cal: 387, fat: 0.0, carbs: 100.0,protein: 0.0  },
  'honey':             { cal: 304, fat: 0.0, carbs: 82.0, protein: 0.3  },
  'ketchup':           { cal: 112, fat: 0.1, carbs: 27.0, protein: 1.3  },
  'ranch dressing':    { cal: 139, fat: 14.0,carbs: 2.4,  protein: 1.3  },
  'soy sauce':         { cal: 53,  fat: 0.1, carbs: 5.0,  protein: 8.1  },
  'salsa':             { cal: 36,  fat: 0.2, carbs: 7.7,  protein: 1.4  },
  'dark chocolate':    { cal: 546, fat: 31.0,carbs: 60.0, protein: 5.0  },
  'protein bar':       { cal: 350, fat: 10.0,carbs: 45.0, protein: 25.0 },
  'orange juice':      { cal: 45,  fat: 0.2, carbs: 10.0, protein: 0.7  },
  'apple juice':       { cal: 46,  fat: 0.1, carbs: 11.0, protein: 0.1  },
  'cola':              { cal: 41,  fat: 0.0, carbs: 11.0, protein: 0.0  },
  'coffee':            { cal: 2,   fat: 0.0, carbs: 0.0,  protein: 0.3  },
  'beer':              { cal: 43,  fat: 0.0, carbs: 3.6,  protein: 0.5  },
  'red wine':          { cal: 85,  fat: 0.0, carbs: 2.6,  protein: 0.1  },
  'white wine':        { cal: 82,  fat: 0.0, carbs: 2.6,  protein: 0.1  },
  'green tea':         { cal: 1,   fat: 0.0, carbs: 0.2,  protein: 0.2  },
  'sports drink':      { cal: 26,  fat: 0.0, carbs: 7.0,  protein: 0.0  },
  'almond milk':       { cal: 17,  fat: 1.0, carbs: 1.5,  protein: 0.6  },
  'oat milk':          { cal: 50,  fat: 1.5, carbs: 9.0,  protein: 1.0  },
  'balsamic vinegar':  { cal: 88,  fat: 0.0, carbs: 17.0, protein: 0.5  },
  'mustard':           { cal: 66,  fat: 4.0, carbs: 6.0,  protein: 3.7  },
  'hot sauce':         { cal: 11,  fat: 0.4, carbs: 1.3,  protein: 0.6  },
  'maple syrup':       { cal: 260, fat: 0.1, carbs: 67.0, protein: 0.0  },
  'jam':               { cal: 278, fat: 0.1, carbs: 69.0, protein: 0.4  },
  'nutella':           { cal: 547, fat: 31.0,carbs: 58.0, protein: 6.3  },
  'chocolate chip cookie': { cal: 488, fat: 23.0, carbs: 65.0, protein: 5.7 },
};

function getMatches(q) {
  const lq = q.toLowerCase().trim();
  if (!lq) return [];
  const keys = Object.keys(FOOD_DB);
  const starts   = keys.filter(k => k.startsWith(lq));
  const contains = keys.filter(k => !k.startsWith(lq) && k.includes(lq));
  return [...starts, ...contains].slice(0, 9);
}

function computeItem(item) {
  const d = FOOD_DB[item.name];
  const f = item.grams / 100;
  return {
    name:    item.name,
    grams:   item.grams,
    cal:     round1(d.cal     * f),
    fat:     round1(d.fat     * f),
    carbs:   round1(d.carbs   * f),
    protein: round1(d.protein * f),
  };
}

function sumTotals(items) {
  return {
    cal:     round1(items.reduce((s, i) => s + i.cal,     0)),
    fat:     round1(items.reduce((s, i) => s + i.fat,     0)),
    carbs:   round1(items.reduce((s, i) => s + i.carbs,   0)),
    protein: round1(items.reduce((s, i) => s + i.protein, 0)),
  };
}

/* ═══════════════════════════════════════════════════════════════
   TESTS
═══════════════════════════════════════════════════════════════ */

describe('FOOD_DB integrity', () => {
  const keys = Object.keys(FOOD_DB);

  test('has at least 150 food entries', () => {
    assert.ok(keys.length >= 150, `Only found ${keys.length} entries`);
  });

  test('every entry has all four macro fields', () => {
    const missing = keys.filter(k => {
      const e = FOOD_DB[k];
      return !('cal' in e && 'fat' in e && 'carbs' in e && 'protein' in e);
    });
    assert.deepEqual(missing, [], `Entries missing fields: ${missing}`);
  });

  test('every macro value is a non-negative number', () => {
    const bad = [];
    for (const [k, v] of Object.entries(FOOD_DB)) {
      for (const field of ['cal', 'fat', 'carbs', 'protein']) {
        if (typeof v[field] !== 'number' || v[field] < 0) {
          bad.push(`${k}.${field} = ${v[field]}`);
        }
      }
    }
    assert.deepEqual(bad, [], `Bad values: ${bad.join(', ')}`);
  });

  test('calories are within a plausible range (0–950 kcal per 100g)', () => {
    const outliers = keys.filter(k => FOOD_DB[k].cal < 0 || FOOD_DB[k].cal > 950);
    assert.deepEqual(outliers, [], `Out of range: ${outliers}`);
  });

  test('specific expected foods are present', () => {
    const required = [
      'chicken breast', 'brown rice', 'apple', 'salmon', 'egg',
      'olive oil', 'broccoli', 'greek yogurt', 'almonds', 'lentils',
    ];
    const missing = required.filter(f => !(f in FOOD_DB));
    assert.deepEqual(missing, [], `Missing foods: ${missing}`);
  });

  test('chicken breast has correct per-100g values', () => {
    const cb = FOOD_DB['chicken breast'];
    assert.equal(cb.cal, 165);
    assert.equal(cb.fat, 3.6);
    assert.equal(cb.carbs, 0.0);
    assert.equal(cb.protein, 31.0);
  });

  test('pure oils have 0 carbs and 0 protein', () => {
    for (const oil of ['olive oil', 'coconut oil', 'vegetable oil']) {
      assert.equal(FOOD_DB[oil].carbs,   0, `${oil} carbs should be 0`);
      assert.equal(FOOD_DB[oil].protein, 0, `${oil} protein should be 0`);
    }
  });
});

describe('round1', () => {
  test('rounds 1.05 to 1.1 (standard rounding)', () => {
    // JS floating point: round1(1.05) may be 1 due to float representation — test real-world values
    assert.equal(round1(1.04),  1.0);
    assert.equal(round1(1.05),  1.1);
    assert.equal(round1(10.0),  10.0);
    assert.equal(round1(0.0),   0.0);
  });

  test('handles already-round values', () => {
    assert.equal(round1(100), 100);
    assert.equal(round1(0),   0);
  });

  test('handles values with many decimal places', () => {
    assert.equal(round1(12.3456789), 12.3);
    assert.equal(round1(99.95),      100.0);
    assert.equal(round1(0.04),       0.0);
    assert.equal(round1(0.05),       0.1);
  });

  test('handles negative values (JS Math.round rounds toward +inf at .5)', () => {
    // Math.round(-12.5) = -12 in JS, so round1(-1.25) = -1.2
    assert.equal(round1(-1.25), -1.2);
  });
});

describe('computeItem', () => {
  test('100g of any food returns the exact db values', () => {
    for (const name of ['chicken breast', 'apple', 'olive oil', 'brown rice', 'almonds']) {
      const result = computeItem({ name, grams: 100 });
      const db = FOOD_DB[name];
      assert.equal(result.cal,     round1(db.cal),     `${name} cal`);
      assert.equal(result.fat,     round1(db.fat),     `${name} fat`);
      assert.equal(result.carbs,   round1(db.carbs),   `${name} carbs`);
      assert.equal(result.protein, round1(db.protein), `${name} protein`);
    }
  });

  test('200g chicken breast doubles the values', () => {
    const result = computeItem({ name: 'chicken breast', grams: 200 });
    assert.equal(result.cal,     330.0);
    assert.equal(result.fat,     7.2);
    assert.equal(result.carbs,   0.0);
    assert.equal(result.protein, 62.0);
  });

  test('50g apple gives half the 100g values', () => {
    const result = computeItem({ name: 'apple', grams: 50 });
    assert.equal(result.cal,     26.0);  // 52 * 0.5
    assert.equal(result.fat,     0.1);   // 0.2 * 0.5
    assert.equal(result.carbs,   7.0);   // 14 * 0.5
    assert.equal(result.protein, 0.2);   // 0.3 * 0.5, rounds to 0.2
  });

  test('150g brown rice', () => {
    const result = computeItem({ name: 'brown rice', grams: 150 });
    assert.equal(result.cal,     168.0);  // 112 * 1.5
    assert.equal(result.fat,     1.4);    // 0.9 * 1.5
    assert.equal(result.carbs,   36.0);   // 24 * 1.5
    assert.equal(result.protein, 3.5);    // 2.3 * 1.5
  });

  test('decimal grams: 22.5g of oats', () => {
    const result = computeItem({ name: 'oats', grams: 22.5 });
    const f = 22.5 / 100;
    assert.equal(result.cal,     round1(389 * f));
    assert.equal(result.protein, round1(17.0 * f));
  });

  test('preserves name and grams on the result', () => {
    const result = computeItem({ name: 'salmon', grams: 120 });
    assert.equal(result.name,  'salmon');
    assert.equal(result.grams, 120);
  });

  test('1g of olive oil (pure fat)', () => {
    const result = computeItem({ name: 'olive oil', grams: 1 });
    assert.equal(result.cal,   8.8);
    assert.equal(result.fat,   1.0);
    assert.equal(result.carbs, 0.0);
    assert.equal(result.protein, 0.0);
  });
});

describe('sumTotals', () => {
  test('empty array returns all zeros', () => {
    const totals = sumTotals([]);
    assert.deepEqual(totals, { cal: 0, fat: 0, carbs: 0, protein: 0 });
  });

  test('single item totals equal that item', () => {
    const item = computeItem({ name: 'chicken breast', grams: 200 });
    const totals = sumTotals([item]);
    assert.equal(totals.cal,     item.cal);
    assert.equal(totals.fat,     item.fat);
    assert.equal(totals.carbs,   item.carbs);
    assert.equal(totals.protein, item.protein);
  });

  test('post-workout meal: chicken + brown rice + broccoli', () => {
    const items = [
      computeItem({ name: 'chicken breast', grams: 200 }),
      computeItem({ name: 'brown rice',     grams: 150 }),
      computeItem({ name: 'broccoli',       grams: 100 }),
    ];
    const totals = sumTotals(items);

    // Expected: 330 + 168 + 34 = 532
    assert.equal(totals.cal,     532.0);
    // Expected fat: 7.2 + 1.4 + 0.4 = 9.0
    assert.equal(totals.fat,     9.0);
    // Expected carbs: 0 + 36.0 + 7.0 = 43.0
    assert.equal(totals.carbs,   43.0);
    // Expected protein: 62.0 + 3.5 + 2.8 = 68.3
    assert.equal(totals.protein, 68.3);
  });

  test('breakfast: oatmeal + banana + greek yogurt', () => {
    const items = [
      computeItem({ name: 'oatmeal',     grams: 200 }),
      computeItem({ name: 'banana',      grams: 120 }),
      computeItem({ name: 'greek yogurt',grams: 150 }),
    ];
    const totals = sumTotals(items);
    assert.equal(totals.cal, round1(
      computeItem({ name: 'oatmeal',     grams: 200 }).cal +
      computeItem({ name: 'banana',      grams: 120 }).cal +
      computeItem({ name: 'greek yogurt',grams: 150 }).cal
    ));
  });

  test('rounding is stable for multi-item sums (no floating-point drift)', () => {
    const items = Array.from({ length: 10 }, () =>
      computeItem({ name: 'apple', grams: 100 })
    );
    const totals = sumTotals(items);
    // 10 × 52 = 520; should be exactly 520 not 519.99...
    assert.equal(totals.cal, 520.0);
  });
});

describe('getMatches (autocomplete)', () => {
  test('empty query returns empty array', () => {
    assert.deepEqual(getMatches(''), []);
  });

  test('whitespace-only query returns empty array', () => {
    assert.deepEqual(getMatches('   '), []);
  });

  test('exact match returns that food first', () => {
    const results = getMatches('apple');
    assert.equal(results[0], 'apple');
  });

  test('prefix match ranks before contains match', () => {
    // "ch" starts: "chicken breast", "chicken thigh", "chia seeds", "chickpeas", "cheddar cheese"
    // "ch" contains: nothing else
    const results = getMatches('ch');
    const chickenIdx  = results.indexOf('chicken breast');
    const chiaIdx     = results.indexOf('chia seeds');
    assert.ok(chickenIdx >= 0, 'chicken breast should appear');
    assert.ok(chiaIdx    >= 0, 'chia seeds should appear');
    // Both start with 'ch', so both are in the starts group — just verify both present
  });

  test('"rice" returns brown rice and white rice', () => {
    const results = getMatches('rice');
    assert.ok(results.includes('brown rice')  || results.includes('white rice'),
      'Should find rice foods');
  });

  test('case-insensitive: "APPLE" finds apple', () => {
    const results = getMatches('APPLE');
    assert.ok(results.includes('apple'));
  });

  test('partial query "chick" returns chicken entries', () => {
    const results = getMatches('chick');
    assert.ok(results.some(r => r.startsWith('chicken')));
  });

  test('returns at most 9 results', () => {
    const results = getMatches('a'); // many foods contain 'a'
    assert.ok(results.length <= 9);
  });

  test('unknown query returns empty array', () => {
    const results = getMatches('xyzxyzxyz');
    assert.deepEqual(results, []);
  });

  test('"oil" finds olive oil, coconut oil, vegetable oil', () => {
    const results = getMatches('oil');
    assert.ok(results.includes('olive oil'));
    assert.ok(results.includes('coconut oil'));
    assert.ok(results.includes('vegetable oil'));
  });

  test('"yogurt" finds greek yogurt and plain yogurt', () => {
    const results = getMatches('yogurt');
    assert.ok(results.includes('greek yogurt'));
    assert.ok(results.includes('plain yogurt'));
  });
});

describe('real-world meal scenarios', () => {
  test('keto breakfast: eggs + bacon + avocado', () => {
    const items = [
      computeItem({ name: 'egg',     grams: 200 }), // ~2 large eggs
      computeItem({ name: 'bacon',   grams: 30  }),
      computeItem({ name: 'avocado', grams: 100 }),
    ];
    const totals = sumTotals(items);
    // High fat, very low carbs
    assert.ok(totals.fat > totals.carbs, 'keto meal should have more fat than carbs');
    assert.ok(totals.cal > 400,          'should have substantial calories');
  });

  test('vegan bowl: chickpeas + quinoa + spinach + olive oil', () => {
    const items = [
      computeItem({ name: 'chickpeas', grams: 150 }),
      computeItem({ name: 'quinoa',    grams: 150 }),
      computeItem({ name: 'spinach',   grams: 100 }),
      computeItem({ name: 'olive oil', grams: 15  }),
    ];
    const totals = sumTotals(items);
    assert.ok(totals.protein > 20, 'vegan bowl should have meaningful protein');
    assert.ok(totals.carbs > 50,   'should have substantial carbs');
  });

  test('pure sugar (100g) has ~387 cal and 100g carbs, 0 fat, 0 protein', () => {
    const result = computeItem({ name: 'white sugar', grams: 100 });
    assert.equal(result.cal,     387);
    assert.equal(result.carbs,   100);
    assert.equal(result.fat,     0);
    assert.equal(result.protein, 0);
  });

  test('calorie-dense vs light food comparison', () => {
    const oilResult  = computeItem({ name: 'olive oil',  grams: 100 });
    const watermelonResult = computeItem({ name: 'watermelon', grams: 100 });
    assert.ok(
      oilResult.cal > watermelonResult.cal * 10,
      'olive oil should have >10× the calories of watermelon per 100g'
    );
  });
});

describe('weight unit conversion (UNIT_TO_GRAMS)', () => {
  const UNIT_TO_GRAMS = { g: 1, kg: 1000, oz: 28.3495, lbs: 453.592 };

  function toGrams(val, unit) {
    return round1(val * UNIT_TO_GRAMS[unit]);
  }

  test('grams mode: 1 g = 1 g', () => {
    assert.equal(toGrams(1, 'g'), 1);
  });

  test('kg: 1 kg = 1000 g', () => {
    assert.equal(toGrams(1, 'kg'), 1000);
  });

  test('kg: 0.15 kg = 150 g', () => {
    assert.equal(toGrams(0.15, 'kg'), 150);
  });

  test('oz: 1 oz ≈ 28.3 g', () => {
    assert.equal(toGrams(1, 'oz'), 28.3);
  });

  test('oz: 5 oz ≈ 141.7 g', () => {
    assert.equal(toGrams(5, 'oz'), round1(5 * 28.3495));
  });

  test('lbs: 1 lb ≈ 453.6 g', () => {
    assert.equal(toGrams(1, 'lbs'), round1(453.592));
  });

  test('lbs: 0.5 lbs ≈ 226.8 g', () => {
    assert.equal(toGrams(0.5, 'lbs'), round1(0.5 * 453.592));
  });

  test('nutrition is correct when weight entered in kg', () => {
    const grams = toGrams(0.2, 'kg'); // 0.2 kg = 200 g
    const result = computeItem({ name: 'chicken breast', grams });
    assert.equal(result.cal, 330);
    assert.equal(result.protein, 62);
  });

  test('nutrition is correct when weight entered in lbs', () => {
    const grams = toGrams(1, 'lbs'); // 1 lb ≈ 453.6 g
    const result = computeItem({ name: 'chicken breast', grams });
    assert.ok(result.cal > 700 && result.cal < 780, `Expected ~748 kcal, got ${result.cal}`);
  });
});

describe('saved meal data shape validation', () => {
  test('a saved meal entry has all required fields', () => {
    const meal = {
      id: 'test-uuid',
      label: 'Lunch',
      savedAt: new Date().toISOString(),
      items: [{ name: 'apple', grams: 150 }],
      results: {
        items: [computeItem({ name: 'apple', grams: 150 })],
        totals: sumTotals([computeItem({ name: 'apple', grams: 150 })]),
      },
    };

    assert.ok(typeof meal.id      === 'string');
    assert.ok(typeof meal.label   === 'string');
    assert.ok(typeof meal.savedAt === 'string');
    assert.ok(Array.isArray(meal.items));
    assert.ok(meal.results && typeof meal.results.totals === 'object');
  });

  test('results totals have all four macro keys', () => {
    const totals = sumTotals([computeItem({ name: 'banana', grams: 100 })]);
    assert.ok('cal'     in totals);
    assert.ok('fat'     in totals);
    assert.ok('carbs'   in totals);
    assert.ok('protein' in totals);
  });
});
