import * as faceapi from '@vladmandic/face-api'

// Model weights are served from our own host (public/models/) — no CDN dependency.
const MODEL_URL = '/models'

let ready = false
let loadPromise = null

async function ensureLoaded() {
  if (ready) return
  if (!loadPromise) {
    loadPromise = faceapi.nets.tinyFaceDetector.loadFromUri(MODEL_URL)
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
