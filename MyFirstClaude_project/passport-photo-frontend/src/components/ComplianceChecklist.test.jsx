import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import ComplianceChecklist from './ComplianceChecklist'

const usSpec = {
  widthPx: 600, heightPx: 600,
  widthMm: 51, heightMm: 51,
  dpi: 300,
  backgroundColor: 'WHITE',
  backgroundColorHex: '#FFFFFF',
  faceRatioMin: 0.50, faceRatioMax: 0.69,
  description: '2×2 inch photo with white background',
}

function makeAnalysis(overrides = {}) {
  return {
    country: 'US',
    spec: usSpec,
    allPassed: true,
    passedCount: 5,
    totalCount: 5,
    checks: [
      { rule: 'Image Dimensions', passed: true, message: 'OK', expected: '≥600×600 px', actual: '600×600 px' },
      { rule: 'Aspect Ratio',     passed: true, message: 'OK', expected: '1.00:1', actual: '1.00:1' },
      { rule: 'Background Color', passed: true, message: 'OK', expected: '#FFFFFF', actual: '95% light' },
      { rule: 'Resolution / Quality', passed: true, message: 'OK', expected: '300 DPI', actual: '600×600 px' },
      { rule: 'Face Presence (estimated)', passed: true, message: 'OK', expected: 'Centred face', actual: 'Content detected' },
    ],
    ...overrides,
  }
}

describe('ComplianceChecklist', () => {
  it('renders "Compliance Results" heading', () => {
    render(<ComplianceChecklist analysis={makeAnalysis()} />)
    expect(screen.getByText('Compliance Results')).toBeInTheDocument()
  })

  it('shows green "5 / 5 Passed" badge when all checks pass', () => {
    render(<ComplianceChecklist analysis={makeAnalysis()} />)
    const badge = screen.getByText('5 / 5 Passed')
    expect(badge).toBeInTheDocument()
    expect(badge.className).toContain('green')
  })

  it('shows amber badge when some checks fail', () => {
    const analysis = makeAnalysis({ allPassed: false, passedCount: 3 })
    render(<ComplianceChecklist analysis={analysis} />)
    const badge = screen.getByText('3 / 5 Passed')
    expect(badge).toBeInTheDocument()
    expect(badge.className).toContain('amber')
  })

  it('shows red badge when fewer than half the checks pass', () => {
    const analysis = makeAnalysis({ allPassed: false, passedCount: 2 })
    render(<ComplianceChecklist analysis={analysis} />)
    const badge = screen.getByText('2 / 5 Passed')
    expect(badge).toBeInTheDocument()
    expect(badge.className).toContain('red')
  })

  it('renders all 5 individual check rows', () => {
    render(<ComplianceChecklist analysis={makeAnalysis()} />)
    expect(screen.getByText('Image Dimensions')).toBeInTheDocument()
    expect(screen.getByText('Aspect Ratio')).toBeInTheDocument()
    expect(screen.getByText('Background Color')).toBeInTheDocument()
    expect(screen.getByText('Resolution / Quality')).toBeInTheDocument()
    expect(screen.getByText('Face Presence (estimated)')).toBeInTheDocument()
  })

  it('renders expected/actual values for each check', () => {
    render(<ComplianceChecklist analysis={makeAnalysis()} />)
    // expected label for first check
    expect(screen.getByText('≥600×600 px')).toBeInTheDocument()
    // two checks have identical actual '600×600 px' — confirm both appear
    expect(screen.getAllByText('600×600 px')).toHaveLength(2)
  })

  it('shows the spec summary block with description text', () => {
    render(<ComplianceChecklist analysis={makeAnalysis()} />)
    // description is unique to the spec summary block
    expect(screen.getByText('2×2 inch photo with white background')).toBeInTheDocument()
  })

  it('shows "Check official requirements" link for US', () => {
    render(<ComplianceChecklist analysis={makeAnalysis()} />)
    const link = screen.getByText('Check official requirements →')
    expect(link).toBeInTheDocument()
    expect(link).toHaveAttribute('href', expect.stringContaining('travel.state.gov'))
    expect(link).toHaveAttribute('target', '_blank')
  })

  it('does not show official link for unknown/custom country', () => {
    render(<ComplianceChecklist analysis={makeAnalysis({ country: 'Custom' })} />)
    expect(screen.queryByText('Check official requirements →')).not.toBeInTheDocument()
  })

  it('shows "ready to correct and download" banner when allPassed is true', () => {
    render(<ComplianceChecklist analysis={makeAnalysis()} />)
    expect(screen.getByText(/ready to correct and download/i)).toBeInTheDocument()
  })

  it('shows "Some issues found" banner when allPassed is false', () => {
    const analysis = makeAnalysis({ allPassed: false, passedCount: 3 })
    render(<ComplianceChecklist analysis={analysis} />)
    expect(screen.getByText(/Some issues found/i)).toBeInTheDocument()
  })
})
