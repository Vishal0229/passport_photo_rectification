# Contributing

## How to Add a New Country Specification

### 1. Add the spec to the JSON file

Edit `passport-photo-backend/src/main/resources/country-specs.json` and add a new entry:

```json
"SG": {
  "name": "Singapore",
  "widthMm": 35,
  "heightMm": 45,
  "widthPx": 413,
  "heightPx": 531,
  "dpi": 300,
  "backgroundColor": "WHITE",
  "backgroundColorHex": "#FFFFFF",
  "faceRatioMin": 0.60,
  "faceRatioMax": 0.80,
  "description": "35×45 mm photo with plain white background"
}
```

Field rules:
- Use the ISO 3166-1 alpha-2 code where possible (e.g. `SG`, `JP`, `BR`). For regional standards (Schengen), use a short descriptive key.
- `widthPx` and `heightPx` must equal `round(widthMm * dpi / 25.4)` and `round(heightMm * dpi / 25.4)` respectively. Always use `dpi: 300`.
- `backgroundColor` must be exactly `"WHITE"` or `"LIGHT_GREY"`.
- `backgroundColorHex` must be a valid 6-digit hex colour starting with `#`.
- `faceRatioMin` / `faceRatioMax` are fractions (0–1) of the photo height. Verify against the official government spec.

### 2. Add the country to the frontend dropdown

Edit `passport-photo-frontend/src/components/CountrySelector.jsx` and add an entry to the `COUNTRIES` array:

```js
{ code: 'SG', name: 'Singapore', flag: '🇸🇬', spec: '35×45 mm · white bg' },
```

Keep the list ordered roughly by usage frequency, with `Custom / Other` always last.

### 3. Add an official requirements link (optional but recommended)

Edit `passport-photo-frontend/src/components/ComplianceChecklist.jsx` and add an entry to the `officialLinks` object at the bottom of the file:

```js
SG: 'https://www.ica.gov.sg/documents/ic/passport-photo',
```

The link should point directly to the photo requirements page, not the general passport page.

### 4. Verify

1. Restart the backend (`mvn spring-boot:run`).
2. Confirm the new code appears: `curl http://localhost:8080/api/countries | jq '.SG'`
3. Open the frontend and check the new country appears in the dropdown.
4. Upload a test photo and verify the analysis results look reasonable.

---

## Running Backend Tests

```powershell
cd passport-photo-backend
mvn test
```

Test output appears in the console. The Surefire report is written to `target/surefire-reports/`.

To run a single test class:

```powershell
mvn test -Dtest=PhotoAnalysisServiceTest
```

To run a single test method:

```powershell
mvn test -Dtest="PhotoControllerTest#analyzePhoto_multipleFaces_returns400WithErrorType"
```

The test suite includes:
- `PhotoControllerTest` — MockMvc slice tests covering all REST endpoints, error paths, and custom-spec parsing.
- `PhotoAnalysisServiceTest` — Unit tests for every compliance check rule and the image correction logic using synthetic `BufferedImage` inputs.

---

## Running Frontend Tests

```powershell
cd passport-photo-frontend
npm test
```

Vitest runs in watch mode. Press `q` to quit.

For a single CI run (no watch):

```powershell
npm test -- --run
```

To run a specific test file:

```powershell
npm test -- src/components/CountrySelector.test.jsx
```

Test coverage report:

```powershell
npm test -- --coverage
```

The test suite covers:
- `CountrySelector.test.jsx` — Renders all countries; selection triggers onChange.
- `ComplianceChecklist.test.jsx` — Renders pass/fail checks; shows spec summary; shows disclaimer link.
- `CustomSpecForm.test.jsx` — mm-to-px conversion; background colour sync; pixel preview.
- `api.test.js` — Axios calls for `analyzePhoto` and `correctPhoto`; custom-spec JSON serialisation.

---

## Code Style

### Java (backend)

- Java 17. No preview features.
- Follow the existing patterns in the `service` and `controller` packages.
- No Lombok. Plain getters/setters only.
- `@Autowired` on fields (constructor injection is preferred for new code but field injection is used throughout the existing codebase — stay consistent within a class).
- All public methods must have JavaDoc.
- Do not add comments that just restate what the code does. Only comment non-obvious behaviour.

### JavaScript / JSX (frontend)

- React 18 functional components with hooks. No class components.
- Tailwind CSS utility classes only — no custom CSS files.
- `export default` at the bottom of each component file.
- Prop types are not enforced with PropTypes (the codebase does not use it) — keep type information in JSDoc instead.
- All exported functions and components must have JSDoc.

---

## How to Run the Full Agent Pipeline

The project was built using a multi-agent Claude Code pipeline. The agents are invoked via Claude Code skills defined in `.claude/settings.json`. To run the full pipeline from scratch (coder → tester → reviewer → documentation):

1. Open Claude Code in the project root.
2. Run each skill in order:
   - `/run-backend` — starts the Spring Boot backend
   - `/run-frontend` — starts the Vite frontend
   - `/verify` — runs the app and confirms features work end-to-end
   - `/code-review` — reviews the current diff for correctness bugs
   - `/security-review` — checks pending changes for security issues

Individual agents can also be invoked ad hoc. See [agents/README.md](agents/README.md) for details on each agent's role and output format.
