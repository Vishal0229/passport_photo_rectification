# Security Review Findings (Re-scan after fixes)

Reviewed files:
- `passport-photo-backend/src/main/java/com/passport/photo/aspect/LoggingAspect.java`
- `passport-photo-backend/src/main/java/com/passport/photo/controller/GlobalExceptionHandler.java`
- `passport-photo-backend/src/main/java/com/passport/photo/controller/PhotoController.java`
- `passport-photo-backend/src/main/java/com/passport/photo/service/FaceDetectionService.java`
- `passport-photo-backend/src/main/java/com/passport/photo/service/PhotoAnalysisService.java`
- `passport-photo-frontend/src/utils/logger.js`
- `passport-photo-frontend/src/main.jsx`
- `passport-photo-frontend/src/services/logger.js` (referenced by main.jsx)

Issues resolved since last scan (not listed below):
- IOException no longer leaks internal paths or messages to clients — generic message returned.
- LoggingAspect and GlobalExceptionHandler now sanitise exception messages before logging (newlines stripped via `replaceAll("[\r\n]", " ")`).
- MissingServletRequestParameterException now returns a generic message instead of echoing the Spring-generated parameter name and type.
- Magic-byte MIME validation added to `validateImageType`; Content-Type header alone is no longer sufficient.
- Dead `initGlobalErrorLogging()` call removed from `main.jsx`.

---

## [HIGH] Unauthenticated log injection endpoint — newlines and ANSI codes not stripped

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

## [MEDIUM] No rate limiting on any endpoint — CPU/memory exhaustion via repeated uploads

**File:** `passport-photo-backend/src/main/java/com/passport/photo/aspect/LoggingAspect.java`  
**Line:** 33 (pointcut covers all application methods)  
**Description:** None of the POST endpoints (`/analyze`, `/correct`, `/pdf`, `/sheet`, `/log`) have rate limiting. Each request triggers the AOP `LoggingAspect` around-advice on every method in `com.passport.photo`, multiplying logging overhead. Face detection, PDF generation, and A4 sheet tiling are CPU/memory-intensive operations. An attacker can send a flood of large multipart uploads to exhaust server resources. No evidence of Bucket4j, gateway-level throttling, or Spring Security request throttling anywhere in the codebase.  
**Recommended fix:**
1. Set `logging.level.com.passport.photo.aspect=WARN` in `application.properties` so DEBUG entries are suppressed in production.
2. Add per-IP rate limiting via Bucket4j (or an API gateway rule) on all POST endpoints, e.g. 10 requests/minute for `/analyze`, `/correct`, `/pdf`, `/sheet`.

---

## [MEDIUM] Frontend error context may include user-identifiable or architecture-revealing data sent to backend

**File:** `passport-photo-frontend/src/main.jsx`  
**Lines:** 10–25  
**File:** `passport-photo-frontend/src/services/logger.js`  
**Lines:** 13–25  
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

## [LOW] Cascade XML temporary files are world-readable and may accumulate across restarts

**File:** `passport-photo-backend/src/main/java/com/passport/photo/service/FaceDetectionService.java`  
**Lines:** 93–100  
**Description:** `File.createTempFile("cascade_", ".xml")` creates files in the system temp directory with default permissions (on Linux/macOS: `rw-r--r--`, readable by all local users). The XML content itself is not sensitive, but this is a defence-in-depth gap and establishes a permissive precedent. The file is marked `deleteOnExit()` only — if the JVM is killed abnormally (`kill -9`, OOM kill) the file persists and accumulates across restarts, leaving stale files in temp.  
**Recommended fix:** Use `Files.createTempFile(Path, String, String, FileAttribute<?>)` with `PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------"))` on POSIX systems. Also add a `@PreDestroy` method that explicitly deletes the temp files in addition to the `deleteOnExit()` marker.

---

## [LOW] `wipeBytes` zeroing is a best-effort measure — JIT dead-store elimination and GC copies may retain image data

**File:** `passport-photo-backend/src/main/java/com/passport/photo/controller/PhotoController.java`  
**Lines:** 406–410  
**Description:** `Arrays.fill(bytes, (byte) 0)` is called in `finally` blocks to zero the image byte array after processing. The JIT compiler may eliminate this as a dead store (the array is about to become unreachable). Additionally, `photo.getBytes()` materialises a copy inside Spring's multipart infrastructure (Tomcat request buffers) that is outside the controller's control. The zeroing therefore provides a false sense of security.  
**Recommended fix:** Add a comment documenting that zeroing is best-effort. For a higher assurance level, stream the upload directly into processing without materialising a full `byte[]` in the controller, or use off-heap buffers that can be reliably zeroed.

---

## [INFO] `spring-boot-starter-aop` transitive `aspectjweaver` dependency not confirmed against vulnerability database

**File:** `passport-photo-backend/pom.xml`  
**Line:** 33–35 (aop starter declaration)  
**Description:** `spring-boot-starter-aop` pulls in `aspectjweaver` transitively. Spring Boot 3.2.5 manages this to version `1.9.21`. No confirmed CVE at time of review, but the version has not been verified against the current OSS advisory databases (OSV, GitHub Advisory Database).  
**Recommended fix:** Run `mvn dependency:tree | grep aspectj` and cross-reference the resolved version. Run `mvn org.owasp:dependency-check-maven:check` in CI to catch future regressions.

---

## [INFO] No HTTP security response headers (`X-Content-Type-Options`, `X-Frame-Options`)

**File:** `passport-photo-backend/src/main/java/com/passport/photo/controller/PhotoController.java` (all endpoints)  
**Description:** The API does not set `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, or `Referrer-Policy`. Spring Security would inject these automatically, but the project does not include `spring-boot-starter-security`.  
**Recommended fix:** Add `spring-boot-starter-security` (with HTTP Basic disabled via `security.basic.enabled=false`) to get security headers automatically, or register a `OncePerRequestFilter` / `HandlerInterceptor` that appends these headers to every response.

---

**Summary:** 1 HIGH · 2 MEDIUM · 2 LOW · 2 INFO = 7 open issues remaining (3 issues resolved since last scan).
