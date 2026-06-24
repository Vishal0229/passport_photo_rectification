import { useRef, useState, useEffect, useCallback } from 'react'
import * as faceapi from '@vladmandic/face-api'
import { ensureLoaded } from '../services/faceDetection'

// Guide occupies 55% of canvas width, portrait 3:4 aspect ratio
const GUIDE_W_RATIO = 0.55
const GUIDE_ASPECT  = 4 / 3
// Auto-capture after this many consecutive aligned detection frames (~400 ms each)
const ALIGNED_FRAMES_NEEDED = 5

function getGuideRect(cw, ch) {
  const gw = cw * GUIDE_W_RATIO
  const gh = gw * GUIDE_ASPECT
  const gx = (cw - gw) / 2
  const gy = (ch - gh) / 2
  return { gx, gy, gw, gh }
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

// Returns {isAligned, message} given a face-api detection and guide rect in canvas coords.
function checkAlignment(detection, gx, gy, gw, gh, scaleX, scaleY) {
  if (!detection) return { isAligned: false, message: 'Position your face in the oval' }

  const fx = detection.box.x * scaleX
  const fy = detection.box.y * scaleY
  const fw = detection.box.width  * scaleX
  const fh = detection.box.height * scaleY
  const faceCX = fx + fw / 2
  const guideCX = gx + gw / 2

  if (Math.abs(faceCX - guideCX) > gw * 0.15)
    return { isAligned: false, message: faceCX < guideCX ? 'Move right' : 'Move left' }

  const wRatio = fw / gw
  if (wRatio < 0.38) return { isAligned: false, message: 'Move closer' }
  if (wRatio > 0.88) return { isAligned: false, message: 'Move further back' }

  if (fy < gy + gh * 0.03)         return { isAligned: false, message: 'Move down slightly' }
  if (fy + fh > gy + gh * 0.82)    return { isAligned: false, message: 'Move up slightly' }

  return { isAligned: true, message: 'Hold still — auto-capturing…' }
}

function drawOverlay(canvas, video, detection, borderColor) {
  const ctx = canvas.getContext('2d')
  const cw  = canvas.width
  const ch  = canvas.height
  ctx.clearRect(0, 0, cw, ch)

  const { gx, gy, gw, gh } = getGuideRect(cw, ch)

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

  // Face oval (dashed, upper portion of guide)
  const ovalCX = gx + gw / 2
  const ovalCY = gy + gh * 0.34
  const ovalRX = gw * 0.32
  const ovalRY = gh * 0.30
  ctx.beginPath()
  ctx.ellipse(ovalCX, ovalCY, ovalRX, ovalRY, 0, 0, Math.PI * 2)
  ctx.strokeStyle = borderColor
  ctx.lineWidth = 2
  ctx.setLineDash([6, 5])
  ctx.stroke()
  ctx.setLineDash([])

  // Shoulder curve hint
  const sy = gy + gh * 0.70
  ctx.beginPath()
  ctx.moveTo(gx + gw * 0.05, sy)
  ctx.bezierCurveTo(gx + gw * 0.25, gy + gh * 0.56, gx + gw * 0.75, gy + gh * 0.56, gx + gw * 0.95, sy)
  ctx.strokeStyle = borderColor + '66'
  ctx.lineWidth = 1.5
  ctx.setLineDash([4, 5])
  ctx.stroke()
  ctx.setLineDash([])
}

export default function CameraCapture({ onPhotoCapture, onCancel }) {
  const videoRef        = useRef(null)
  const canvasRef       = useRef(null)
  const streamRef       = useRef(null)
  const intervalRef     = useRef(null)
  const alignedRef      = useRef(0)
  const capturedRef     = useRef(false)

  const [camStatus, setCamStatus]   = useState('loading') // loading | ready | error
  const [guidance,  setGuidance]    = useState('Starting camera…')
  const [faceState, setFaceState]   = useState('none')    // none | wrong | aligned
  const [countdown, setCountdown]   = useState(null)
  const [camError,  setCamError]    = useState(null)

  // Capture current video frame and hand it to parent
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

  // Start camera + load model in parallel
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

  // Face detection loop — runs every 400 ms once camera is ready
  useEffect(() => {
    if (camStatus !== 'ready') return

    const tick = async () => {
      if (capturedRef.current) return
      const video  = videoRef.current
      const canvas = canvasRef.current
      if (!video || !canvas || video.readyState < 2) return

      // Sync canvas size to its CSS-rendered size
      const cw = canvas.offsetWidth
      const ch = canvas.offsetHeight
      if (canvas.width !== cw || canvas.height !== ch) {
        canvas.width  = cw
        canvas.height = ch
      }

      let det = null
      try {
        det = await faceapi.detectSingleFace(
          video,
          new faceapi.TinyFaceDetectorOptions({ scoreThreshold: 0.4, inputSize: 320 })
        )
      } catch (_) {}

      const { gx, gy, gw, gh } = getGuideRect(cw, ch)
      const scaleX = video.videoWidth  ? cw / video.videoWidth  : 1
      const scaleY = video.videoHeight ? ch / video.videoHeight : 1
      const { isAligned, message } = checkAlignment(det, gx, gy, gw, gh, scaleX, scaleY)

      const borderColor = !det ? '#94a3b8' : isAligned ? '#22c55e' : '#f59e0b'
      drawOverlay(canvas, video, det, borderColor)

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

      // Face aligned
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
  }, [camStatus, doCapture])

  const handleManualCapture = () => {
    if (capturedRef.current || camStatus !== 'ready') return
    capturedRef.current = true
    clearInterval(intervalRef.current)
    setCountdown(null)
    doCapture()
  }

  return (
    <div className="bg-white rounded-2xl shadow-md p-6">
      <h2 className="text-base font-semibold text-gray-700 mb-4">Take Photo</h2>

      {camError ? (
        <div className="text-center py-8 space-y-3">
          <p className="text-red-600 text-sm">{camError}</p>
          <button onClick={onCancel} className="text-sm text-blue-600 underline">
            ← Back to Upload
          </button>
        </div>
      ) : (
        <>
          {/* Camera viewport */}
          <div className="relative bg-black rounded-xl overflow-hidden" style={{ aspectRatio: '4/3' }}>
            <video
              ref={videoRef}
              autoPlay
              playsInline
              muted
              className="w-full h-full object-cover"
            />
            <canvas
              ref={canvasRef}
              className="absolute inset-0 w-full h-full pointer-events-none"
            />

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

          {/* Guidance text */}
          <p className={`mt-3 text-center text-sm font-medium min-h-[1.25rem] ${
            faceState === 'aligned' ? 'text-green-600' :
            faceState === 'wrong'   ? 'text-amber-600' : 'text-gray-400'
          }`}>
            {guidance}
          </p>

          {/* Buttons */}
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
            Auto-captures when your face is correctly positioned
          </p>
        </>
      )}
    </div>
  )
}
