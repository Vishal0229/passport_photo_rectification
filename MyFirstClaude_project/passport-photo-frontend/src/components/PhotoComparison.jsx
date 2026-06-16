// Checks that correction always fixes (resize + background fill) — omit from the
// "still needs a retake" banner so only genuinely uncorrectable issues are shown.
const CORRECTED_BY_TOOL = new Set(['Image Dimensions', 'Aspect Ratio', 'Background Color'])

export default function PhotoComparison({ originalUrl, correctedUrl, country, analysis, onDownloadSheet, sheetLoading, onDownloadPdf, pdfLoading }) {
  const failedChecks = analysis?.checks?.filter(c => !c.passed && !CORRECTED_BY_TOOL.has(c.rule)) ?? []

  const handleDownload = () => {
    const link = document.createElement('a')
    link.href = correctedUrl
    link.download = `passport_photo_${country.toLowerCase()}.jpg`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
  }

  return (
    <div className="bg-white rounded-2xl shadow-md p-6 mt-6">
      <h2 className="text-base font-semibold text-gray-700 mb-4">Before &amp; After</h2>

      {analysis && (failedChecks.length > 0 ? (
        <div role="alert" className="mb-5 bg-amber-50 border border-amber-200 rounded-xl px-4 py-3">
          <p className="text-xs font-semibold text-amber-800 mb-2">
            <span aria-hidden="true">⚠️</span> Dimensions &amp; background adjusted — but these issues still require a retake:
          </p>
          <ul className="space-y-1">
            {failedChecks.map((check, i) => (
              <li key={i} className="flex items-start gap-2 text-xs text-amber-700">
                <span aria-hidden="true" className="shrink-0 mt-0.5">❌</span>
                <span><strong>{check.rule}:</strong> {check.message}</span>
              </li>
            ))}
          </ul>
        </div>
      ) : (
        <div className="mb-5 bg-green-50 border border-green-200 rounded-xl px-4 py-3 text-xs font-semibold text-green-800">
          <span aria-hidden="true">✅</span> All requirements met — photo is passport-ready for {country}
        </div>
      ))}

      <div className="grid grid-cols-2 gap-4">
        {/* Original */}
        <div>
          <div className="bg-gray-100 rounded-xl overflow-hidden flex items-center justify-center" style={{ minHeight: '200px' }}>
            <img
              src={originalUrl}
              alt="Original photo"
              className="max-h-72 w-full object-contain"
            />
          </div>
          <p className="text-center text-xs text-gray-400 mt-2 font-medium">Original</p>
        </div>

        {/* Corrected */}
        <div>
          <div className="bg-gray-100 rounded-xl overflow-hidden flex items-center justify-center border-2 border-green-200" style={{ minHeight: '200px' }}>
            <img
              src={correctedUrl}
              alt="Corrected passport photo"
              className="max-h-72 w-full object-contain"
            />
          </div>
          <p className="text-center text-xs text-green-600 mt-2 font-medium">Corrected for {country}</p>
        </div>
      </div>

      {/* Download */}
      <div className="mt-6 flex flex-col items-center gap-3">
        <div className="flex flex-wrap justify-center gap-3">
          <button
            onClick={handleDownload}
            aria-label="Download single photo as JPEG"
            className="bg-green-600 hover:bg-green-700 active:bg-green-800 text-white font-semibold
                       py-3 px-8 rounded-xl transition-colors shadow-sm flex items-center gap-2"
          >
            <svg aria-hidden="true" className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                    d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
            </svg>
            Download JPEG
          </button>

          <button
            onClick={onDownloadSheet}
            disabled={sheetLoading}
            aria-label="Download photo sheet as JPEG"
            aria-busy={sheetLoading}
            aria-disabled={sheetLoading}
            className="bg-indigo-600 hover:bg-indigo-700 active:bg-indigo-800 text-white font-semibold
                       py-3 px-8 rounded-xl transition-colors shadow-sm flex items-center gap-2
                       disabled:opacity-40 disabled:cursor-not-allowed"
          >
            {sheetLoading ? (
              <span className="inline-block w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
            ) : (
              <svg aria-hidden="true" className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                      d="M17 17H7a4 4 0 01-4-4V7a4 4 0 014-4h5m5 0v5m0-5l-7 7" />
              </svg>
            )}
            {sheetLoading ? 'Generating…' : 'Print Sheet (JPEG)'}
          </button>

          <button
            onClick={onDownloadPdf}
            disabled={pdfLoading}
            aria-label="Download photo sheet as PDF"
            aria-busy={pdfLoading}
            aria-disabled={pdfLoading}
            className="bg-rose-600 hover:bg-rose-700 active:bg-rose-800 text-white font-semibold
                       py-3 px-8 rounded-xl transition-colors shadow-sm flex items-center gap-2
                       disabled:opacity-40 disabled:cursor-not-allowed"
          >
            {pdfLoading ? (
              <span className="inline-block w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
            ) : (
              <svg aria-hidden="true" className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                      d="M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z" />
              </svg>
            )}
            {pdfLoading ? 'Generating…' : 'Print Sheet (PDF)'}
          </button>
        </div>

        <p className="text-xs text-gray-400 text-center">
          JPEG · sized to {country} passport requirements ·
          Print Sheet = multiple copies on a 4×6" (JPEG) or A4 (PDF) page
        </p>
      </div>
    </div>
  )
}
