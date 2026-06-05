/**
 * Thin logging client that forwards browser-side events to POST /api/log.
 *
 * Rules:
 * - Fire-and-forget: never throws, never awaited, never blocks the UI.
 * - Silent on network failure (backend unreachable → the app still works).
 * - Used in two ways:
 *     Option A — explicit calls inside catch blocks (known error paths).
 *     Option C — wired to window.onerror + window.onunhandledrejection in main.jsx
 *                to catch anything that escapes all try/catch blocks.
 */

const LOG_URL = `${import.meta.env.VITE_API_URL ?? 'http://localhost:8080'}/api/log`

function send(level, message, context = {}) {
  fetch(LOG_URL, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      level,
      message: String(message).substring(0, 500),
      context: JSON.stringify(context).substring(0, 1000),
    }),
  }).catch(() => {})
}

export const logger = {
  info:  (message, context) => send('info',  message, context),
  warn:  (message, context) => send('warn',  message, context),
  error: (message, context) => send('error', message, context),
}
