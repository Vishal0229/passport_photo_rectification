export default function PhotoComparison({ originalUrl, correctedUrl, country }) {
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
      <div className="mt-6 flex flex-col items-center gap-2">
        <button
          onClick={handleDownload}
          className="bg-green-600 hover:bg-green-700 active:bg-green-800 text-white font-semibold
                     py-3 px-10 rounded-xl transition-colors shadow-sm flex items-center gap-2"
        >
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                  d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
          </svg>
          Download Corrected Photo
        </button>
        <p className="text-xs text-gray-400">
          JPEG · sized to {country} passport requirements · print-ready
        </p>
      </div>
    </div>
  )
}
