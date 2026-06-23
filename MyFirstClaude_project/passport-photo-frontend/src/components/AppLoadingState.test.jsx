import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import App from '../App'

vi.mock('@vladmandic/face-api', () => ({ default: {} }))

vi.mock('../services/api', () => ({
  analyzePhoto: vi.fn(),
  correctPhoto: vi.fn(),
  downloadPhotoSheet: vi.fn(),
  downloadPhotoPdf: vi.fn(),
}))

vi.mock('../services/logger', () => ({
  logger: { info: vi.fn(), warn: vi.fn(), error: vi.fn() },
}))

const mockCountriesResponse = {
  US: {
    name: 'United States',
    requirementsBulletPoints: ['Neutral expression'],
    source: 'State Dept',
    description: '2x2 inch',
    officialLink: 'https://travel.state.gov',
  },
}

function makeFetchResponse(body) {
  return Promise.resolve({
    ok: true,
    json: () => Promise.resolve(body),
  })
}

let originalFetch

beforeEach(() => {
  vi.restoreAllMocks()
  originalFetch = global.fetch
})

afterEach(() => {
  vi.restoreAllMocks()
  global.fetch = originalFetch
})

describe('App — country specs loading states', () => {
  it('shows a loading spinner while the countries fetch is pending', () => {
    global.fetch = vi.fn(() => new Promise(() => {}))

    render(<App />)

    expect(document.querySelector('.animate-spin')).toBeInTheDocument()
  })

  it('shows the fallback message when the countries fetch fails', async () => {
    global.fetch = vi.fn(() => Promise.reject(new Error('Network Error')))

    render(<App />)

    await waitFor(() => {
      expect(
        screen.getByText(/Could not load country requirements/i)
      ).toBeInTheDocument()
    })

    expect(
      screen.getByText(/Make sure the backend is running/i)
    ).toBeInTheDocument()
  })

  it('renders PreUploadRequirementsChecklist for the default country once specs load', async () => {
    global.fetch = vi.fn(() => makeFetchResponse(mockCountriesResponse))

    render(<App />)

    await waitFor(() => {
      expect(screen.getByText('Neutral expression')).toBeInTheDocument()
    })
  })
})
