import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import CountrySelector from './CountrySelector'

describe('CountrySelector', () => {
  it('renders a select with all 16 country options', () => {
    render(<CountrySelector country="US" onChange={() => {}} />)
    const options = screen.getAllByRole('option')
    expect(options).toHaveLength(16)
    const values = options.map((o) => o.value)
    expect(values).toEqual(
      expect.arrayContaining([
        'US', 'UK', 'India', 'Canada', 'Australia', 'UAE', 'Schengen',
        'Germany', 'France', 'Japan', 'China', 'Brazil', 'NewZealand', 'Singapore', 'Mexico',
        'Custom',
      ])
    )
  })

  it('shows the selected country info card', () => {
    render(<CountrySelector country="US" onChange={() => {}} />)
    expect(screen.getByText('United States')).toBeInTheDocument()
    expect(screen.getByText('2×2 in · white bg')).toBeInTheDocument()
  })

  it('calls onChange with the new country code when selection changes', () => {
    const onChange = vi.fn()
    render(<CountrySelector country="US" onChange={onChange} />)
    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'UK' } })
    expect(onChange).toHaveBeenCalledWith('UK')
  })

  it('shows Custom info card when Custom is selected', () => {
    render(<CountrySelector country="Custom" onChange={() => {}} />)
    expect(screen.getByText('My Country not on list')).toBeInTheDocument()
    expect(screen.getByText('Enter your own specifications')).toBeInTheDocument()
  })

  it('shows Schengen info when Schengen is selected', () => {
    render(<CountrySelector country="Schengen" onChange={() => {}} />)
    expect(screen.getByText('Schengen (EU)')).toBeInTheDocument()
  })

  it('reflects the current country code as the selected option', () => {
    render(<CountrySelector country="India" onChange={() => {}} />)
    expect(screen.getByRole('combobox')).toHaveValue('India')
  })

  it('does not render the info card for an unrecognized country code', () => {
    // selected = undefined when no COUNTRIES entry matches → {selected && ...} guard fires
    render(<CountrySelector country="INVALID_CODE" onChange={() => {}} />)
    // All 16 options still render inside the <select>
    expect(screen.getAllByRole('option')).toHaveLength(16)
    // Spec-description strings only appear in the info card, never in <option> text
    expect(screen.queryByText('2×2 in · white bg')).not.toBeInTheDocument()
    expect(screen.queryByText('Enter your own specifications')).not.toBeInTheDocument()
  })
})
