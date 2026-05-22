# Passport Photo Corrector

A Progressive Web App (PWA) that checks your passport photo against official government standards and automatically corrects dimensions, cropping, and background colour. Upload a photo, pick your country, and download a print-ready JPEG in seconds.

---

## Supported Countries

| Country | Code | Photo Size | Print Size | Background |
|---------|------|-----------|-----------|------------|
| United States | `US` | 600 × 600 px | 51 × 51 mm (2 × 2 in) | White |
| United Kingdom | `UK` | 413 × 531 px | 35 × 45 mm | Light grey |
| India | `India` | 600 × 600 px | 51 × 51 mm | White |
| Canada | `Canada` | 591 × 827 px | 50 × 70 mm | White |
| Australia | `Australia` | 413 × 531 px | 35 × 45 mm | White |
| United Arab Emirates | `UAE` | 472 × 709 px | 40 × 60 mm | White |
| Schengen / EU | `Schengen` | 413 × 531 px | 35 × 45 mm | Light grey |
| Custom / Other | `Custom` | User-entered | User-entered | User-entered |

---

## Installation and Setup (Windows)

### Prerequisites

| Tool | Required version | Download |
|------|-----------------|---------|
| Java JDK | 17 or later | [adoptium.net](https://adoptium.net) |
| Apache Maven | 3.8 or later | [maven.apache.org](https://maven.apache.org) |
| Node.js | 18 or later | [nodejs.org](https://nodejs.org) |

### 1. Clone the repository

```powershell
git clone <repo-url>
cd MyFirstClaude_project
```

### 2. Start the backend (Spring Boot — port 8080)

```powershell
cd passport-photo-backend
mvn spring-boot:run
```

The first run downloads the OpenCV Haar Cascade XML file automatically. Wait until you see:

```
Started PassportPhotoApplication in X.XXX seconds
```

Confirm it is running:

```powershell
Invoke-WebRequest http://localhost:8080/api/countries
```

Expected: HTTP 200 with a JSON object listing all countries.

> **Port conflict?** If port 8080 is already in use, kill the old process first:
> ```powershell
> $proc = (netstat -ano | Select-String ":8080 " | Select-String "LISTENING" | ForEach-Object { ($_ -split '\s+')[-1] } | Select-Object -First 1)
> if ($proc) { Stop-Process -Id $proc -Force }
> ```

### 3. Install frontend dependencies (first run only)

```powershell
cd ..\passport-photo-frontend
npm install
```

### 4. Start the frontend (Vite dev server — port 3000)

```powershell
npm run dev
```

Open [http://localhost:3000](http://localhost:3000) in your browser.

---

## How to Use the App

1. **Upload a photo** — drag and drop a JPEG, PNG, or WEBP file onto the upload zone, or click the zone to browse. Files must be under 15 MB and show exactly one face. The app runs face detection in the browser and will reject photos with multiple people.

2. **Select your country** — choose from the dropdown. A summary of that country's requirements appears below the selector.

3. **Click "Analyze Photo"** — the backend checks your photo against five compliance rules:
   - Image dimensions (minimum pixel size)
   - Aspect ratio (within ±15% of the target ratio)
   - Background colour (corners must be predominantly light)
   - Resolution / quality (total pixel count vs. required)
   - Face presence (heuristic: image centre must have visible content)

4. **Review the checklist** — each rule shows pass (green) or fail (red) with expected vs. actual values. Even if all checks pass, clicking "Correct Photo" is still recommended to get the exact target dimensions.

5. **Click "Correct Photo"** — the backend scales and centre-crops your photo to the exact pixel dimensions required. A before/after comparison appears.

6. **Download** — click "Download Corrected Photo" to save the print-ready JPEG.

7. **Start over** — click "Start over with a new photo" to reset and upload another image.

---

## Custom Country Specifications

If your country is not in the built-in list, select **Custom / Other** from the dropdown.

1. In the form that appears, enter a **Country / Region** name (e.g. "Saudi Arabia").
2. Enter the **Width (mm)** and **Height (mm)** from your country's official passport photo requirements.
3. Select the required **Background** colour (White or Light Grey).
4. Optionally set the **Face min %** and **Face max %** — the percentage of the photo height that the face should occupy. If unsure, leave the defaults (50% – 75%).
5. The pixel dimensions are calculated automatically at 300 DPI and shown as a preview.
6. Click **Analyze Photo** and then **Correct Photo** as usual.

---

## Installing as a PWA on Android

The app is a full Progressive Web App and can be installed on your Android home screen for quick offline-capable access.

1. Open the app in **Google Chrome** on your Android device (navigate to the server's address or `http://localhost:3000` if on the same network).
2. Tap the **three-dot menu** (⋮) in the top-right corner of Chrome.
3. Tap **"Add to Home screen"**.
4. Confirm the name ("Passport Photo Corrector") and tap **"Add"**.
5. The app icon appears on your home screen. Tap it to launch in standalone mode (no browser chrome).

> Note: The install banner also appears automatically at the top of the app when Chrome detects the PWA criteria are met.

---

## Environment Variables

No environment variables are required for local development. The frontend is hardcoded to connect to `http://localhost:8080`.

| Variable | Default | Purpose |
|----------|---------|---------|
| *(none required)* | — | — |

For production deployment, update the `BASE_URL` constant in `passport-photo-frontend/src/services/api.js` to point to your production backend URL, and update the `allowedOrigins` in `passport-photo-backend/src/main/java/com/passport/photo/config/CorsConfig.java`.

---

## Known Limitations

- **Face presence check is heuristic.** The compliance check labelled "Face Presence (estimated)" uses pixel colour variance in the upper-centre region, not a true face recogniser. It can return false negatives on very uniform photos and false positives on patterned backgrounds.

- **Background check samples corners only.** The background compliance check samples the four corners of the image. A non-white background that does not extend to the corners will be missed.

- **Image correction may crop out faces near the edge.** The corrector uses cover-mode scaling followed by a centre crop with a slight top-bias offset. Faces positioned very low or very high in the original photo may be partially cropped.

- **CORS is locked to `localhost:3000`.** The backend accepts requests only from `http://localhost:3000`. A production deployment requires changing `CorsConfig.java`.

- **UAE official link is unconfirmed.** The link to the UAE passport photo requirements page (icp.gov.ae) has not been verified against the live site. Use Custom spec as a fallback.

- **No actual DPI metadata is read.** The "Resolution / Quality" check compares total pixel count against the required pixel area. It does not read or validate EXIF DPI metadata embedded in the file.

- **Maximum file size is 15 MB.** Larger files are rejected by the frontend before upload.

- **Supported formats: JPEG, PNG, WEBP only.** TIFF, BMP, HEIC, and other formats are not supported.

---

## How to Add a New Country Specification

1. Open `passport-photo-backend/src/main/resources/country-specs.json`.
2. Add a new entry using the country code as the key:

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

3. Add the country to the `COUNTRIES` array in `passport-photo-frontend/src/components/CountrySelector.jsx`:

```js
{ code: 'SG', name: 'Singapore', flag: '🇸🇬', spec: '35×45 mm · white bg' },
```

4. Optionally add an official requirements link in `passport-photo-frontend/src/components/ComplianceChecklist.jsx` in the `officialLinks` object:

```js
SG: 'https://www.ica.gov.sg/documents/ic/passport-photo',
```

5. Restart the backend. The new country will appear in `/api/countries` and in the frontend dropdown.

See [CONTRIBUTING.md](CONTRIBUTING.md) for full contribution guidelines.
