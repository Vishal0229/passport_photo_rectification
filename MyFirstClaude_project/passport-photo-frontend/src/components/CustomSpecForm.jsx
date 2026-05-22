const BACKGROUNDS = [
  { value: 'WHITE',      label: 'White',      hex: '#FFFFFF' },
  { value: 'LIGHT_GREY', label: 'Light Grey', hex: '#F0F0F0' },
]

const mm2px = (mm) => Math.round(mm * 300 / 25.4)

/**
 * Form for entering a custom passport photo specification.
 *
 * Shown when the user selects "Custom / Other" in {@link CountrySelector}.
 * Accepts width and height in millimetres and automatically converts them to
 * pixels at 300 DPI using the formula `round(mm * 300 / 25.4)`. Background
 * colour selection also auto-populates the corresponding hex value.
 *
 * A read-only pixel-dimension preview is displayed once both width and height
 * are non-zero.
 *
 * @param {Object}   props
 * @param {Object}   props.spec     - The current spec state object (matches the `CountrySpec` shape).
 * @param {string}   props.spec.name - Country/region label.
 * @param {number}   props.spec.widthMm - Photo width in mm.
 * @param {number}   props.spec.heightMm - Photo height in mm.
 * @param {number}   props.spec.widthPx - Derived photo width in px (auto-calculated).
 * @param {number}   props.spec.heightPx - Derived photo height in px (auto-calculated).
 * @param {string}   props.spec.backgroundColor - `"WHITE"` or `"LIGHT_GREY"`.
 * @param {string}   props.spec.backgroundColorHex - Hex colour string (auto-populated from `backgroundColor`).
 * @param {number}   props.spec.faceRatioMin - Minimum face height ratio (0–1).
 * @param {number}   props.spec.faceRatioMax - Maximum face height ratio (0–1).
 * @param {Function} props.onChange - Called with the updated spec object on any field change.
 * @returns {JSX.Element} The custom spec input form card.
 */
export default function CustomSpecForm({ spec, onChange }) {
  const set = (field, raw) => {
    const next = { ...spec }

    if (field === 'widthMm') {
      next.widthMm  = raw
      next.widthPx  = mm2px(raw)
    } else if (field === 'heightMm') {
      next.heightMm = raw
      next.heightPx = mm2px(raw)
    } else if (field === 'backgroundColor') {
      next.backgroundColor    = raw
      next.backgroundColorHex = BACKGROUNDS.find(b => b.value === raw)?.hex ?? '#FFFFFF'
    } else if (field === 'faceRatioMinPct') {
      next.faceRatioMin = raw / 100
    } else if (field === 'faceRatioMaxPct') {
      next.faceRatioMax = raw / 100
    } else {
      next[field] = raw
    }

    next.dpi = 300
    next.description = `${next.name || 'Custom'}: ${next.widthMm}×${next.heightMm} mm, ${(next.backgroundColor || 'WHITE').replace('_', ' ').toLowerCase()} background`
    onChange(next)
  }

  const faceMinPct = spec.faceRatioMin != null ? Math.round(spec.faceRatioMin * 100) : ''
  const faceMaxPct = spec.faceRatioMax != null ? Math.round(spec.faceRatioMax * 100) : ''

  return (
    <div className="bg-amber-50 border border-amber-200 rounded-xl p-4 space-y-3">
      <p className="text-xs font-semibold text-amber-800">Enter your country's passport photo requirements</p>

      {/* Region name */}
      <div>
        <label className="text-xs text-gray-600 font-medium block mb-1">Country / Region</label>
        <input
          type="text"
          value={spec.name || ''}
          onChange={(e) => set('name', e.target.value)}
          placeholder="e.g. UAE, Saudi Arabia…"
          className="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm text-gray-700 bg-white
                     focus:outline-none focus:ring-2 focus:ring-amber-400"
        />
      </div>

      {/* Dimensions */}
      <div className="grid grid-cols-2 gap-3">
        <div>
          <label className="text-xs text-gray-600 font-medium block mb-1">Width (mm)</label>
          <input
            type="number"
            min="15" max="150"
            value={spec.widthMm || ''}
            onChange={(e) => set('widthMm', parseInt(e.target.value, 10) || 0)}
            className="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm text-gray-700 bg-white
                       focus:outline-none focus:ring-2 focus:ring-amber-400"
          />
        </div>
        <div>
          <label className="text-xs text-gray-600 font-medium block mb-1">Height (mm)</label>
          <input
            type="number"
            min="15" max="200"
            value={spec.heightMm || ''}
            onChange={(e) => set('heightMm', parseInt(e.target.value, 10) || 0)}
            className="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm text-gray-700 bg-white
                       focus:outline-none focus:ring-2 focus:ring-amber-400"
          />
        </div>
      </div>

      {/* Background */}
      <div>
        <label className="text-xs text-gray-600 font-medium block mb-1">Background</label>
        <select
          value={spec.backgroundColor || 'WHITE'}
          onChange={(e) => set('backgroundColor', e.target.value)}
          className="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm text-gray-700 bg-white
                     focus:outline-none focus:ring-2 focus:ring-amber-400"
        >
          {BACKGROUNDS.map(b => (
            <option key={b.value} value={b.value}>{b.label}</option>
          ))}
        </select>
      </div>

      {/* Face ratio */}
      <div className="grid grid-cols-2 gap-3">
        <div>
          <label className="text-xs text-gray-600 font-medium block mb-1">Face min % of height</label>
          <input
            type="number"
            min="20" max="90"
            value={faceMinPct}
            onChange={(e) => set('faceRatioMinPct', parseInt(e.target.value, 10) || 0)}
            placeholder="50"
            className="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm text-gray-700 bg-white
                       focus:outline-none focus:ring-2 focus:ring-amber-400"
          />
        </div>
        <div>
          <label className="text-xs text-gray-600 font-medium block mb-1">Face max % of height</label>
          <input
            type="number"
            min="20" max="95"
            value={faceMaxPct}
            onChange={(e) => set('faceRatioMaxPct', parseInt(e.target.value, 10) || 0)}
            placeholder="75"
            className="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm text-gray-700 bg-white
                       focus:outline-none focus:ring-2 focus:ring-amber-400"
          />
        </div>
      </div>

      {/* Pixel preview */}
      {spec.widthMm > 0 && spec.heightMm > 0 && (
        <p className="text-xs text-amber-700 bg-amber-100 rounded-lg px-3 py-2">
          {spec.widthPx} × {spec.heightPx} px at 300 DPI
        </p>
      )}
    </div>
  )
}
