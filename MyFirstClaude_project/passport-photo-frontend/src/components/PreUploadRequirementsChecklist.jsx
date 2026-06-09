/**
 * Pre-upload requirements card for a selected country.
 *
 * Displays the key photo requirements for the selected country immediately
 * after the user picks a country — before they upload a photo. Intended to
 * help users prepare their photo against official standards up front.
 *
 * Reuses the visual language of {@link ComplianceChecklist} (card shell,
 * header, source attribution, official-link block) without the pass/fail
 * check logic.
 *
 * Renders nothing if {@code spec} is missing or {@code requirementsBulletPoints}
 * is absent or empty — this covers the "My Country not on list" (Custom) path
 * and any future entries lacking the field.
 *
 * @param {Object}          props
 * @param {Object}          props.spec                          - The {@code CountrySpec} for the selected country (from {@code /api/countries}).
 * @param {string}          props.spec.name                     - Display name of the country.
 * @param {string}          props.spec.source                   - Authority that defined the requirements.
 * @param {string[]}        props.spec.requirementsBulletPoints - Plain-language requirement bullets.
 * @param {string}          props.spec.description              - Human-readable summary of the spec.
 * @param {string}          [props.spec.officialLink]           - Optional URL to the official requirements page.
 * @returns {JSX.Element|null} The requirements card, or null when bullet points are unavailable.
 */
export default function PreUploadRequirementsChecklist({ spec }) {
  if (!spec?.requirementsBulletPoints?.length) return null

  return (
    <div className="bg-white rounded-2xl shadow-md p-6">
      {/* Header */}
      <div className="mb-4">
        <h2 className="text-base font-semibold text-gray-700">{spec.name} Photo Requirements</h2>
        {spec.source && (
          <p className="text-xs text-gray-400 mt-1">Verified against {spec.source}</p>
        )}
      </div>

      {/* Bullet points */}
      <ul className="space-y-1 mb-4">
        {spec.requirementsBulletPoints.map((bullet, i) => (
          <li key={i} className="flex items-start gap-2 text-xs text-green-700">
            <span className="shrink-0 mt-0.5">✓</span>
            <span>{bullet}</span>
          </li>
        ))}
      </ul>

      {/* Description */}
      {spec.description && (
        <p className="text-xs text-gray-500 mb-3 leading-relaxed">{spec.description}</p>
      )}

      {/* Official link */}
      {spec.officialLink && (
        <a
          href={spec.officialLink}
          target="_blank"
          rel="noopener noreferrer"
          className="inline-flex items-center gap-1 text-blue-600 hover:text-blue-800 font-medium text-xs transition-colors"
        >
          🔗 View official requirements
        </a>
      )}
    </div>
  )
}
