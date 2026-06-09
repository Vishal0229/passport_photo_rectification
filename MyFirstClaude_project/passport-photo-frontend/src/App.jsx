import { useState, useEffect, useRef } from 'react'
import UploadZone from './components/UploadZone'
import CountrySelector from './components/CountrySelector'
import CustomSpecForm from './components/CustomSpecForm'
import ScaleWarning from './components/ScaleWarning'
import ComplianceChecklist from './components/ComplianceChecklist'
import PreUploadRequirementsChecklist from './components/PreUploadRequirementsChecklist'
import PhotoComparison from './components/PhotoComparison'
import { analyzePhoto, correctPhoto, downloadPhotoSheet, downloadPhotoPdf } from './services/api'
import { logger } from './services/logger'

const DEFAULT_CUSTOM_SPEC = {
  name: '',
  widthMm: 0,
  heightMm: 0,
  widthPx: 0,
  heightPx: 0,
  dpi: 300,
  backgroundColor: 'WHITE',
  backgroundColorHex: '#FFFFFF',
  faceRatioMin: 0.50,
  faceRatioMax: 0.75,
  description: '',
}

/**
 * Inline animated spinner used inside loading buttons.
 *
 * @returns {JSX.Element} A small circular CSS animation element.
 */
function Spinner() {
  return (
    <span className="inline-block w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
  )
}

/**
 * Root application component for the Passport Photo Corrector.
 *
 * Orchestrates the full analysis and correction flow:
 * 1. User uploads a photo via {@link UploadZone} (frontend face detection runs here).
 * 2. User selects a country via {@link CountrySelector}, or fills in a custom spec
 *    via {@link CustomSpecForm} when "My Country not on list" is chosen.
 * 3. Clicking "Analyze Photo" calls `POST /api/analyze` and displays results in
 *    {@link ComplianceChecklist}.
 * 4. Clicking "Correct Photo" calls `POST /api/correct` and displays before/after
 *    images in {@link PhotoComparison}.
 *
 * Also listens for the browser's `beforeinstallprompt` event and shows an
 * "Add to Home Screen" banner when the PWA install criteria are met.
 *
 * @returns {JSX.Element} The full single-page application layout.
 */
export default function App() {
  const [photo, setPhoto] = useState(null)
  const [photoUrl, setPhotoUrl] = useState(null)
  const [photoDimensions, setPhotoDimensions] = useState(null)
  const [countrySpecs, setCountrySpecs] = useState(null)
  const [country, setCountry] = useState('US')
  const [customSpec, setCustomSpec] = useState(DEFAULT_CUSTOM_SPEC)
  const [analysis, setAnalysis] = useState(null)
  const [correctedUrl, setCorrectedUrl] = useState(null)
  const [loading, setLoading] = useState(false)
  const [correcting, setCorrecting] = useState(false)
  const [downloadingSheet, setDownloadingSheet] = useState(false)
  const [downloadingPdf, setDownloadingPdf] = useState(false)
  const [error, setError] = useState(null)
  const [installPrompt, setInstallPrompt] = useState(null)
  const [uploadKey, setUploadKey] = useState(0)
  // Incremented on every new analyze/correct call; stale responses check against this before updating state.
  const requestIdRef = useRef(0)

  useEffect(() => {
    const handler = (e) => { e.preventDefault(); setInstallPrompt(e) }
    window.addEventListener('beforeinstallprompt', handler)
    return () => window.removeEventListener('beforeinstallprompt', handler)
  }, [])

  useEffect(() => {
    fetch(`${import.meta.env.VITE_API_URL ?? 'http://localhost:8080'}/api/countries`)
      .then(r => r.json()).then(setCountrySpecs).catch(() => {})
  }, [])

  useEffect(() => {
    if (!photoUrl) { setPhotoDimensions(null); return }
    const img = new window.Image()
    img.onload = () => setPhotoDimensions({ w: img.naturalWidth, h: img.naturalHeight })
    img.src = photoUrl
  }, [photoUrl])

  const handleNewFileSelected = () => {
    if (photoUrl) URL.revokeObjectURL(photoUrl)
    if (correctedUrl) URL.revokeObjectURL(correctedUrl)
    setPhoto(null)
    setPhotoUrl(null)
    setAnalysis(null)
    setCorrectedUrl(null)
    setError(null)
  }

  const handlePhotoSelect = (file) => {
    setPhoto(file)
    setPhotoUrl(URL.createObjectURL(file))
  }

  const handleCountryChange = (c) => {
    if (correctedUrl) URL.revokeObjectURL(correctedUrl)
    setCountry(c)
    setAnalysis(null)
    setCorrectedUrl(null)
    setError(null)
  }

  const handleAnalyze = async () => {
    if (!photo) return
    const id = ++requestIdRef.current
    setLoading(true)
    setError(null)
    setAnalysis(null)
    if (correctedUrl) { URL.revokeObjectURL(correctedUrl); setCorrectedUrl(null) }
    try {
      const result = await analyzePhoto(photo, country, country === 'Custom' ? customSpec : null)
      if (id !== requestIdRef.current) return
      setAnalysis(result)
    } catch (err) {
      if (id !== requestIdRef.current) return
      const msg = err.response?.data?.message || err.message || 'Unknown error'
      logger.error('POST /api/analyze failed', { country, message: msg })
      setError(
        msg.includes('Network Error') || msg.includes('ECONNREFUSED')
          ? 'Cannot reach the backend. Make sure Spring Boot is running on port 8080.'
          : `Analysis failed: ${msg}`
      )
    } finally {
      if (id === requestIdRef.current) setLoading(false)
    }
  }

  const handleCorrect = async () => {
    if (!photo) return
    const id = ++requestIdRef.current
    setCorrecting(true)
    setError(null)
    try {
      const blob = await correctPhoto(photo, country, country === 'Custom' ? customSpec : null)
      if (id !== requestIdRef.current) return
      if (correctedUrl) URL.revokeObjectURL(correctedUrl)
      setCorrectedUrl(URL.createObjectURL(blob))
    } catch (err) {
      if (id !== requestIdRef.current) return
      const msg = err.humanMessage || err.response?.data?.message || err.message || 'Unknown error'
      logger.error('POST /api/correct failed', { country, message: msg })
      setError(`Correction failed: ${msg}`)
    } finally {
      if (id === requestIdRef.current) setCorrecting(false)
    }
  }

  const handleDownloadSheet = async () => {
    if (!photo) return
    setDownloadingSheet(true)
    setError(null)
    try {
      const blob = await downloadPhotoSheet(photo, country, country === 'Custom' ? customSpec : null)
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = `passport_sheet_${country.toLowerCase()}.jpg`
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      URL.revokeObjectURL(url)
    } catch (err) {
      const msg = err.humanMessage || err.response?.data?.message || err.message || 'Unknown error'
      logger.error('POST /api/sheet failed', { country, message: msg })
      setError(`Sheet download failed: ${msg}`)
    } finally {
      setDownloadingSheet(false)
    }
  }

  const handleDownloadPdf = async () => {
    if (!photo) return
    setDownloadingPdf(true)
    setError(null)
    try {
      const blob = await downloadPhotoPdf(photo, country, country === 'Custom' ? customSpec : null)
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = `passport_sheet_${country.toLowerCase()}.pdf`
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      URL.revokeObjectURL(url)
    } catch (err) {
      const msg = err.humanMessage || err.response?.data?.message || err.message || 'Unknown error'
      logger.error('POST /api/pdf failed', { country, message: msg })
      setError(`PDF download failed: ${msg}`)
    } finally {
      setDownloadingPdf(false)
    }
  }

  const handleReset = () => {
    requestIdRef.current++   // invalidate any in-flight analyze/correct response
    if (photoUrl) URL.revokeObjectURL(photoUrl)
    if (correctedUrl) URL.revokeObjectURL(correctedUrl)
    setPhoto(null)
    setPhotoUrl(null)
    setAnalysis(null)
    setCorrectedUrl(null)
    setError(null)
    setLoading(false)
    setCorrecting(false)
    setUploadKey((k) => k + 1)
  }

  const handleInstall = async () => {
    if (!installPrompt) return
    installPrompt.prompt()
    const { outcome } = await installPrompt.userChoice
    if (outcome === 'accepted') setInstallPrompt(null)
  }

  const targetSpec = country === 'Custom' ? customSpec : countrySpecs?.[country]
  const scaleFactor = (photoDimensions && targetSpec?.widthPx && targetSpec?.heightPx)
    ? Math.max(targetSpec.widthPx / photoDimensions.w, targetSpec.heightPx / photoDimensions.h)
    : 1

  return (
    <div className="min-h-screen bg-gradient-to-b from-slate-50 to-white">
      {/* Install banner */}
      {installPrompt && (
        <div className="bg-blue-600 text-white text-sm text-center py-2 px-4">
          Install this app for quick access on your device!{' '}
          <button onClick={handleInstall} className="underline font-semibold ml-1">
            Add to Home Screen
          </button>
          <button onClick={() => setInstallPrompt(null)} className="ml-4 opacity-60 hover:opacity-100">✕</button>
        </div>
      )}

      {/* Header */}
      <header className="bg-blue-700 text-white py-5 shadow-lg">
        <div className="container mx-auto px-4 max-w-4xl flex items-center gap-4">
          <div className="w-10 h-10 bg-white/20 rounded-xl flex items-center justify-center text-xl shrink-0">
            🛂
          </div>
          <div>
            <h1 className="text-xl font-bold leading-tight">Passport Photo Corrector</h1>
            <p className="text-blue-200 text-xs mt-0.5">
              Verify &amp; auto-correct photos to meet official passport photo standards for 15+ countries
            </p>
          </div>
        </div>
      </header>

      {/* Main */}
      <main className="container mx-auto px-4 py-8 max-w-4xl">

        {/* Upload + Country row */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-5 mb-5">
          <div className="flex flex-col gap-4">
            <UploadZone key={uploadKey} onPhotoSelect={handlePhotoSelect} onNewFileSelected={handleNewFileSelected} photoUrl={photoUrl} />
            <ScaleWarning scaleFactor={scaleFactor} />
          </div>

          <div className="flex flex-col gap-4">
            <CountrySelector country={country} onChange={handleCountryChange} />

            {country !== 'Custom' && countrySpecs?.[country] && (
              <PreUploadRequirementsChecklist spec={countrySpecs[country]} />
            )}

            {country === 'Custom' && (
              <CustomSpecForm spec={customSpec} onChange={setCustomSpec} />
            )}

            <button
              onClick={handleAnalyze}
              disabled={!photo || loading || (country === 'Custom' && (!customSpec.widthMm || !customSpec.heightMm))}
              className="w-full bg-blue-600 hover:bg-blue-700 active:bg-blue-800 text-white
                         font-semibold py-3.5 rounded-xl transition-colors
                         disabled:opacity-40 disabled:cursor-not-allowed shadow-sm
                         flex items-center justify-center gap-2"
            >
              {loading ? <><Spinner /> Analyzing…</> : '🔍 Analyze Photo'}
            </button>
          </div>
        </div>

        {/* Resolution quality warning — always visible before upload */}
        {targetSpec?.widthPx && targetSpec?.heightPx && (
          <div className="mb-5 bg-amber-50 border border-amber-200 rounded-xl px-5 py-4 flex items-start gap-3">
            <span className="text-xl shrink-0 mt-0.5">⚠️</span>
            <div className="text-sm">
              <p className="font-semibold text-amber-900 mb-1">
                Upload a photo of at least 400×400 px for best results
              </p>
              <p className="text-amber-800 leading-relaxed">
                Photos below 400×400 px will be upscaled to meet passport dimensions, but upscaled images
                may still be rejected — passport authorities can detect digitally enhanced or AI-corrected photos
                even when pixel dimensions appear correct.{' '}
                <span className="font-semibold">Always use a high-resolution original for official submissions.</span>
              </p>
            </div>
          </div>
        )}

        {/* Error */}
        {error && (
          <div className="bg-red-50 border border-red-200 text-red-700 px-5 py-4 rounded-xl mb-5 flex items-start gap-3">
            <span className="text-lg shrink-0">⚠️</span>
            <p className="text-sm">{error}</p>
          </div>
        )}

        {/* Compliance checklist */}
        {analysis && (
          <>
            <ComplianceChecklist analysis={analysis} />

            <div className="flex justify-center mt-5">
              <button
                onClick={handleCorrect}
                disabled={correcting}
                className="bg-indigo-600 hover:bg-indigo-700 active:bg-indigo-800 text-white
                           font-semibold py-3 px-10 rounded-xl transition-colors shadow-sm
                           flex items-center gap-2 disabled:opacity-40"
              >
                {correcting ? <><Spinner /> Correcting…</> : '🔧 Correct Photo'}
              </button>
            </div>
          </>
        )}

        {/* Before/after comparison */}
        {correctedUrl && (
          <PhotoComparison
            originalUrl={photoUrl}
            correctedUrl={correctedUrl}
            country={country}
            onDownloadSheet={handleDownloadSheet}
            sheetLoading={downloadingSheet}
            onDownloadPdf={handleDownloadPdf}
            pdfLoading={downloadingPdf}
          />
        )}

        {/* Reset */}
        {photo && (
          <div className="flex justify-center mt-8">
            <button
              onClick={handleReset}
              className="text-gray-400 hover:text-gray-600 text-sm underline transition-colors"
            >
              Start over with a new photo
            </button>
          </div>
        )}
      </main>

      <footer className="mt-16 py-6 border-t border-gray-100">
        <p className="text-center text-xs text-gray-300">
          Passport Photo Corrector · Spring Boot + React PWA
        </p>
      </footer>
    </div>
  )
}
