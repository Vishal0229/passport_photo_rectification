import { useState, useRef, useEffect } from 'react'
import { countFaces } from '../services/faceDetection'
import { logger } from '../services/logger'

/**
 * Drag-and-drop photo upload zone with inline browser-side face detection.
 *
 * Accepts JPEG, PNG, and WEBP files up to 15 MB. For each file the user selects:
 * 1. Immediately shows a preview with a loading spinner.
 * 2. Runs {@link countFaces} (TinyFaceDetector neural network) in the browser.
 * 3. Rejects the file with an error message if more than one face is detected.
 * 4. Calls `onPhotoSelect` only when the file passes the face check.
 *
 * Calls `onNewFileSelected` before every face-detection attempt so the parent
 * component can clear stale analysis and correction state immediately, preventing
 * the Analyze/Correct buttons from firing on the previous photo while detection runs.
 *
 * @param {Object}   props
 * @param {Function} props.onPhotoSelect     - Called with the `File` object after it passes face detection.
 * @param {Function} props.onNewFileSelected - Called immediately when any new file is selected, before detection completes.
 * @param {string|null} props.photoUrl       - Object URL of the currently accepted photo, used to keep the preview visible.
 * @returns {JSX.Element} The upload card with drop zone, preview, and error messaging.
 */
export default function UploadZone({ onPhotoSelect, onNewFileSelected, photoUrl }) {
  const [dragOver, setDragOver] = useState(false)
  const [checking, setChecking] = useState(false)
  // { heading, message } or null
  const [faceError, setFaceError] = useState(null)
  const [localPreviewUrl, setLocalPreviewUrl] = useState(null)
  const inputRef = useRef(null)

  useEffect(() => {
    return () => {
      if (localPreviewUrl) URL.revokeObjectURL(localPreviewUrl)
    }
  }, [localPreviewUrl])

  const MAX_SIZE_BYTES = 15 * 1024 * 1024

  const handleFile = async (file) => {
    if (!file || !file.type.startsWith('image/')) return

    if (file.size > MAX_SIZE_BYTES) {
      setFaceError({ heading: 'File too large', message: 'Please upload an image smaller than 15 MB.' })
      if (inputRef.current) inputRef.current.value = ''
      return
    }

    // Immediately clear parent state so Analyze/Correct can't fire on the old photo
    onNewFileSelected?.()

    // Show preview immediately for every file, accepted or rejected
    if (localPreviewUrl) URL.revokeObjectURL(localPreviewUrl)
    setLocalPreviewUrl(URL.createObjectURL(file))

    setChecking(true)
    setFaceError(null)

    try {
      const faces = await countFaces(file)

      if (faces > 1) {
        setFaceError({
          heading: 'Multiple people detected',
          message: `${faces} people detected. Passport photos must show exactly one person — please upload a solo photo.`,
        })
        if (inputRef.current) inputRef.current.value = ''
        return
      }

      onPhotoSelect(file)
    } catch (err) {
      logger.error('Face detection error', { message: err.message, stack: err.stack?.substring(0, 500) })
      setFaceError({
        heading: 'Face detection unavailable',
        message: 'Could not run face detection. Make sure the app is fully loaded and try again.',
      })
      if (inputRef.current) inputRef.current.value = ''
    } finally {
      setChecking(false)
    }
  }

  const handleDrop = (e) => {
    e.preventDefault()
    setDragOver(false)
    handleFile(e.dataTransfer.files[0])
  }

  const open = () => {
    if (!checking) inputRef.current?.click()
  }

  return (
    <div className="bg-white rounded-2xl shadow-md p-6">
      <h2 className="text-base font-semibold text-gray-700 mb-4">Upload Photo</h2>

      {/* Drop zone */}
      <div
        className={`border-2 border-dashed rounded-xl p-6 text-center cursor-pointer transition-all ${
          checking
            ? 'border-blue-300 bg-blue-50 cursor-wait'
            : dragOver
            ? 'border-blue-500 bg-blue-50 scale-[1.01]'
            : faceError != null
            ? 'border-red-300 bg-red-50'
            : 'border-gray-300 hover:border-blue-400 hover:bg-gray-50'
        }`}
        onDragOver={(e) => { e.preventDefault(); setDragOver(true) }}
        onDragLeave={() => setDragOver(false)}
        onDrop={handleDrop}
        onClick={open}
      >
        <input
          ref={inputRef}
          type="file"
          accept="image/jpeg,image/png,image/webp"
          className="hidden"
          onChange={(e) => handleFile(e.target.files[0])}
        />

        {checking ? (
          <div className="space-y-3">
            {localPreviewUrl && (
              <div className="relative inline-block">
                <img
                  src={localPreviewUrl}
                  alt="Checking photo"
                  className="mx-auto max-h-56 rounded-lg object-contain shadow-sm opacity-60"
                />
                <div className="absolute inset-0 flex items-center justify-center">
                  <span className="inline-block w-10 h-10 border-4 border-blue-400 border-t-transparent rounded-full animate-spin" />
                </div>
              </div>
            )}
            {!localPreviewUrl && (
              <div className="flex justify-center py-4">
                <span className="inline-block w-10 h-10 border-4 border-blue-400 border-t-transparent rounded-full animate-spin" />
              </div>
            )}
            <p className="text-blue-600 font-medium text-sm">Checking for a face…</p>
            <p className="text-blue-400 text-xs">This takes a moment on the first photo</p>
          </div>
        ) : (localPreviewUrl || photoUrl) ? (
          <div className="space-y-3">
            <img
              src={localPreviewUrl || photoUrl}
              alt="Uploaded photo"
              className="mx-auto max-h-72 rounded-lg object-contain shadow-sm"
            />
            <p className="text-sm text-gray-400">Click or drop to replace</p>
          </div>
        ) : (
          <div className="space-y-3 py-4">
            <div className="text-5xl select-none">📷</div>
            <p className="text-gray-600 font-medium">Drag &amp; drop your photo here</p>
            <p className="text-gray-400 text-sm">or click to browse files</p>
            <p className="text-gray-400 text-xs font-medium mt-1">Must contain a clearly visible face</p>
            <p className="text-gray-300 text-xs">JPG · PNG · WEBP · up to 15 MB</p>
          </div>
        )}
      </div>

      {/* Upload error */}
      {faceError && (
        <div className="mt-3 bg-red-50 border border-red-200 rounded-xl px-4 py-3 flex items-start gap-2.5">
          <span className="text-xl shrink-0 mt-0.5">⚠️</span>
          <div className="flex-1">
            <p className="text-red-700 text-sm font-semibold">{faceError.heading}</p>
            <p className="text-red-600 text-xs mt-0.5 leading-relaxed">{faceError.message}</p>
            <button
              onClick={(e) => { e.stopPropagation(); setFaceError(null); open() }}
              className="mt-2 text-xs text-red-500 underline hover:text-red-700"
            >
              Try a different photo →
            </button>
          </div>
        </div>
      )}
    </div>
  )
}
