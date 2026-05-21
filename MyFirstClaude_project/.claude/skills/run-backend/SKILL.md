---
description: Kill any stale port-8080 listener and start the Spring Boot backend
---

## Steps

1. Kill any process already on port 8080 (leftover Spring Boot windows are common):

```powershell
$listener = netstat -ano | Select-String ":8080 " | Select-String "LISTENING" | Select-Object -First 1
if ($listener) {
  $stalePid = ($listener.ToString() -split '\s+' | Where-Object { $_ -match '^\d+$' } | Select-Object -Last 1)
  Stop-Process -Id $stalePid -Force
  Write-Host "Killed stale process $stalePid on port 8080"
  Start-Sleep -Seconds 1
}
```

2. Start the backend in a background window:

```powershell
Start-Process -FilePath "powershell" `
  -ArgumentList "-NoProfile -Command `"cd C:\Claude\MyFirstClaude_project\passport-photo-backend; mvn spring-boot:run`"" `
  -WindowStyle Minimized
```

3. Wait and verify:

```powershell
$ready = $false
for ($i = 0; $i -lt 12; $i++) {
  Start-Sleep -Seconds 5
  try {
    $r = Invoke-WebRequest -Uri "http://localhost:8080/api/countries" -UseBasicParsing -TimeoutSec 3
    if ($r.StatusCode -eq 200) { $ready = $true; break }
  } catch {}
}
if ($ready) { Write-Host "Backend ready on http://localhost:8080" }
else         { Write-Host "Backend did not start within 60 s — check the minimised window for errors" }
```

## Ready signal
`GET http://localhost:8080/api/countries` → HTTP 200

## Notes
- First run after a clean Maven cache can take 2-3 minutes to download OpenCV natives (~80 MB)
- The Haar cascade XML (`haarcascade_frontalface_default.xml`) is downloaded once by the Maven build and cached in `target/generated-resources/`; `haarcascade_frontalface_alt2.xml` is bundled in `src/main/resources/`
- Spring Boot logs appear in the minimised PowerShell window
