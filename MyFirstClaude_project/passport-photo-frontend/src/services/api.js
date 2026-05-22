import axios from 'axios'

/** Base URL for all backend API requests. Update for production deployment. */
const BASE_URL = 'http://localhost:8080/api'

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
  const res = await axios.post(`${BASE_URL}/correct`, form, {
    headers: { 'Content-Type': 'multipart/form-data' },
    responseType: 'blob',
    timeout: 60000,
  })
  return res.data
}
