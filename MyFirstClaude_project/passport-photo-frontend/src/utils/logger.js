/**
 * Centralised frontend logging utility.
 *
 * Named exports:
 *   logEntry(methodName, argTypes)  — DEBUG on method entry
 *   logExit(methodName, returnType) — DEBUG on normal method exit
 *   logError(source, error)         — ERROR on exception / global error event
 *   initGlobalErrorLogging()        — registers window.onerror + window.onunhandledrejection
 *                                     (idempotent: safe to call multiple times)
 */

let _globalErrorLoggingInitialised = false

/**
 * Logs method entry at DEBUG level.
 *
 * @param {string}   methodName - name of the method being entered
 * @param {string[]} argTypes   - simple type names of the arguments (not values)
 */
export function logEntry(methodName, argTypes = []) {
  console.debug(`>> ${methodName}(${argTypes.join(', ')})`)
}

/**
 * Logs normal method exit at DEBUG level.
 *
 * @param {string} methodName  - name of the method that returned
 * @param {string} returnType  - simple name of the return type (e.g. "string", "void")
 */
export function logExit(methodName, returnType = 'void') {
  console.debug(`<< ${methodName} → ${returnType}`)
}

/**
 * Logs an error at ERROR level.
 *
 * @param {string} source - identifier of the error's origin (e.g. "window.onerror")
 * @param {Error|unknown} error - the error object or value
 */
export function logError(source, error) {
  const errorType = error instanceof Error ? error.constructor.name : typeof error
  const message   = error instanceof Error ? error.message : String(error)
  console.error(`!! [${source}] ${errorType}: ${message}`)
}

/**
 * Registers global browser error listeners. Idempotent — repeated calls are no-ops.
 *
 * Listeners registered:
 *   window.onerror              — catches synchronous uncaught JS exceptions
 *   window.onunhandledrejection — catches unhandled Promise rejections
 */
export function initGlobalErrorLogging() {
  if (_globalErrorLoggingInitialised) return
  _globalErrorLoggingInitialised = true

  window.onerror = (msg, _source, _line, _col, err) => {
    logError('window.onerror', err ?? new Error(String(msg)))
  }

  window.onunhandledrejection = (event) => {
    const reason = event.reason
    logError('unhandledrejection', reason instanceof Error ? reason : new Error(String(reason)))
  }
}
