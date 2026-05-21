export default function ComplianceChecklist({ analysis }) {
  const { checks, passedCount, totalCount, allPassed, spec, country } = analysis

  return (
    <div className="bg-white rounded-2xl shadow-md p-6">
      {/* Header */}
      <div className="flex items-center justify-between mb-5">
        <h2 className="text-base font-semibold text-gray-700">Compliance Results</h2>
        <span
          className={`px-4 py-1.5 rounded-full text-xs font-bold tracking-wide ${
            allPassed
              ? 'bg-green-100 text-green-700'
              : passedCount >= totalCount / 2
              ? 'bg-amber-100 text-amber-700'
              : 'bg-red-100 text-red-700'
          }`}
        >
          {passedCount} / {totalCount} Passed
        </span>
      </div>

      {/* Overall banner */}
      <div
        className={`rounded-xl px-4 py-3 mb-5 flex items-center gap-3 text-sm font-medium ${
          allPassed ? 'bg-green-50 text-green-800' : 'bg-amber-50 text-amber-800'
        }`}
      >
        <span className="text-xl">{allPassed ? '✅' : '⚠️'}</span>
        {allPassed
          ? 'Photo meets all requirements — ready to correct and download!'
          : 'Some issues found. Click "Correct Photo" to auto-fix what can be fixed.'}
      </div>

      {/* Individual checks */}
      <div className="space-y-2.5">
        {checks.map((check, i) => (
          <div
            key={i}
            className={`rounded-xl border px-4 py-3.5 ${
              check.passed
                ? 'bg-green-50 border-green-200'
                : 'bg-red-50 border-red-200'
            }`}
          >
            <div className="flex items-start gap-3">
              <span className="text-lg mt-0.5 shrink-0">{check.passed ? '✅' : '❌'}</span>
              <div className="flex-1 min-w-0">
                <p className={`font-semibold text-sm ${check.passed ? 'text-green-800' : 'text-red-800'}`}>
                  {check.rule}
                </p>
                <p className={`text-xs mt-0.5 leading-relaxed ${check.passed ? 'text-green-700' : 'text-red-700'}`}>
                  {check.message}
                </p>
                {(check.expected || check.actual) && (
                  <div className="mt-2 flex flex-wrap gap-x-4 gap-y-1 text-xs text-gray-400">
                    {check.expected && <span>Expected: <span className="text-gray-500">{check.expected}</span></span>}
                    {check.actual   && <span>Actual: <span className="text-gray-500">{check.actual}</span></span>}
                  </div>
                )}
              </div>
            </div>
          </div>
        ))}
      </div>

      {/* Spec summary */}
      {spec && (
        <div className="mt-5 bg-gray-50 rounded-xl p-4 text-xs text-gray-500">
          <p className="font-semibold text-gray-600 mb-2">{country} requirements</p>
          <p className="mb-2 text-gray-500">{spec.description}</p>
          <div className="grid grid-cols-2 gap-x-6 gap-y-1">
            <span>Size: {spec.widthPx}×{spec.heightPx} px</span>
            <span>Print: {spec.widthMm}×{spec.heightMm} mm</span>
            <span>DPI: {spec.dpi}</span>
            <span>Background: {spec.backgroundColor.replace('_', ' ')}</span>
            <span className="col-span-2">
              Face ratio: {Math.round(spec.faceRatioMin * 100)}–{Math.round(spec.faceRatioMax * 100)}% of height
            </span>
          </div>
        </div>
      )}

      {/* Disclaimer */}
      <div className="mt-4 flex items-start gap-2 text-xs text-gray-400 border-t border-gray-100 pt-4">
        <span className="shrink-0 mt-0.5">ℹ️</span>
        <p className="leading-relaxed">
          Specifications are based on official government guidelines but may change.
          Always verify your final photo against your country's passport authority before submitting.{' '}
          {officialLinks[country] && (
            <a
              href={officialLinks[country]}
              target="_blank"
              rel="noopener noreferrer"
              className="underline hover:text-gray-600 transition-colors"
            >
              Check official requirements →
            </a>
          )}
        </p>
      </div>
    </div>
  )
}

const officialLinks = {
  US:        'https://travel.state.gov/content/travel/en/us-visas/visa-information-resources/photos.html',
  UK:        'https://www.gov.uk/photos-for-passports',
  India:     'https://www.passportindia.gov.in/',
  Canada:    'https://www.canada.ca/en/immigration-refugees-citizenship/services/canadian-passports/photos.html',
  Australia: 'https://www.passports.gov.au/getting-passport-how-it-works/passport-photo-requirements',
  UAE:       'https://www.icp.gov.ae',
  Schengen:  'https://home-affairs.ec.europa.eu/policies/schengen-borders-and-visa_en',
}
