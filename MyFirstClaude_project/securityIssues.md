# Security Review Findings

## Scan history

| Scan | Files reviewed | New issues | Resolved |
|------|----------------|-----------|---------|
| Initial scan | Backend controllers, services, logging | 7 total (1H 2M 2L 2I) | — |
| Re-scan (logging/exception hardening) | LoggingAspect, GlobalExceptionHandler, PhotoController, FaceDetectionService, PhotoAnalysisService, logger.js, main.jsx | 0 new | 3 resolved |
| F-002 loading-state fix | App.jsx, AppLoadingState.test.jsx | 0 new | 0 |
| Deployment config scan | Dockerfile, render.yaml | 4 new (1M 1L 2I) | 0 |
| Deployment config fixes | Dockerfile | 0 new | 4 resolved (digest pinning, adduser flags, HEALTHCHECK, VITE_API_URL accepted) |

---

## Open issues (carry-forward — unchanged by deployment scan)

### [HIGH] Unauthenticated log injection endpoint — newlines and ANSI codes not stripped

**File:** `passport-photo-backend/src/main/java/com/passport/photo/controller/PhotoController.java`
**Line:** 401–403 (`sanitizeLog`)
**Description:** The `POST /api/log` endpoint accepts arbitrary `level`, `message`, and `context` strings from any HTTP client and writes them into the server-side log under the `[FRONTEND]` prefix. `sanitizeLog` truncates strings by length but does **not** strip `\n`, `\r`, or ANSI escape sequences (`\x1B[...`). An attacker can therefore:
- Inject forged log lines by embedding `\n` in `message` or `context` (log forging / log injection).
- Poison ANSI-aware terminals or log dashboards with escape sequences.
- Flood the log storage / aggregation quota via high-volume unauthenticated requests (no rate limiting on this endpoint).

The CORS config restricts cross-origin browser requests to `http://localhost:3000`, but direct HTTP clients (curl, scripts) are unrestricted.
**Recommended fix:**
1. Extend `sanitizeLog` to strip newlines and ANSI codes: `s.replaceAll("[\r\n\t]", " ").replaceAll("\\x1B\\[[0-9;]*[mK]", "")`.
2. Add per-IP rate limiting on `/api/log` (e.g., Bucket4j at 20 requests/minute per IP).
3. Log frontend-submitted content at INFO or WARN rather than ERROR to reduce SIEM noise from attacker-controlled input.

---

### [MEDIUM] No rate limiting on any endpoint — CPU/memory exhaustion via repeated uploads

**File:** `passport-photo-backend/src/main/java/com/passport/photo/aspect/LoggingAspect.java`
**Line:** 33 (pointcut covers all application methods)
**Description:** None of the POST endpoints (`/analyze`, `/correct`, `/pdf`, `/sheet`, `/log`) have rate limiting. Each request triggers the AOP `LoggingAspect` around-advice on every method in `com.passport.photo`, multiplying logging overhead. Face detection, PDF generation, and A4 sheet tiling are CPU/memory-intensive operations. An attacker can send a flood of large multipart uploads to exhaust server resources. No evidence of Bucket4j, gateway-level throttling, or Spring Security request throttling anywhere in the codebase.
**Recommended fix:**
1. Set `logging.level.com.passport.photo.aspect=WARN` in `application.properties` so DEBUG entries are suppressed in production.
2. Add per-IP rate limiting via Bucket4j (or an API gateway rule) on all POST endpoints, e.g. 10 requests/minute for `/analyze`, `/correct`, `/pdf`, `/sheet`.

---

### [MEDIUM] Frontend error context may include user-identifiable or architecture-revealing data sent to backend

**File:** `passport-photo-frontend/src/main.jsx` (lines 10–25)
**File:** `passport-photo-frontend/src/services/logger.js` (lines 13–25)
**Description:** The global `window.onerror` and `window.onunhandledrejection` handlers POST the following fields to `POST /api/log`:
- `source` — the script URL of the faulting script (may contain CDN paths or internal origin details).
- `line`, `col` — exact source location, disclosing application structure.
- `stack` — up to 500 characters of a stack trace, persisted in server-side logs.
- `reason` — string representation of the rejection reason, which may include URL parameters or user-entered text if a fetch rejects with a constructed URL.

Stack traces and script source URLs expose internal application architecture and may inadvertently include PII embedded in URLs (e.g., query parameters on a failed API call).
**Recommended fix:**
1. Strip query parameters from the `source` field before sending (e.g., `new URL(source).origin + new URL(source).pathname`).
2. Send only the first line of `stack` rather than up to 500 characters.
3. Apply the same per-IP rate limit recommended for `/api/log` (see HIGH finding above).

---

### [LOW] Cascade XML temporary files are world-readable and may accumulate across restarts

**File:** `passport-photo-backend/src/main/java/com/passport/photo/service/FaceDetectionService.java`
**Lines:** 93–100
**Description:** `File.createTempFile("cascade_", ".xml")` creates files in the system temp directory with default permissions (on Linux/macOS: `rw-r--r--`, readable by all local users). The XML content itself is not sensitive, but this is a defence-in-depth gap and establishes a permissive precedent. The file is marked `deleteOnExit()` only — if the JVM is killed abnormally (`kill -9`, OOM kill) the file persists and accumulates across restarts, leaving stale files in temp.
**Recommended fix:** Use `Files.createTempFile(Path, String, String, FileAttribute<?>)` with `PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------"))` on POSIX systems. Also add a `@PreDestroy` method that explicitly deletes the temp files in addition to the `deleteOnExit()` marker.

---

### [LOW] `wipeBytes` zeroing is a best-effort measure — JIT dead-store elimination and GC copies may retain image data

**File:** `passport-photo-backend/src/main/java/com/passport/photo/controller/PhotoController.java`
**Lines:** 406–410
**Description:** `Arrays.fill(bytes, (byte) 0)` is called in `finally` blocks to zero the image byte array after processing. The JIT compiler may eliminate this as a dead store (the array is about to become unreachable). Additionally, `photo.getBytes()` materialises a copy inside Spring's multipart infrastructure (Tomcat request buffers) that is outside the controller's control. The zeroing therefore provides a false sense of security.
**Recommended fix:** Add a comment documenting that zeroing is best-effort. For a higher assurance level, stream the upload directly into processing without materialising a full `byte[]` in the controller, or use off-heap buffers that can be reliably zeroed.

---

### [INFO] `spring-boot-starter-aop` transitive `aspectjweaver` dependency not confirmed against vulnerability database

**File:** `passport-photo-backend/pom.xml`
**Line:** 33–35 (aop starter declaration)
**Description:** `spring-boot-starter-aop` pulls in `aspectjweaver` transitively. Spring Boot 3.2.5 manages this to version `1.9.21`. No confirmed CVE at time of review, but the version has not been verified against the current OSS advisory databases (OSV, GitHub Advisory Database).
**Recommended fix:** Run `mvn dependency:tree | grep aspectj` and cross-reference the resolved version. Run `mvn org.owasp:dependency-check-maven:check` in CI to catch future regressions.

---

### [INFO] No HTTP security response headers (`X-Content-Type-Options`, `X-Frame-Options`)

**File:** `passport-photo-backend/src/main/java/com/passport/photo/controller/PhotoController.java` (all endpoints)
**Description:** The API does not set `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, or `Referrer-Policy`. Spring Security would inject these automatically, but the project does not include `spring-boot-starter-security`.
**Recommended fix:** Add `spring-boot-starter-security` (with HTTP Basic disabled via `security.basic.enabled=false`) to get security headers automatically, or register a `OncePerRequestFilter` / `HandlerInterceptor` that appends these headers to every response.

---

## Deployment config scan — all 4 findings resolved (2026-06-23)

**Files reviewed:** `passport-photo-backend/Dockerfile`, `render.yaml`

### ✅ FIXED — [MEDIUM] Base images use floating tags — no digest pinning (supply chain risk)

**File:** `passport-photo-backend/Dockerfile`
**Lines:** 2 (`FROM maven:3.9.6-eclipse-temurin-17`) and 14 (`FROM eclipse-temurin:17-jre-jammy`)
**Description:** Both base images are referenced by mutable tag only. Docker tags are not immutable: the upstream maintainer (or a registry compromise) can silently replace the image behind `maven:3.9.6-eclipse-temurin-17` or `eclipse-temurin:17-jre-jammy` with a different layer set. Every rebuild on Render would pull the current content of those tags without any integrity check. This is a supply chain risk — a malicious layer push to Docker Hub would be consumed transparently on the next deploy.

Additionally, `eclipse-temurin:17-jre-jammy` is based on Ubuntu Jammy (22.04). Jammy receives security patches, but the layer baked into the image at build time is frozen until a new image is explicitly pulled or rebuilt. The Dockerfile has no `RUN apt-get upgrade` step, so any OS-level CVEs present in the base layer at the time of the last Docker Hub push are inherited silently.

**Recommended fix:**
1. Pin both images to their SHA-256 digest alongside the tag:
   ```dockerfile
   FROM maven:3.9.6-eclipse-temurin-17@sha256:<digest> AS build
   FROM eclipse-temurin:17-jre-jammy@sha256:<digest>
   ```
   Retrieve current digests with `docker pull <image> && docker inspect --format='{{index .RepoDigests 0}}' <image>`.
2. Add a scheduled CI job (e.g., Dependabot for Docker, or Renovate) to update digests automatically and run a test build when upstream digests change.
3. Optionally add `RUN apt-get update && apt-get upgrade -y && rm -rf /var/lib/apt/lists/*` in the run stage to apply OS patches at image build time (accepted trade-off: larger layers, but fewer inherited CVEs).

---

### ✅ FIXED — [LOW] Non-root user has no explicit home directory or shell restriction

**File:** `passport-photo-backend/Dockerfile`
**Line:** 22 (`RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser`)
**Description:** The `adduser --system` invocation does not set `--no-create-home` or `--shell /usr/sbin/nologin` / `--shell /bin/false`. On Debian/Ubuntu, `--system` implies `--shell /usr/sbin/nologin` and `--no-create-home` is not the default (a home directory under `/home/appuser` or `/nonexistent` is created depending on the distro version). The attack surface is low because there is no shell accessible from outside the container, but the absence of an explicit `--shell /bin/false` and `--home /nonexistent` leaves ambiguity across base image variants.

More concretely: if an attacker achieves RCE inside the container (e.g., via a Java deserialization flaw), the `appuser` account's capabilities are constrained only by filesystem permissions rather than by explicit shell/home restrictions. The `/app` directory is owned by root (written during the build stage before `USER appuser`) which is good, but `appuser` still has write access to `/tmp` and any paths created at runtime.

**Recommended fix:**
```dockerfile
RUN addgroup --system appgroup \
 && adduser --system --ingroup appgroup --no-create-home --shell /bin/false appuser
```
Also consider adding `--read-only` to the Docker run flags (or equivalent in render.yaml if Render supports it) and mounting `/tmp` as a `tmpfs` with a size cap to limit temp file accumulation (related to the cascade-XML temp file finding above).

---

### ✅ FIXED — [INFO] No health-check defined in Dockerfile — Render free plan has no `healthCheckPath`

**File:** `passport-photo-backend/Dockerfile` and `render.yaml` (line 9, comment)
**Description:** The Dockerfile has no `HEALTHCHECK` instruction. The `render.yaml` comment correctly notes that `healthCheckPath` is a paid-plan feature. As a result:
- Render's free-tier scheduler has no signal beyond TCP port availability to determine whether the container is healthy. A process that binds port 8080 but returns 500 on every request would be considered healthy by the platform.
- Docker itself also cannot report unhealthy status, so orchestration restarts (if Render adds them) would not trigger.

This is an operational concern rather than a direct security vulnerability, but an unhealthy container kept in rotation silently could mask an exploited or corrupted runtime.
**Recommended fix:** Add a lightweight `HEALTHCHECK` to the Dockerfile:
```dockerfile
HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD curl -f http://localhost:8080/api/countries || exit 1
```
`curl` must be available in the run stage (`apt-get install -y curl` in the same `RUN` layer that applies OS upgrades — see supply chain finding above). On the paid Render plan, also set `healthCheckPath: /api/countries`.

---

### ✅ ACCEPTED — [INFO] `VITE_API_URL` is a build-time env var — value is baked into the static bundle

**File:** `render.yaml`
**Line:** 24 (`key: VITE_API_URL`, `sync: false`)
**Description:** Vite replaces `import.meta.env.VITE_*` variables at build time via string substitution. This means the backend URL set in the Render dashboard at deploy time is embedded verbatim in the compiled JavaScript bundle (`dist/`) and is visible to anyone who inspects the bundle. This is expected and documented Vite behaviour, but it has two minor implications:
1. The backend service URL is publicly discoverable from the frontend bundle, potentially easing reconnaissance against the API.
2. If the backend URL ever needs to change after a deploy (e.g., you swap to a paid instance with a different hostname), a full frontend rebuild and redeploy is required — the env var cannot be swapped at runtime.

Neither implication is a critical flaw for a public-facing web app (the API endpoints are already public by design), but it is worth documenting so the team is not surprised.
**Recommended fix:** No code change required. Document that `VITE_API_URL` is build-time only. If the backend URL is considered sensitive (e.g., internal-only endpoint protected by IP allowlist), consider proxying through a Render Rewrite rule instead of exposing the origin directly.

---

## F-002 scan notes (App.jsx + AppLoadingState.test.jsx)

The F-002 loading-state fix was reviewed and found clean:

- **`countrySpecsLoading` state + `.finally()` chain (`App.jsx` lines 37, 61):** Pure boolean state toggle. No server response data is echoed into the DOM on either the loading or error branch. The fallback card text is a hardcoded string (`"Could not load country requirements. Make sure the backend is running."`).
- **Spinner JSX (`App.jsx` lines 252–255):** CSS-only; no user-controlled content.
- **Error display (`App.jsx` line 317):** `{error}` is rendered as a React text node, not via `dangerouslySetInnerHTML`. React escapes this automatically; no XSS vector.
- **`VITE_API_URL` usage (`App.jsx` line 59):** Unchanged from prior scans; build-time env var, not user-controlled at runtime.
- **`AppLoadingState.test.jsx`:** Test-only file, never shipped. Uses `vi.mock` stubs; no runtime security surface.

No new security issues introduced by F-002.

---

**Summary: 1 HIGH · 2 MEDIUM · 2 LOW · 2 INFO = 7 open issues (4 deployment findings resolved).**
