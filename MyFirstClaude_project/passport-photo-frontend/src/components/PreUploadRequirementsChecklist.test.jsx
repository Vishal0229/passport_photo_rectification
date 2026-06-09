import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import PreUploadRequirementsChecklist from './PreUploadRequirementsChecklist'

const baseSpec = {
  name: 'United States',
  description: '2×2 inch photo with white background',
  officialLink: 'https://travel.state.gov/content/travel/en/passports/requirements/photos.html',
  source: 'U.S. Department of State',
  requirementsBulletPoints: ['Neutral expression', 'No glasses'],
}

describe('PreUploadRequirementsChecklist', () => {
  it('renders all bullet points from requirementsBulletPoints', () => {
    render(<PreUploadRequirementsChecklist spec={baseSpec} />)
    expect(screen.getByText('Neutral expression')).toBeInTheDocument()
    expect(screen.getByText('No glasses')).toBeInTheDocument()
  })

  it('renders the spec description text', () => {
    render(<PreUploadRequirementsChecklist spec={baseSpec} />)
    expect(screen.getByText('2×2 inch photo with white background')).toBeInTheDocument()
  })

  it('renders an official-requirements link when officialLink is present', () => {
    render(<PreUploadRequirementsChecklist spec={baseSpec} />)
    const link = screen.getByRole('link', { name: /official requirements/i })
    expect(link).toBeInTheDocument()
    expect(link).toHaveAttribute(
      'href',
      'https://travel.state.gov/content/travel/en/passports/requirements/photos.html'
    )
  })

  it('does not render an official-requirements link when officialLink is absent', () => {
    const specNoLink = { ...baseSpec, officialLink: null }
    render(<PreUploadRequirementsChecklist spec={specNoLink} />)
    expect(screen.queryByRole('link', { name: /official requirements/i })).not.toBeInTheDocument()
  })

  it('does not render an official-requirements link when officialLink is undefined', () => {
    const { officialLink: _omitted, ...specNoLink } = baseSpec
    render(<PreUploadRequirementsChecklist spec={specNoLink} />)
    expect(screen.queryByRole('link', { name: /official requirements/i })).not.toBeInTheDocument()
  })

  it('renders the country name in the header', () => {
    render(<PreUploadRequirementsChecklist spec={baseSpec} />)
    expect(screen.getByText(/United States/i)).toBeInTheDocument()
  })

  it('renders nothing when requirementsBulletPoints is an empty array', () => {
    const specEmpty = { ...baseSpec, requirementsBulletPoints: [] }
    const { container } = render(<PreUploadRequirementsChecklist spec={specEmpty} />)
    expect(container.firstChild).toBeNull()
  })

  it('renders nothing when requirementsBulletPoints is missing', () => {
    const { requirementsBulletPoints: _omitted, ...specMissing } = baseSpec
    const { container } = render(<PreUploadRequirementsChecklist spec={specMissing} />)
    expect(container.firstChild).toBeNull()
  })

  it('renders nothing when spec prop is not provided', () => {
    const { container } = render(<PreUploadRequirementsChecklist />)
    expect(container.firstChild).toBeNull()
  })
})
