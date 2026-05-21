---
description: Start the Vite dev server for the passport-photo React frontend
---

## Steps

1. Start the frontend in a background window:

```powershell
Start-Process -FilePath "powershell" `
  -ArgumentList "-NoProfile -Command `"cd C:\Claude\MyFirstClaude_project\passport-photo-frontend; npm run dev`"" `
  -WindowStyle Minimized
```

2. Wait and verify:

```powershell
$ready = $false
for ($i = 0; $i -lt 6; $i++) {
  Start-Sleep -Seconds 3
  try {
    $r = Invoke-WebRequest -Uri "http://localhost:3000" -UseBasicParsing -TimeoutSec 3
    if ($r.StatusCode -eq 200) { $ready = $true; break }
  } catch {}
}
if ($ready) { Write-Host "Frontend ready on http://localhost:3000" }
else         { Write-Host "Frontend did not start — check the minimised window for errors" }
```

## Ready signal
`GET http://localhost:3000` → HTTP 200

## Notes
- Backend must also be running on port 8080 for Analyze / Correct to work — run `/run-backend` first
- Vite hot-reloads on file save; no restart needed for frontend-only changes
- face-api model files are served from `public/models/` — the browser downloads them on the first photo upload (TinyFaceDetector, ~190 KB)
- If `node_modules` is missing, run `npm install` in `passport-photo-frontend/` first
