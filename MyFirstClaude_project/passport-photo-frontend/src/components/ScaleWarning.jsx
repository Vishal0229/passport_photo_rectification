export default function ScaleWarning({ scaleFactor }) {
  if (!scaleFactor || scaleFactor < 1.5) return null

  const isRed = scaleFactor >= 2.5

  return (
    <div role="alert" className={`rounded-xl px-4 py-3 flex items-start gap-3 border ${
      isRed
        ? 'bg-red-50 border-red-200'
        : 'bg-amber-50 border-amber-200'
    }`}>
      <span aria-hidden="true" className="text-lg shrink-0 mt-0.5">{isRed ? '🔴' : '⚠️'}</span>
      <div className="min-w-0">
        <p className={`font-semibold text-sm ${isRed ? 'text-red-800' : 'text-amber-800'}`}>
          {isRed ? 'Low resolution — significant quality loss expected' : 'Low resolution — some quality loss possible'}
        </p>
        <p className={`text-xs mt-0.5 leading-relaxed ${isRed ? 'text-red-700' : 'text-amber-700'}`}>
          This photo needs {scaleFactor.toFixed(1)}× upscaling to meet the required dimensions.{' '}
          {isRed
            ? 'The corrected photo will likely appear blurry — use a higher-resolution photo if possible.'
            : 'Minor softening may occur. For best results, use a higher-resolution photo.'}
        </p>
      </div>
    </div>
  )
}
