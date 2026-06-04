/**
 * Browser-side face detection service using @vladmandic/face-api TinyFaceDetector.
 *
 * Model weights are served from `/public/models/` on the same host — no CDN
 * dependency. The model is loaded lazily on the first call to {@link countFaces}
 * and cached for the lifetime of the page. Subsequent calls reuse the loaded model.
 *
 * Detection settings: scoreThreshold=0.4, inputSize=416.
 */
import * as faceapi from '@vladmandic/face-api'

/** Path to the TinyFaceDetector model weights (relative to the public root). */
const MODEL_URL = '/models'

let ready = false
let loadPromise = null

/**
 * Ensures the TinyFaceDetector model is loaded before detection runs.
 *
 * Uses a shared promise so multiple concurrent callers wait for the same load
 * rather than triggering multiple network requests.
 *
 * @returns {Promise<void>}
 */
async function ensureLoaded() {
  if (ready) return
  if (!loadPromise) {
    loadPromise = faceapi.nets.tinyFaceDetector.loadFromUri(MODEL_URL)
      .catch(err => { loadPromise = null; throw err })
  }
  await loadPromise
  ready = true
}

/**
 * Returns the number of faces detected in `imageFile`.
 *
 * Throws:
 *   - If the model fails to load (network/file missing) — the caller must handle this and show an error.
 *     We intentionally do NOT silently pass through; that was the bug in the previous version.
 *   - If the image cannot be decoded.
 */
export async function countFaces(imageFile) {
  await ensureLoaded()

  return new Promise((resolve, reject) => {
    const url = URL.createObjectURL(imageFile)
    const img = new Image()

    img.onload = async () => {
      try {
        const detections = await faceapi.detectAllFaces(
          img,
          new faceapi.TinyFaceDetectorOptions({ scoreThreshold: 0.4, inputSize: 416 }),
        )
        resolve(detections.length)
      } catch (err) {
        reject(err)
      } finally {
        URL.revokeObjectURL(url)
      }
    }

    img.onerror = () => {
      URL.revokeObjectURL(url)
      reject(new Error('Could not load image for face detection'))
    }

    img.src = url
  })
}
