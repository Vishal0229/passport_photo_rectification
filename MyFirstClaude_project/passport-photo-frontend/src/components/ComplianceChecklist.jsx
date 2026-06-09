export default function ComplianceChecklist({ analysis }) {
  const { checks, passedCount, totalCount, allPassed, certified, spec, country } = analysis
  const isPassportReady = certified ?? allPassed

  return (
    <div role="region" aria-label="Compliance results" className="bg-white rounded-2xl shadow-md p-6">
      {/* Header with official source */}
      <div className="flex items-start justify-between mb-4">
        <div>
          <h2 className="text-base font-semibold text-gray-700">Compliance Results</h2>
          {spec?.source && (
            <p className="text-xs text-gray-400 mt-1">Verified against {spec.source}</p>
          )}
        </div>
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

      {/* Overall banner with certification badge */}
      <div
        className={`rounded-xl px-4 py-3 mb-5 flex items-center gap-3 text-sm font-medium ${
          isPassportReady ? 'bg-green-50 text-green-800' : 'bg-amber-50 text-amber-800'
        }`}
      >
        <span aria-hidden="true" className="text-xl shrink-0">{isPassportReady ? '✅' : '⚠️'}</span>
        <span>
          <strong>{isPassportReady ? 'Passport-ready!' : 'Needs correction'}</strong>
          {' '}
          <span className="text-xs font-normal">
            {isPassportReady
              ? 'Your photo meets all official requirements'
              : 'Some issues found. Click "Correct Photo" to auto-fix what can be fixed.'}
          </span>
        </span>
      </div>

      {/* Why this passes — shown only when certified */}
      {isPassportReady && (
        <div className="mb-5 bg-green-50 border border-green-200 rounded-xl px-4 py-3">
          <h3 className="text-xs font-semibold text-green-700 mb-2">Why this photo is passport-ready</h3>
          <ul className="space-y-1">
            {checks.map((check, i) => (
              <li key={i} className="flex items-start gap-2 text-xs text-green-700">
                <span aria-hidden="true" className="shrink-0 mt-0.5">✓</span>
                <span>{check.message}</span>
              </li>
            ))}
          </ul>
        </div>
      )}

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
              <span aria-hidden="true" className="text-lg mt-0.5 shrink-0">{check.passed ? '✅' : '❌'}</span>
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

      {/* Spec summary with detailed requirements */}
      {spec && (
        <div className="mt-5 bg-blue-50 rounded-xl p-4 text-xs text-gray-500 border border-blue-100">
          <p className="font-semibold text-blue-700 mb-2">{country} Requirements</p>
          <p className="mb-3 text-gray-600">{spec.description}</p>
          <div className="grid grid-cols-2 gap-x-6 gap-y-2 mb-3 pb-3 border-b border-blue-200">
            <span><strong>Dimensions:</strong> {spec.widthPx}×{spec.heightPx} px ({spec.widthMm}×{spec.heightMm} mm)</span>
            <span><strong>DPI:</strong> {spec.dpi}</span>
            <span><strong>Background:</strong> {(spec.backgroundColor || '').replace('_', ' ')}</span>
            <span><strong>Face ratio:</strong> {Math.round(spec.faceRatioMin * 100)}–{Math.round(spec.faceRatioMax * 100)}%</span>
          </div>
          {spec.officialLink && (
            <a
              href={spec.officialLink}
              target="_blank"
              rel="noopener noreferrer"
              className="inline-flex items-center gap-1 text-blue-600 hover:text-blue-800 font-medium transition-colors"
            >
              <span aria-hidden="true">🔗</span> View official requirements
            </a>
          )}
        </div>
      )}

      {/* Disclaimer with verified badge */}
      <div className="mt-4 flex items-start gap-2 text-xs text-gray-400 border-t border-gray-100 pt-4">
        <span aria-hidden="true" className="shrink-0 mt-0.5">ℹ️</span>
        <p className="leading-relaxed">
          Specifications are based on official government guidelines but may change.
          Always verify your final photo against official requirements before submitting.
        </p>
      </div>
    </div>
  )
}
