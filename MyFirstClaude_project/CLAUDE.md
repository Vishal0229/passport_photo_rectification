# Passport Photo Corrector

Full-stack PWA that checks and auto-corrects passport photos against country-specific standards.

## Project layout

```
passport-photo-backend/    Spring Boot 3.2.5 · Java 17 · Maven
passport-photo-frontend/   React 18 · Vite 5 · Tailwind CSS v3
```

## Running the app

**Backend** (port 8080):
```powershell
cd passport-photo-backend
mvn spring-boot:run
```
Ready when: `GET http://localhost:8080/api/countries` returns 200.

**Frontend** (port 3000):
```powershell
cd passport-photo-frontend
npm run dev
```

Use the skills for a one-step launch that handles port conflicts automatically:
- `/run-backend`
- `/run-frontend`

### Common gotcha — port 8080 already in use
Previous Spring Boot windows linger in the background. Kill before restarting:
```powershell
$proc = (netstat -ano | Select-String ":8080 " | Select-String "LISTENING" | ForEach-Object { ($_ -split '\s+')[-1] } | Select-Object -First 1)
if ($proc) { Stop-Process -Id $proc -Force }
```

## Deployment (Render)

The app deploys to Render via `render.yaml` at the repo root (Render Blueprint).

| Service | Type | Config |
|---------|------|--------|
| `passport-photo-backend` | Docker Web Service | `passport-photo-backend/Dockerfile` |
| `passport-photo-frontend` | Static Site | `passport-photo-frontend/` → `dist/` |

### First-deploy steps
1. Push the repo to GitHub and connect it to Render (New → Blueprint).
2. After both services are live, copy the **backend** public URL from the Render dashboard.
3. On the **frontend** service → Environment → set `VITE_API_URL` to the backend URL (e.g. `https://passport-photo-backend.onrender.com`) → Save → Manual Deploy.
4. Copy the **frontend** public URL.
5. On the **backend** service → Environment → set `CORS_ALLOWED_ORIGINS` to the frontend URL (e.g. `https://passport-photo-frontend.onrender.com`) → Save → Manual Deploy.

### Free-tier notes
- Backend spins down after 15 min idle — first request after idle takes ~30-60 s (Spring Boot + OpenCV cold start).
- Digest-pinned base images: update pins periodically with `docker pull <image>` and `docker inspect --format='{{index .RepoDigests 0}}'` to stay current on security patches.

## API endpoints

| Method | Path | Purpose |
|--------|------|---------|
| GET | `/api/countries` | List all supported country specs (returns full `CountrySpec` objects including `requirementsBulletPoints`) |
| POST | `/api/analyze` | Analyze photo compliance |
| POST | `/api/correct` | Return corrected photo (JPEG bytes) |
| POST | `/api/sheet` | Generate print-ready 4×6" JPEG photo sheet (multiple copies tiled at 300 DPI) |
| POST | `/api/pdf` | Generate print-ready A4 PDF photo sheet |

Both POST endpoints accept `multipart/form-data` with:
- `photo` — image file (JPEG / PNG / WEBP)
- `country` — country code string (e.g. `US`, `India`, `Custom`)
- `customSpec` *(optional)* — JSON string of a `CountrySpec` object, used when `country=Custom`

## Architecture decisions

### Face detection — two layers
1. **Frontend**: `@vladmandic/face-api` TinyFaceDetector (neural network, browser-side). scoreThreshold=0.4, inputSize=416. Runs before upload reaches the server.
2. **Backend**: OpenCV Haar Cascade — two-pass (shared by both `countFaces` and `detectPrimaryFace`):
   - Pass 1: `haarcascade_frontalface_default.xml`, minNeighbors=4, minSize=30×30, no equalizeHist (equalizeHist caused false negatives on uniform backgrounds)
   - Pass 2 (if count==0 / no face found): `haarcascade_frontalface_alt2.xml` + 2× upscale + equalizeHist, minNeighbors=2

   `countFaces` — returns total number of detected faces (used by the multiple-people gate).
   `detectPrimaryFace` — returns `int[]{x, y, w, h}` of the largest face in original pixel coordinates. Pass 2 divides coordinates by 2 before returning to undo the upscale.

Only the **multiple-people gate** (`faces > 1`) blocks requests — no "no face" gate. Real users upload their own photos; the analysis itself checks face presence as one of its compliance checks.

### "My Country not on list" (Custom) spec
When `country=Custom`, the frontend sends a `customSpec` JSON field with all `CountrySpec` fields pre-populated (pixel dimensions auto-calculated from mm at 300 DPI). The backend parses it directly and skips the country-spec lookup.

### Cascade XML files
- `haarcascade_frontalface_default.xml` — downloaded at Maven build time via `download-maven-plugin` into `target/generated-resources/`, bundled into the fat JAR.
- `haarcascade_frontalface_alt2.xml` — bundled directly in `src/main/resources/` (the Maven download plugin had a Windows cache-index permission error for a second download execution).

### Logging architecture

**Backend — `LoggingAspect`** (`aspect/LoggingAspect.java`)
Spring AOP `@Around` aspect applied to all methods in `com.passport.photo.*` (excluding the aspect class itself).
- Entry: `>> ClassName.method(Type1, Type2)` at DEBUG — argument *types* only, never values.
- Exit: `<< ClassName.method → ReturnType` at DEBUG.
- Exception: `!! ClassName.method threw ExType: message` at ERROR — exception messages are sanitised (newlines stripped) before logging, and nothing internal is forwarded to clients.
- Requires the `spring-boot-starter-aop` dependency in `pom.xml`.

**Backend — `GlobalExceptionHandler`** (`controller/GlobalExceptionHandler.java`)
`@RestControllerAdvice` catch-all that maps exceptions to HTTP responses:
- `IllegalArgumentException` → 400 Bad Request
- Spring MVC exceptions (e.g. `MethodArgumentNotValidException`) → appropriate 4xx
- All others → 500 Internal Server Error
Returns generic, safe messages to clients; sanitised details are logged server-side only.

**Frontend — `src/utils/logger.js`**
Thin logging utility mirroring the backend pattern:
- `logEntry(methodName, argTypes)` — logs method entry at the console DEBUG level.
- `logExit(methodName, returnType)` — logs method exit.
- `logError(source, error)` — logs errors with source context.
- `initGlobalErrorLogging()` — registers `window.onerror` and `window.onunhandledrejection` handlers (idempotent; safe to call multiple times).

### Image correction
Two code paths depending on whether a face is detected:

- **Face-aware crop (primary path)**: When `detectPrimaryFace` finds a face, the crop window is sized so the face occupies `faceRatioMax` of the output height, preserving the target aspect ratio. ~4 mm of head-room is added above the top of the detected face. The window is clamped to source bounds before cropping.
- **Fallback path**: When no face is detected, or the face-aware crop window would exceed source bounds, falls back to cover-mode: Thumbnailator scales to fill both dimensions, then a centre-crop with a slight top-bias vertical offset is placed onto a `BufferedImage` canvas filled with the spec's background colour.

## Key files

### Backend
| File | Purpose |
|------|---------|
| `controller/PhotoController.java` | REST endpoints; face-count gate; custom-spec JSON parsing |
| `service/PhotoAnalysisService.java` | Compliance checks (including face-ratio check via `detectPrimaryFace`) + face-aware image correction logic |
| `service/FaceDetectionService.java` | Two-pass Haar Cascade: `countFaces` (multiple-people gate) and `detectPrimaryFace` (returns largest face bounding box) |
| `model/CountrySpec.java` | Spec model (widthMm, heightMm, widthPx, heightPx, dpi, backgroundColor, faceRatioMin/Max, requirementsBulletPoints) |
| `resources/country-specs.json` | Country spec data |
| `resources/haarcascade_frontalface_alt2.xml` | Alt2 cascade (bundled) |
| `aspect/LoggingAspect.java` | AOP `@Around` aspect — logs entry/exit/exception for all methods in `com.passport.photo.*`; argument types only, never values |
| `controller/GlobalExceptionHandler.java` | `@RestControllerAdvice` catch-all — maps `IllegalArgumentException` → 400, Spring MVC exceptions → 4xx, all others → 500; returns safe generic messages to clients |

### Frontend
| File | Purpose |
|------|---------|
| `src/App.jsx` | Root state, orchestrates analyze/correct flow; countrySpecsLoading state shows spinner/fallback while /api/countries fetch is in flight or failed |
| `src/components/UploadZone.jsx` | Drag-drop upload; runs frontend face detection; `onNewFileSelected` clears stale state |
| `src/components/CountrySelector.jsx` | Country dropdown including Custom option |
| `src/components/CustomSpecForm.jsx` | Form for entering custom passport spec |
| `src/components/PreUploadRequirementsChecklist.jsx` | Shows country-specific requirements (bullets, description, official link) immediately after country selection, before upload |
| `src/components/ComplianceChecklist.jsx` | Displays check results + disclaimer + official links |
| `src/components/PhotoComparison.jsx` | Before/after slider; post-correction banner (amber warning listing checks that still fail after correction — face size, lighting, etc.; green "all requirements met" when all pass; Dimensions/Aspect Ratio/Background are always fixed by the tool and never listed as retake items) |
| `src/services/api.js` | Axios calls to backend (supports `customSpec` param) |
| `src/services/faceDetection.js` | face-api.js TinyFaceDetector wrapper |
| `src/utils/logger.js` | Logging utility: `logEntry`, `logExit`, `logError`; `initGlobalErrorLogging()` registers global `window.onerror` / `unhandledrejection` handlers |

## Supported countries (built-in specs)

15 real countries + "My Country not on list" option = 16 dropdown entries.

| Code | Name | Size |
|------|------|------|
| US | United States | 600×600 px (2×2 in) |
| UK | United Kingdom | 413×531 px (35×45 mm) |
| India | India | 602×602 px (51×51 mm) |
| Canada | Canada | 591×827 px (50×70 mm) |
| Australia | Australia | 413×531 px (35×45 mm) |
| UAE | United Arab Emirates | 472×709 px (40×60 mm) |
| Schengen | Schengen / EU | 413×531 px (35×45 mm) |
| Germany | Germany | 413×531 px (35×45 mm) |
| France | France | 413×531 px (35×45 mm) |
| Japan | Japan | 531×413 px (45×35 mm) |
| China | China | 390×567 px (33×48 mm) |
| Brazil | Brazil | 591×827 px (50×70 mm) |
| NewZealand | New Zealand | 413×531 px (35×45 mm) |
| Singapore | Singapore | 413×531 px (35×45 mm) |
| Mexico | Mexico | 413×531 px (35×45 mm) |
| Custom | My Country not on list | User-entered |

## Security

Open findings are tracked in `securityIssues.md` at the project root (1 HIGH, 2 MEDIUM, 2 LOW, 2 INFO). Review that file before deploying to production; none of the findings are auto-remediated by the application code.

## Official spec links (in ComplianceChecklist.jsx)
- US: travel.state.gov passports/requirements/photos
- UK: gov.uk/photos-for-passports
- India: Passport Seva PDF (ctfassets.net)
- Canada: canada.ca immigration passports/photos
- Australia: passports.gov.au/help/passport-photos
- UAE: icp.gov.ae (link still TBD — users can use "My Country not on list" instead)
- Schengen: schengenvisainfo.com/photo-requirements/
