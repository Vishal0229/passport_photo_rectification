import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App.jsx'
import './index.css'
import { logger } from './services/logger.js'

// Option C — global safety net for anything that escapes all try/catch blocks.
// Covers: unexpected JS crashes, component errors that aren't caught by ErrorBoundary,
// and forgotten promise rejections.
window.onerror = (msg, source, line, col, err) => {
  logger.error('Uncaught JS error', {
    message: String(msg),
    source,
    line,
    col,
    stack: err?.stack?.substring(0, 500),
  })
}

window.onunhandledrejection = (event) => {
  logger.error('Unhandled promise rejection', {
    reason: String(event.reason?.message ?? event.reason),
    stack:  event.reason?.stack?.substring(0, 500),
  })
}

if ('serviceWorker' in navigator) {
  window.addEventListener('load', () => {
    navigator.serviceWorker.register('/sw.js').catch(() => {})
  })
}

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
)
