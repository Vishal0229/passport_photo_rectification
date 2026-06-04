import axios from 'axios'

const BASE_URL = `${import.meta.env.VITE_API_URL ?? 'http://localhost:8080'}/api`

/**
 * Blob endpoints (correct, sheet, pdf) receive error bodies as raw Blobs.
 * This helper reads the Blob as text and parses the JSON message so callers
 * get a useful string instead of undefined.
 */
async function extractBlobErrorMessage(err) {
  if (err.response?.data instanceof Blob) {
    try {
      const text = await err.response.data.text()
      const json = JSON.parse(text)
      if (json?.message) return json.message
    } catch (_) { /* ignore parse failures */ }
  }
  return err.response?.data?.message || err.message || 'Unknown error'
}

/**
 * Submits a photo to `POST /api/analyze` and returns the compliance analysis result.
 *
 * @param {File}        photo       - The image file to analyze (JPEG, PNG, or WEBP).
 * @param {string}      country     - Country code (e.g. `"US"`) or `"Custom"`.
 * @param {Object|null} customSpec  - When `country="Custom"`, the full spec object to send as JSON.
 *                                    Pass `null` for built-in countries.
 * @returns {Promise<Object>} The `AnalysisResult` JSON object from the backend.
 * @throws {AxiosError} On network errors or HTTP 4xx/5xx responses.
 */
export async function analyzePhoto(photo, country, customSpec) {
  const form = new FormData()
  form.append('photo', photo)
  form.append('country', country)
  if (customSpec) form.append('customSpec', JSON.stringify(customSpec))
  const res = await axios.post(`${BASE_URL}/analyze`, form, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 30000,
  })
  return res.data
}

/**
 * Submits a photo to `POST /api/pdf` and returns a print-ready A4 PDF as a Blob.
 * Photos are tiled at their physical mm dimensions (72 pt/inch) — correct size on any A4 printer.
 *
 * @param {File}        photo       - The image file to process (JPEG, PNG, or WEBP).
 * @param {string}      country     - Country code (e.g. `"US"`) or `"Custom"`.
 * @param {Object|null} customSpec  - When `country="Custom"`, the full spec object to send as JSON.
 * @returns {Promise<Blob>} The A4 PDF as a Blob.
 * @throws {AxiosError} On network errors or HTTP 4xx/5xx responses.
 */
export async function downloadPhotoPdf(photo, country, customSpec) {
  const form = new FormData()
  form.append('photo', photo)
  form.append('country', country)
  if (customSpec) form.append('customSpec', JSON.stringify(customSpec))
  try {
    const res = await axios.post(`${BASE_URL}/pdf`, form, {
      headers: { 'Content-Type': 'multipart/form-data' },
      responseType: 'blob',
      timeout: 60000,
    })
    return res.data
  } catch (err) {
    err.humanMessage = await extractBlobErrorMessage(err)
    throw err
  }
}

/**
 * Submits a photo to `POST /api/sheet` and returns a print-ready 4×6" sheet as a Blob.
 * The sheet tiles multiple copies of the corrected photo with a 15 px gutter.
 *
 * @param {File}        photo       - The image file to process (JPEG, PNG, or WEBP).
 * @param {string}      country     - Country code (e.g. `"US"`) or `"Custom"`.
 * @param {Object|null} customSpec  - When `country="Custom"`, the full spec object to send as JSON.
 * @returns {Promise<Blob>} The print sheet as a JPEG Blob.
 * @throws {AxiosError} On network errors or HTTP 4xx/5xx responses.
 */
export async function downloadPhotoSheet(photo, country, customSpec) {
  const form = new FormData()
  form.append('photo', photo)
  form.append('country', country)
  if (customSpec) form.append('customSpec', JSON.stringify(customSpec))
  try {
    const res = await axios.post(`${BASE_URL}/sheet`, form, {
      headers: { 'Content-Type': 'multipart/form-data' },
      responseType: 'blob',
      timeout: 60000,
    })
    return res.data
  } catch (err) {
    err.humanMessage = await extractBlobErrorMessage(err)
    throw err
  }
}

/**
 * Submits a photo to `POST /api/correct` and returns the corrected image as a Blob.
 *
 * @param {File}        photo       - The image file to correct (JPEG, PNG, or WEBP).
 * @param {string}      country     - Country code (e.g. `"UK"`) or `"Custom"`.
 * @param {Object|null} customSpec  - When `country="Custom"`, the full spec object to send as JSON.
 *                                    Pass `null` for built-in countries.
 * @returns {Promise<Blob>} The corrected JPEG image as a binary Blob.
 * @throws {AxiosError} On network errors or HTTP 4xx/5xx responses.
 */
export async function correctPhoto(photo, country, customSpec) {
  const form = new FormData()
  form.append('photo', photo)
  form.append('country', country)
  if (customSpec) form.append('customSpec', JSON.stringify(customSpec))
  try {
    const res = await axios.post(`${BASE_URL}/correct`, form, {
      headers: { 'Content-Type': 'multipart/form-data' },
      responseType: 'blob',
      timeout: 60000,
    })
    return res.data
  } catch (err) {
    err.humanMessage = await extractBlobErrorMessage(err)
    throw err
  }
}
