import { useRef, useState, useEffect, useCallback } from 'react'
import * as faceapi from '@vladmandic/face-api'
import { ensureLoaded } from '../services/faceDetection'

const GUIDE_W_RATIO         = 0.55   // guide occupies 55% of canvas width (portrait)
const ALIGNED_FRAMES_NEEDED = 5      // ~2 s at 400 ms/frame

// Default spec used when none is provided
const DEFAULT_SPEC = {
  widthMm: 35, heightMm: 45,
  faceRatioMin: 0.50, faceRatioMax: 0.75,
  backgroundColorHex: '#FFFFFF', backgroundColor: 'WHITE',
}

/**
 * Returns the guide rectangle in canvas px, respecting the country's aspect ratio.
 * For landscape specs (widthMm > heightMm) the guide is wider than tall.
 */
function getGuideRect(cw, ch, spec) {
  const { widthMm = 35, heightMm = 45 } = spec || DEFAULT_SPEC
  const aspect = heightMm / widthMm   // > 1 portrait, < 1 landscape

  let gw, gh
  if (aspect >= 1) {
    // Portrait — constrain by width
    gw = cw * GUIDE_W_RATIO
    gh = gw * aspect
    if (gh > ch * 0.88) { gh = ch * 0.88; gw = gh / aspect }
  } else {
    // Landscape (e.g. Japan 45×35 mm)
    gh = ch * 0.55
    gw = gh / aspect
    if (gw > cw * 0.88) { gw = cw * 0.88; gh = gw * aspect }
  }
  return { gx: (cw - gw) / 2, gy: (ch - gh) / 2, gw, gh }
}

function roundedRectPath(ctx, x, y, w, h, r) {
  ctx.beginPath()
  ctx.moveTo(x + r, y)
  ctx.lineTo(x + w - r, y)
  ctx.arcTo(x + w, y,     x + w, y + r,     r)
  ctx.lineTo(x + w, y + h - r)
  ctx.arcTo(x + w, y + h, x + w - r, y + h, r)
  ctx.lineTo(x + r, y + h)
  ctx.arcTo(x,     y + h, x,     y + h - r, r)
  ctx.lineTo(x, y + r)
  ctx.arcTo(x,     y,     x + r, y,         r)
  ctx.closePath()
}

/**
 * Checks whether the detected face satisfies the country spec's face-ratio
 * and position requirements. Returns {isAligned, message}.
 */
function checkAlignment(detection, gx, gy, gw, gh, scaleX, scaleY, spec) {
  const { faceRatioMin = 0.50, faceRatioMax = 0.75 } = spec || DEFAULT_SPEC

  if (!detection) return { isAligned: false, message: 'Position your face in the oval' }

  const fx = detection.box.x * scaleX
  const fy = detection.box.y * scaleY
  const fw = detection.box.width  * scaleX
  const fh = detection.box.height * scaleY
  const faceCX = fx + fw / 2
  const guideCX = gx + gw / 2

  // Horizontal centering
  if (Math.abs(faceCX - guideCX) > gw * 0.15)
    return { isAligned: false, message: faceCX < guideCX ? 'Move right' : 'Move left' }

  // Vertical position — face should be in the upper portion of the guide
  if (fy < gy + gh * 0.02)      return { isAligned: false, message: 'Move down slightly' }
  if (fy + fh > gy + gh * 0.85) return { isAligned: false, message: 'Move up slightly' }

  // Face ratio: face height / guide height must be within spec range
  const ratio = fh / gh
  if (ratio < faceRatioMin - 0.04) return { isAligned: false, message: 'Move closer' }
  if (ratio > faceRatioMax + 0.04) return { isAligned: false, message: 'Move further back' }

  return { isAligned: true, message: 'Hold still — auto-capturing…' }
}

function drawOverlay(canvas, detection, borderColor, spec) {
  const ctx = canvas.getContext('2d')
  const cw  = canvas.width
  const ch  = canvas.height
  ctx.clearRect(0, 0, cw, ch)

  const { gx, gy, gw, gh } = getGuideRect(cw, ch, spec)

  // Dark vignette outside guide, punched out inside
  ctx.save()
  ctx.fillStyle = 'rgba(0,0,0,0.52)'
  ctx.fillRect(0, 0, cw, ch)
  ctx.globalCompositeOperation = 'destination-out'
  roundedRectPath(ctx, gx, gy, gw, gh, 10)
  ctx.fill()
  ctx.restore()

  // Guide border
  roundedRectPath(ctx, gx, gy, gw, gh, 10)
  ctx.strokeStyle = borderColor
  ctx.lineWidth = 3
  ctx.stroke()

  // Face oval sized to spec's target face ratio (midpoint of min/max range)
  const { faceRatioMin = 0.50, faceRatioMax = 0.75 } = spec || DEFAULT_SPEC
  const targetRatio = (faceRatioMin + faceRatioMax) / 2
  const ovalRY = (gh * targetRatio) / 2
  const ovalRX = ovalRY * 0.76   // typical face width ≈ 76% of height
  const ovalCX = gx + gw / 2
  const ovalCY = gy + gh * 0.08 + ovalRY   // ~8% headroom above oval

  ctx.beginPath()
  ctx.ellipse(ovalCX, ovalCY, ovalRX, ovalRY, 0, 0, Math.PI * 2)
  ctx.strokeStyle = borderColor
  ctx.lineWidth = 2
  ctx.setLineDash([6, 5])
  ctx.stroke()
  ctx.setLineDash([])

  // Shoulder curve below the oval
  const sy = ovalCY + ovalRY + gh * 0.10
  ctx.beginPath()
  ctx.moveTo(gx + gw * 0.05, sy)
  ctx.bezierCurveTo(
    gx + gw * 0.28, ovalCY + ovalRY + gh * 0.02,
    gx + gw * 0.72, ovalCY + ovalRY + gh * 0.02,
    gx + gw * 0.95, sy,
  )
  ctx.strokeStyle = borderColor + '66'
  ctx.lineWidth = 1.5
  ctx.setLineDash([4, 5])
  ctx.stroke()
  ctx.setLineDash([])
}

export default function CameraCapture({ onPhotoCapture, onCancel, spec }) {
  const videoRef    = useRef(null)
  const canvasRef   = useRef(null)
  const streamRef   = useRef(null)
  const intervalRef = useRef(null)
  const alignedRef  = useRef(0)
  const capturedRef = useRef(false)

  const [camStatus, setCamStatus] = useState('loading')
  const [guidance,  setGuidance]  = useState('Starting camera…')
  const [faceState, setFaceState] = useState('none')   // none | wrong | aligned
  const [countdown, setCountdown] = useState(null)
  const [camError,  setCamError]  = useState(null)

  const effectiveSpec = spec || DEFAULT_SPEC
  const bgHex  = effectiveSpec.backgroundColorHex || '#FFFFFF'
  const bgName = effectiveSpec.backgroundColor
    ? effectiveSpec.backgroundColor.charAt(0) + effectiveSpec.backgroundColor.slice(1).toLowerCase()
    : 'white'

  const doCapture = useCallback(() => {
    const video = videoRef.current
    if (!video) return
    const offscreen = document.createElement('canvas')
    offscreen.width  = video.videoWidth
    offscreen.height = video.videoHeight
    offscreen.getContext('2d').drawImage(video, 0, 0)
    offscreen.toBlob(blob => {
      if (!blob) return
      streamRef.current?.getTracks().forEach(t => t.stop())
      onPhotoCapture(new File([blob], 'camera-capture.jpg', { type: 'image/jpeg' }))
    }, 'image/jpeg', 0.92)
  }, [onPhotoCapture])

  // Start camera + preload model in parallel
  useEffect(() => {
    let cancelled = false
    ;(async () => {
      try {
        let stream
        try {
          stream = await navigator.mediaDevices.getUserMedia({
            video: { facingMode: { ideal: 'user' }, width: { ideal: 1280 }, height: { ideal: 720 } },
          })
        } catch (_) {
          stream = await navigator.mediaDevices.getUserMedia({ video: true })
        }
        await ensureLoaded()
        if (cancelled) { stream.getTracks().forEach(t => t.stop()); return }
        streamRef.current = stream
        videoRef.current.srcObject = stream
        await videoRef.current.play()
        setCamStatus('ready')
        setGuidance('Position your face in the oval')
      } catch (err) {
        if (!cancelled) {
          setCamError(
            err.name === 'NotAllowedError'
              ? 'Camera access denied. Allow camera access in your browser settings and try again.'
              : err.name === 'NotFoundError'
              ? 'No camera found on this device. Use Upload Photo instead, or try on a phone or laptop with a webcam.'
              : `Camera error: ${err.message}`
          )
          setCamStatus('error')
        }
      }
    })()
    return () => {
      cancelled = true
      streamRef.current?.getTracks().forEach(t => t.stop())
      clearInterval(intervalRef.current)
    }
  }, [])

  // Face detection loop — every 400 ms
  useEffect(() => {
    if (camStatus !== 'ready') return

    const tick = async () => {
      if (capturedRef.current) return
      const video  = videoRef.current
      const canvas = canvasRef.current
      if (!video || !canvas || video.readyState < 2) return

      const cw = canvas.offsetWidth
      const ch = canvas.offsetHeight
      if (canvas.width !== cw || canvas.height !== ch) { canvas.width = cw; canvas.height = ch }

      let det = null
      try {
        det = await faceapi.detectSingleFace(
          video,
          new faceapi.TinyFaceDetectorOptions({ scoreThreshold: 0.4, inputSize: 320 })
        )
      } catch (_) {}

      const { gx, gy, gw, gh } = getGuideRect(cw, ch, effectiveSpec)
      const scaleX = video.videoWidth  ? cw / video.videoWidth  : 1
      const scaleY = video.videoHeight ? ch / video.videoHeight : 1
      const { isAligned, message } = checkAlignment(det, gx, gy, gw, gh, scaleX, scaleY, effectiveSpec)

      const borderColor = !det ? '#94a3b8' : isAligned ? '#22c55e' : '#f59e0b'
      drawOverlay(canvas, det, borderColor, effectiveSpec)

      if (!det) {
        setFaceState('none'); setGuidance('Position your face in the oval')
        alignedRef.current = 0; setCountdown(null)
        return
      }
      if (!isAligned) {
        setFaceState('wrong'); setGuidance(message)
        alignedRef.current = 0; setCountdown(null)
        return
      }

      setFaceState('aligned'); setGuidance(message)
      alignedRef.current += 1
      const remaining = ALIGNED_FRAMES_NEEDED - alignedRef.current
      setCountdown(remaining >= 3 ? 3 : remaining >= 1 ? 2 : 1)

      if (alignedRef.current >= ALIGNED_FRAMES_NEEDED) {
        capturedRef.current = true
        clearInterval(intervalRef.current)
        setCountdown(null)
        doCapture()
      }
    }

    intervalRef.current = setInterval(tick, 400)
    return () => clearInterval(intervalRef.current)
  }, [camStatus, doCapture, effectiveSpec])

  const handleManualCapture = () => {
    if (capturedRef.current || camStatus !== 'ready') return
    capturedRef.current = true
    clearInterval(intervalRef.current)
    setCountdown(null)
    doCapture()
  }

  return (
    <div className="bg-white rounded-2xl shadow-md p-6">
      <h2 className="text-base font-semibold text-gray-700 mb-1">Take Photo</h2>

      {/* Background colour requirement */}
      <div className="flex items-center gap-2 mb-4">
        <span className="text-xs text-gray-500">Required background:</span>
        <span
          className="w-4 h-4 rounded-full border border-gray-300 shrink-0"
          style={{ backgroundColor: bgHex }}
        />
        <span className="text-xs font-medium text-gray-600">{bgName}</span>
      </div>

      {camError ? (
        <div className="text-center py-8 space-y-3">
          <p className="text-red-600 text-sm">{camError}</p>
          <button onClick={onCancel} className="text-sm text-blue-600 underline">
            ← Back to Upload
          </button>
        </div>
      ) : (
        <>
          <div className="relative bg-black rounded-xl overflow-hidden" style={{ aspectRatio: '4/3' }}>
            <video ref={videoRef} autoPlay playsInline muted className="w-full h-full object-cover" />
            <canvas ref={canvasRef} className="absolute inset-0 w-full h-full pointer-events-none" />

            {camStatus === 'loading' && (
              <div className="absolute inset-0 flex flex-col items-center justify-center bg-black/70 gap-3">
                <span className="w-8 h-8 border-2 border-white border-t-transparent rounded-full animate-spin" />
                <p className="text-white text-sm">Starting camera…</p>
              </div>
            )}
            {countdown !== null && (
              <div className="absolute inset-0 flex items-center justify-center pointer-events-none">
                <span className="text-7xl font-bold text-white drop-shadow-[0_2px_8px_rgba(0,0,0,0.8)]">
                  {countdown}
                </span>
              </div>
            )}
          </div>

          {/* Guidance */}
          <p className={`mt-3 text-center text-sm font-medium min-h-[1.25rem] ${
            faceState === 'aligned' ? 'text-green-600' :
            faceState === 'wrong'   ? 'text-amber-600' : 'text-gray-400'
          }`}>
            {guidance}
          </p>

          {/* Face ratio hint */}
          <p className="mt-1 text-center text-xs text-gray-400">
            Face should fill {Math.round(effectiveSpec.faceRatioMin * 100)}–{Math.round(effectiveSpec.faceRatioMax * 100)}% of photo height
          </p>

          <div className="mt-4 flex gap-3">
            <button
              onClick={onCancel}
              className="flex-1 py-2 px-4 text-sm border border-gray-200 rounded-xl text-gray-600 hover:bg-gray-50 transition-colors"
            >
              Cancel
            </button>
            <button
              onClick={handleManualCapture}
              disabled={camStatus !== 'ready'}
              className="flex-1 py-2 px-4 text-sm bg-blue-600 text-white rounded-xl font-medium hover:bg-blue-700 disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
            >
              📷 Capture Now
            </button>
          </div>
          <p className="mt-2 text-center text-xs text-gray-400">
            Auto-captures when face is correctly positioned for the selected country
          </p>
        </>
      )}
    </div>
  )
}
