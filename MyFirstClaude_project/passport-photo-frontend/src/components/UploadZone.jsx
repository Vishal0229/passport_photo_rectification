import { useState, useRef, useEffect } from 'react'
import { countFaces } from '../services/faceDetection'

export default function UploadZone({ onPhotoSelect, onNewFileSelected, photoUrl }) {
  const [dragOver, setDragOver] = useState(false)
  const [checking, setChecking] = useState(false)
  const [faceError, setFaceError] = useState(null)
  const [localPreviewUrl, setLocalPreviewUrl] = useState(null)
  const inputRef = useRef(null)

  useEffect(() => {
    return () => {
      if (localPreviewUrl) URL.revokeObjectURL(localPreviewUrl)
    }
  }, [localPreviewUrl])

  const handleFile = async (file) => {
    if (!file || !file.type.startsWith('image/')) return

    // Immediately clear parent state so Analyze/Correct can't fire on the old photo
    onNewFileSelected?.()

    // Show preview immediately for every file, accepted or rejected
    if (localPreviewUrl) URL.revokeObjectURL(localPreviewUrl)
    setLocalPreviewUrl(URL.createObjectURL(file))

    setChecking(true)
    setFaceError(null)

    try {
      const faces = await countFaces(file)

      if (faces === 0) {
        setFaceError(
          'The photo you uploaded is not of a person. Please upload a passport-style photo of yourself facing the camera.'
        )
        if (inputRef.current) inputRef.current.value = ''
        return
      }

      onPhotoSelect(file)
    } catch (err) {
      // Model load failure or decode error — block the photo and tell the user
      console.error('Face detection error:', err)
      setFaceError(
        'Could not run face detection. Make sure the app is fully loaded and try again.'
      )
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
            : faceError
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

      {/* Face-not-found error */}
      {faceError && (
        <div className="mt-3 bg-red-50 border border-red-200 rounded-xl px-4 py-3 flex items-start gap-2.5">
          <span className="text-xl shrink-0 mt-0.5">🚫</span>
          <div className="flex-1">
            <p className="text-red-700 text-sm font-semibold">Photo is not of a person</p>
            <p className="text-red-600 text-xs mt-0.5 leading-relaxed">{faceError}</p>
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
