import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import CustomSpecForm from './CustomSpecForm'

const emptySpec = {}

// Helpers — query by role/placeholder since the component labels lack htmlFor
const getNameInput    = () => screen.getByPlaceholderText(/UAE, Saudi Arabia/i)
const getWidthInput   = () => screen.getAllByRole('spinbutton')[0]  // Width mm
const getHeightInput  = () => screen.getAllByRole('spinbutton')[1]  // Height mm
const getBgSelect     = () => screen.getByRole('combobox')
const getFaceMinInput = () => screen.getByPlaceholderText('50')
const getFaceMaxInput = () => screen.getByPlaceholderText('75')

describe('CustomSpecForm', () => {
  it('renders all required input controls', () => {
    render(<CustomSpecForm spec={emptySpec} onChange={() => {}} />)
    expect(getNameInput()).toBeInTheDocument()
    expect(getWidthInput()).toBeInTheDocument()
    expect(getHeightInput()).toBeInTheDocument()
    expect(getBgSelect()).toBeInTheDocument()
    expect(getFaceMinInput()).toBeInTheDocument()
    expect(getFaceMaxInput()).toBeInTheDocument()
  })

  it('calculates widthPx from widthMm at 300 DPI on change', () => {
    const onChange = vi.fn()
    render(<CustomSpecForm spec={emptySpec} onChange={onChange} />)
    fireEvent.change(getWidthInput(), { target: { value: '35' } })
    // mm2px(35) = Math.round(35 * 300 / 25.4) = 413
    expect(onChange).toHaveBeenCalledWith(
      expect.objectContaining({ widthMm: 35, widthPx: 413, dpi: 300 })
    )
  })

  it('calculates heightPx from heightMm at 300 DPI on change', () => {
    const onChange = vi.fn()
    render(<CustomSpecForm spec={emptySpec} onChange={onChange} />)
    fireEvent.change(getHeightInput(), { target: { value: '45' } })
    // mm2px(45) = Math.round(45 * 300 / 25.4) = 531
    expect(onChange).toHaveBeenCalledWith(
      expect.objectContaining({ heightMm: 45, heightPx: 531, dpi: 300 })
    )
  })

  it('sets backgroundColorHex to #F0F0F0 when LIGHT_GREY is selected', () => {
    const onChange = vi.fn()
    render(<CustomSpecForm spec={emptySpec} onChange={onChange} />)
    fireEvent.change(getBgSelect(), { target: { value: 'LIGHT_GREY' } })
    expect(onChange).toHaveBeenCalledWith(
      expect.objectContaining({ backgroundColor: 'LIGHT_GREY', backgroundColorHex: '#F0F0F0' })
    )
  })

  it('sets faceRatioMin as decimal fraction when percentage changes', () => {
    const onChange = vi.fn()
    render(<CustomSpecForm spec={emptySpec} onChange={onChange} />)
    fireEvent.change(getFaceMinInput(), { target: { value: '60' } })
    expect(onChange).toHaveBeenCalledWith(
      expect.objectContaining({ faceRatioMin: 0.60 })
    )
  })

  it('sets faceRatioMax as decimal fraction when percentage changes', () => {
    const onChange = vi.fn()
    render(<CustomSpecForm spec={emptySpec} onChange={onChange} />)
    fireEvent.change(getFaceMaxInput(), { target: { value: '75' } })
    expect(onChange).toHaveBeenCalledWith(
      expect.objectContaining({ faceRatioMax: 0.75 })
    )
  })

  it('shows pixel preview when both widthMm and heightMm are non-zero', () => {
    render(
      <CustomSpecForm
        spec={{ widthMm: 35, heightMm: 45, widthPx: 413, heightPx: 531 }}
        onChange={() => {}}
      />
    )
    expect(screen.getByText(/413 × 531 px at 300 DPI/)).toBeInTheDocument()
  })

  it('does not show pixel preview when dimensions are zero', () => {
    render(<CustomSpecForm spec={{ widthMm: 0, heightMm: 0 }} onChange={() => {}} />)
    expect(screen.queryByText(/px at 300 DPI/)).not.toBeInTheDocument()
  })

  it('includes description in onChange payload that names the country', () => {
    const onChange = vi.fn()
    render(<CustomSpecForm spec={emptySpec} onChange={onChange} />)
    fireEvent.change(getNameInput(), { target: { value: 'Andorra' } })
    const payload = onChange.mock.calls[0][0]
    expect(payload.description).toContain('Andorra')
  })

  it('always sets dpi to 300 regardless of which field changes', () => {
    const onChange = vi.fn()
    render(<CustomSpecForm spec={emptySpec} onChange={onChange} />)
    fireEvent.change(getWidthInput(), { target: { value: '51' } })
    expect(onChange).toHaveBeenCalledWith(expect.objectContaining({ dpi: 300 }))
  })

  it('calls onChange with widthMm 0 and widthPx 0 when width is zero', () => {
    // parseInt('0', 10) = 0 → 0 || 0 = 0 (same fallback path as NaN || 0 for non-numeric input)
    // Note: number inputs sanitise non-numeric strings to '' at the DOM level, preventing the
    // React onChange from firing for truly invalid text — '0' exercises the identical code path.
    const onChange = vi.fn()
    render(<CustomSpecForm spec={emptySpec} onChange={onChange} />)
    fireEvent.change(getWidthInput(), { target: { value: '0' } })
    expect(onChange).toHaveBeenCalledWith(
      expect.objectContaining({ widthMm: 0, widthPx: 0, dpi: 300 })
    )
  })
})
