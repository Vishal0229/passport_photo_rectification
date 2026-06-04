/**
 * Side-by-side before/after photo comparison with a download button.
 *
 * Renders the original and corrected photos in equal-width columns with labels.
 * The corrected photo column has a green border to draw attention to the result.
 * The download button triggers a programmatic anchor click to save the corrected
 * image as `passport_photo_<country>.jpg`.
 *
 * @param {Object} props
 * @param {string} props.originalUrl   - Object URL of the original uploaded photo.
 * @param {string} props.correctedUrl  - Object URL of the corrected JPEG returned by the backend.
 * @param {string} props.country       - Country code used to name the downloaded file.
 * @returns {JSX.Element} The before/after comparison card with download button.
 */
export default function PhotoComparison({ originalUrl, correctedUrl, country, onDownloadSheet, sheetLoading, onDownloadPdf, pdfLoading }) {
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
      <h2 className="text-base font-semibold text-gray-700 mb-5">Before &amp; After</h2>

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
            className="bg-green-600 hover:bg-green-700 active:bg-green-800 text-white font-semibold
                       py-3 px-8 rounded-xl transition-colors shadow-sm flex items-center gap-2"
          >
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                    d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
            </svg>
            Download JPEG
          </button>

          <button
            onClick={onDownloadSheet}
            disabled={sheetLoading}
            className="bg-indigo-600 hover:bg-indigo-700 active:bg-indigo-800 text-white font-semibold
                       py-3 px-8 rounded-xl transition-colors shadow-sm flex items-center gap-2
                       disabled:opacity-40 disabled:cursor-not-allowed"
          >
            {sheetLoading ? (
              <span className="inline-block w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
            ) : (
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                      d="M17 17H7a4 4 0 01-4-4V7a4 4 0 014-4h5m5 0v5m0-5l-7 7" />
              </svg>
            )}
            {sheetLoading ? 'Generating…' : 'Print Sheet (JPEG)'}
          </button>

          <button
            onClick={onDownloadPdf}
            disabled={pdfLoading}
            className="bg-rose-600 hover:bg-rose-700 active:bg-rose-800 text-white font-semibold
                       py-3 px-8 rounded-xl transition-colors shadow-sm flex items-center gap-2
                       disabled:opacity-40 disabled:cursor-not-allowed"
          >
            {pdfLoading ? (
              <span className="inline-block w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
            ) : (
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
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
