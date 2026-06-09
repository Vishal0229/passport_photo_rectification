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

## API endpoints

| Method | Path | Purpose |
|--------|------|---------|
| GET | `/api/countries` | List all supported country specs (returns full `CountrySpec` objects including `requirementsBulletPoints`) |
| POST | `/api/analyze` | Analyze photo compliance |
| POST | `/api/correct` | Return corrected photo (JPEG bytes) |

Both POST endpoints accept `multipart/form-data` with:
- `photo` — image file (JPEG / PNG / WEBP)
- `country` — country code string (e.g. `US`, `India`, `Custom`)
- `customSpec` *(optional)* — JSON string of a `CountrySpec` object, used when `country=Custom`

## Architecture decisions

### Face detection — two layers
1. **Frontend**: `@vladmandic/face-api` TinyFaceDetector (neural network, browser-side). scoreThreshold=0.4, inputSize=416. Runs before upload reaches the server.
2. **Backend**: OpenCV Haar Cascade — two-pass:
   - Pass 1: `haarcascade_frontalface_default.xml`, minNeighbors=4, minSize=30×30, no equalizeHist (equalizeHist caused false negatives on uniform backgrounds)
   - Pass 2 (if count==0): `haarcascade_frontalface_alt2.xml` + 2× upscale + equalizeHist, minNeighbors=2

Only the **multiple-people gate** (`faces > 1`) blocks requests — no "no face" gate. Real users upload their own photos; the analysis itself checks face presence as one of its compliance checks.

### "My Country not on list" (Custom) spec
When `country=Custom`, the frontend sends a `customSpec` JSON field with all `CountrySpec` fields pre-populated (pixel dimensions auto-calculated from mm at 300 DPI). The backend parses it directly and skips the country-spec lookup.

### Cascade XML files
- `haarcascade_frontalface_default.xml` — downloaded at Maven build time via `download-maven-plugin` into `target/generated-resources/`, bundled into the fat JAR.
- `haarcascade_frontalface_alt2.xml` — bundled directly in `src/main/resources/` (the Maven download plugin had a Windows cache-index permission error for a second download execution).

### Image correction
Uses Thumbnailator for scaling (cover-mode: scale to fill both dimensions), then centre-crops onto a `BufferedImage` canvas filled with the spec's background colour. Slight top-bias vertical offset to keep face in frame.

## Key files

### Backend
| File | Purpose |
|------|---------|
| `controller/PhotoController.java` | REST endpoints; face-count gate; custom-spec JSON parsing |
| `service/PhotoAnalysisService.java` | Compliance checks + image correction logic |
| `service/FaceDetectionService.java` | Two-pass Haar Cascade face counting |
| `model/CountrySpec.java` | Spec model (widthMm, heightMm, widthPx, heightPx, dpi, backgroundColor, faceRatioMin/Max, requirementsBulletPoints) |
| `resources/country-specs.json` | Country spec data |
| `resources/haarcascade_frontalface_alt2.xml` | Alt2 cascade (bundled) |

### Frontend
| File | Purpose |
|------|---------|
| `src/App.jsx` | Root state, orchestrates analyze/correct flow |
| `src/components/UploadZone.jsx` | Drag-drop upload; runs frontend face detection; `onNewFileSelected` clears stale state |
| `src/components/CountrySelector.jsx` | Country dropdown including Custom option |
| `src/components/CustomSpecForm.jsx` | Form for entering custom passport spec |
| `src/components/PreUploadRequirementsChecklist.jsx` | Shows country-specific requirements (bullets, description, official link) immediately after country selection, before upload |
| `src/components/ComplianceChecklist.jsx` | Displays check results + disclaimer + official links |
| `src/components/PhotoComparison.jsx` | Before/after slider |
| `src/services/api.js` | Axios calls to backend (supports `customSpec` param) |
| `src/services/faceDetection.js` | face-api.js TinyFaceDetector wrapper |

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

## Official spec links (in ComplianceChecklist.jsx)
- US: travel.state.gov passports/requirements/photos
- UK: gov.uk/photos-for-passports
- India: Passport Seva PDF (ctfassets.net)
- Canada: canada.ca immigration passports/photos
- Australia: passports.gov.au/help/passport-photos
- UAE: icp.gov.ae (link still TBD — users can use "My Country not on list" instead)
- Schengen: schengenvisainfo.com/photo-requirements/
