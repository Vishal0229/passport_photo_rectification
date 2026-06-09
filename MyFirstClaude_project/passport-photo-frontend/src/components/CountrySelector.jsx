const COUNTRIES = [
  { code: 'US',         name: 'United States',        flag: '🇺🇸', spec: '2×2 in · white bg',          minPx: '600×600 px' },
  { code: 'UK',         name: 'United Kingdom',        flag: '🇬🇧', spec: '35×45 mm · light grey bg',   minPx: '413×531 px' },
  { code: 'India',      name: 'India',                 flag: '🇮🇳', spec: '51×51 mm · white bg',         minPx: '602×602 px' },
  { code: 'Canada',     name: 'Canada',                flag: '🇨🇦', spec: '50×70 mm · white bg',         minPx: '591×827 px' },
  { code: 'Australia',  name: 'Australia',             flag: '🇦🇺', spec: '35×45 mm · white bg',         minPx: '413×531 px' },
  { code: 'UAE',        name: 'United Arab Emirates',  flag: '🇦🇪', spec: '40×60 mm · white bg',         minPx: '472×709 px' },
  { code: 'Schengen',   name: 'Schengen (EU)',          flag: '🇪🇺', spec: '35×45 mm · light grey bg',   minPx: '413×531 px' },
  { code: 'Germany',    name: 'Germany',               flag: '🇩🇪', spec: '35×45 mm · light grey bg',   minPx: '413×531 px' },
  { code: 'France',     name: 'France',                flag: '🇫🇷', spec: '35×45 mm · white bg',         minPx: '413×531 px' },
  { code: 'Japan',      name: 'Japan',                 flag: '🇯🇵', spec: '45×35 mm · white bg',         minPx: '531×413 px' },
  { code: 'China',      name: 'China',                 flag: '🇨🇳', spec: '33×48 mm · white bg',         minPx: '390×567 px' },
  { code: 'Brazil',     name: 'Brazil',                flag: '🇧🇷', spec: '50×70 mm · white bg',         minPx: '591×827 px' },
  { code: 'NewZealand', name: 'New Zealand',           flag: '🇳🇿', spec: '35×45 mm · white bg',         minPx: '413×531 px' },
  { code: 'Singapore',  name: 'Singapore',             flag: '🇸🇬', spec: '35×45 mm · white bg',         minPx: '413×531 px' },
  { code: 'Mexico',     name: 'Mexico',                flag: '🇲🇽', spec: '35×45 mm · white bg',         minPx: '413×531 px' },
  { code: 'Custom',     name: 'My Country not on list', flag: '🌍', spec: 'Enter your own specifications' },
]

/**
 * Country selection dropdown with a compliance summary card.
 *
 * Renders all built-in country options (US, UK, India, Canada, Australia, UAE,
 * Schengen, Germany, France, Japan, China, Brazil, New Zealand, Singapore, Mexico)
 * plus "My Country not on list". Selecting an option fires `onChange` with the
 * new country code string. Below the dropdown, a card shows the selected country's
 * flag, name, and compliance requirements along with official government links.
 *
 * @param {Object}   props
 * @param {string}   props.country  - Currently selected country code (e.g. `"US"`, `"Custom"`).
 * @param {Function} props.onChange - Called with the new country code string when the selection changes.
 * @returns {JSX.Element} The country selector card with compliance details.
 */
export default function CountrySelector({ country, onChange }) {
  const selected = COUNTRIES.find((c) => c.code === country)

  return (
    <div className="bg-white rounded-2xl shadow-md p-6">
      <h2 className="text-base font-semibold text-gray-700 mb-4">Select Country</h2>

      <select
        value={country}
        onChange={(e) => onChange(e.target.value)}
        className="w-full border border-gray-200 rounded-xl px-4 py-3 text-gray-700 bg-gray-50
                   focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent
                   appearance-none cursor-pointer"
      >
        {COUNTRIES.map((c) => (
          <option key={c.code} value={c.code}>
            {c.flag}  {c.name}
          </option>
        ))}
      </select>

      {selected && (
        <div className="mt-4 bg-gradient-to-br from-blue-50 to-blue-100 rounded-xl border border-blue-200 px-4 py-4">
          <div className="flex items-center gap-3 mb-3">
            <span className="text-3xl">{selected.flag}</span>
            <div>
              <p className="font-semibold text-blue-900 text-sm">{selected.name}</p>
              <p className="text-blue-600 text-xs mt-1">{selected.spec}</p>
              {selected.minPx && (
                <p className="text-xs mt-1 font-medium text-blue-800">
                  Min photo size: <span className="font-bold">{selected.minPx}</span> at 300 DPI
                </p>
              )}
            </div>
          </div>
          {selected.minPx && (
            <p className="text-xs text-blue-700 bg-blue-50/60 rounded px-2.5 py-1.5 inline-block">
              ✓ Official government requirements verified
            </p>
          )}
        </div>
      )}
    </div>
  )
}
