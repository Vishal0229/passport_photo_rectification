# API Reference

Base URL: `http://localhost:8080/api`

All POST endpoints accept `multipart/form-data`. Error responses always return JSON with a `message` field.

---

## GET /api/countries

Returns all built-in country specifications.

### Request

No parameters required.

```bash
curl http://localhost:8080/api/countries
```

### Response — 200 OK

```json
{
  "US": {
    "name": "United States",
    "widthMm": 51,
    "heightMm": 51,
    "widthPx": 600,
    "heightPx": 600,
    "dpi": 300,
    "backgroundColor": "WHITE",
    "backgroundColorHex": "#FFFFFF",
    "faceRatioMin": 0.50,
    "faceRatioMax": 0.69,
    "description": "2×2 inch (51×51 mm) square photo with white or off-white background"
  },
  "UK": { ... },
  "India": { ... },
  "Canada": { ... },
  "Australia": { ... },
  "UAE": { ... },
  "Schengen": { ... }
}
```

### CountrySpec fields

| Field | Type | Description |
|-------|------|-------------|
| `name` | string | Display name of the country |
| `widthMm` | integer | Required photo width in millimetres |
| `heightMm` | integer | Required photo height in millimetres |
| `widthPx` | integer | Required photo width in pixels at `dpi` |
| `heightPx` | integer | Required photo height in pixels at `dpi` |
| `dpi` | integer | Print resolution (always 300) |
| `backgroundColor` | string | `WHITE` or `LIGHT_GREY` |
| `backgroundColorHex` | string | Hex colour code for the background |
| `faceRatioMin` | number | Minimum fraction of photo height the face should occupy |
| `faceRatioMax` | number | Maximum fraction of photo height the face should occupy |
| `description` | string | Human-readable spec summary |

### Error codes

| Status | Condition |
|--------|-----------|
| 200 | Always succeeds if the backend is running |

---

## POST /api/analyze

Analyzes a photo against a country's passport photo requirements. Returns a structured compliance report.

### Request

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `photo` | file | Yes | Image file (JPEG, PNG, or WEBP). Maximum 15 MB enforced by the frontend; the backend has no hard size limit. |
| `country` | string | Yes | Country code (e.g. `US`, `India`, `Custom`) |
| `customSpec` | string | No | JSON-serialised `CountrySpec` object. Required when `country=Custom`. |

```bash
# Analyze against a built-in spec
curl -X POST http://localhost:8080/api/analyze \
  -F "photo=@/path/to/photo.jpg" \
  -F "country=US"

# Analyze against a custom spec
curl -X POST http://localhost:8080/api/analyze \
  -F "photo=@/path/to/photo.jpg" \
  -F "country=Custom" \
  -F 'customSpec={"name":"Singapore","widthMm":35,"heightMm":45,"widthPx":413,"heightPx":531,"dpi":300,"backgroundColor":"WHITE","backgroundColorHex":"#FFFFFF","faceRatioMin":0.60,"faceRatioMax":0.80,"description":""}'
```

### Response — 200 OK

```json
{
  "country": "United States",
  "spec": {
    "name": "United States",
    "widthMm": 51,
    "heightMm": 51,
    "widthPx": 600,
    "heightPx": 600,
    "dpi": 300,
    "backgroundColor": "WHITE",
    "backgroundColorHex": "#FFFFFF",
    "faceRatioMin": 0.50,
    "faceRatioMax": 0.69,
    "description": "2×2 inch (51×51 mm) square photo with white or off-white background"
  },
  "checks": [
    {
      "rule": "Image Dimensions",
      "passed": true,
      "message": "Image meets the minimum pixel dimensions",
      "expected": "≥ 600×600 px",
      "actual": "1200×1200 px"
    },
    {
      "rule": "Aspect Ratio",
      "passed": true,
      "message": "Aspect ratio is within the acceptable range",
      "expected": "1.00 : 1  (±15%)",
      "actual": "1.00 : 1"
    },
    {
      "rule": "Background Color",
      "passed": false,
      "message": "Background may be too dark or coloured — use a plain white/light background",
      "expected": "#FFFFFF (plain light)",
      "actual": "42% light pixels sampled at corners"
    },
    {
      "rule": "Resolution / Quality",
      "passed": true,
      "message": "Resolution is sufficient for a quality print at 300 DPI",
      "expected": "300 DPI  (600×600 px)",
      "actual": "1200×1200 px"
    },
    {
      "rule": "Face Presence (estimated)",
      "passed": true,
      "message": "Image centre has visible content — face appears to be present",
      "expected": "Centred face in upper-centre region",
      "actual": "Content detected"
    }
  ],
  "allPassed": false,
  "passedCount": 4,
  "totalCount": 5
}
```

### AnalysisResult fields

| Field | Type | Description |
|-------|------|-------------|
| `country` | string | Country name (or "Custom" for custom specs) |
| `spec` | object | The `CountrySpec` used for analysis |
| `checks` | array | List of `ComplianceCheck` objects (see below) |
| `allPassed` | boolean | `true` only when every check passes |
| `passedCount` | integer | Number of checks that passed |
| `totalCount` | integer | Total number of checks run (always 5) |

### ComplianceCheck fields

| Field | Type | Description |
|-------|------|-------------|
| `rule` | string | Name of the rule being checked |
| `passed` | boolean | Whether this rule passed |
| `message` | string | Human-readable explanation |
| `expected` | string | What the spec requires |
| `actual` | string | What was found in the uploaded photo |

### Compliance rules

| Rule | What is checked |
|------|----------------|
| Image Dimensions | `width >= widthPx` AND `height >= heightPx` |
| Aspect Ratio | `abs(actual_ratio - target_ratio) <= target_ratio * 0.15` |
| Background Color | > 75% of corner pixels have R, G, B > 200 |
| Resolution / Quality | `width * height >= widthPx * heightPx` |
| Face Presence (estimated) | Pixel colour variance > 200 in upper-centre region |

### Error codes

| Status | Body | Condition |
|--------|------|-----------|
| 400 | `{"message": "Unsupported file type..."}` | File is not JPEG, PNG, or WEBP |
| 400 | `{"message": "N people detected...", "errorType": "MULTIPLE_FACES"}` | More than one face detected in the photo |
| 400 | `{"message": "Unknown country code: ZZ"}` | `country` is not a recognised code and no `customSpec` was provided |
| 400 | `{"message": "Cannot read image..."}` | File bytes cannot be decoded as an image |
| 500 | `{"message": "Failed to process image: ..."}` | Unexpected server error (e.g. malformed `customSpec` JSON, disk I/O error) |

---

## POST /api/correct

Corrects a photo to exactly match a country's required pixel dimensions. Returns the corrected photo as a JPEG binary.

The correction algorithm:
1. Scale the source image to "cover" mode — both target dimensions are fully filled while maintaining the original aspect ratio.
2. Centre-crop horizontally.
3. Apply a slight top-bias vertical crop so the face stays in frame.
4. Fill any remaining canvas area with the spec's background colour.

### Request

Same fields as `/api/analyze`.

```bash
# Correct against a built-in spec
curl -X POST http://localhost:8080/api/correct \
  -F "photo=@/path/to/photo.jpg" \
  -F "country=UK" \
  --output corrected_uk.jpg

# Correct against a custom spec
curl -X POST http://localhost:8080/api/correct \
  -F "photo=@/path/to/photo.jpg" \
  -F "country=Custom" \
  -F 'customSpec={"name":"Singapore","widthMm":35,"heightMm":45,"widthPx":413,"heightPx":531,"dpi":300,"backgroundColor":"WHITE","backgroundColorHex":"#FFFFFF","faceRatioMin":0.60,"faceRatioMax":0.80,"description":""}' \
  --output corrected_sg.jpg
```

### Response — 200 OK

Binary JPEG bytes.

| Header | Value |
|--------|-------|
| `Content-Type` | `image/jpeg` |
| `Content-Disposition` | `form-data; name="attachment"; filename="passport_photo.jpg"` |

### Error codes

| Status | Body | Condition |
|--------|------|-----------|
| 400 | `{"message": "Unsupported file type..."}` | File is not JPEG, PNG, or WEBP |
| 400 | `{"message": "N people detected...", "errorType": "MULTIPLE_FACES"}` | More than one face detected |
| 400 | `{"message": "Unknown country code: ZZ"}` | Unknown country code and no `customSpec` |
| 400 | `{"message": "Cannot read image..."}` | File cannot be decoded as an image |
| 500 | `{"message": "Failed to correct image: ..."}` | Unexpected server error |

---

## Error Response Shape

All error responses follow this shape:

```json
{
  "message": "Human-readable description of the error"
}
```

The multiple-faces error includes an additional field:

```json
{
  "message": "3 people detected. Passport photos must show exactly one person — please upload a solo photo.",
  "errorType": "MULTIPLE_FACES"
}
```

---

## Notes

- **CORS**: The backend only allows requests from `http://localhost:3000`. Update `CorsConfig.java` for production.
- **Timeouts**: The frontend sets a 30-second timeout for `/analyze` and 60 seconds for `/correct`. The backend has no explicit timeout.
- **File size**: No server-side file size limit is enforced. The frontend blocks files over 15 MB before they are uploaded.
