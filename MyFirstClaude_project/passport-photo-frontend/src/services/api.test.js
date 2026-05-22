import { describe, it, expect, vi, beforeEach } from 'vitest'
import axios from 'axios'
import { analyzePhoto, correctPhoto } from './api'

vi.mock('axios')

const mockFile = new File(['photo-data'], 'photo.jpg', { type: 'image/jpeg' })

describe('analyzePhoto', () => {
  beforeEach(() => { vi.clearAllMocks() })

  it('POSTs to /api/analyze and returns response data', async () => {
    const mockData = { allPassed: true, passedCount: 5, totalCount: 5 }
    axios.post.mockResolvedValue({ data: mockData })

    const result = await analyzePhoto(mockFile, 'US')
    expect(result).toEqual(mockData)
    expect(axios.post).toHaveBeenCalledTimes(1)
    const [url] = axios.post.mock.calls[0]
    expect(url).toContain('/api/analyze')
  })

  it('includes photo and country in the FormData', async () => {
    axios.post.mockResolvedValue({ data: {} })
    await analyzePhoto(mockFile, 'India')

    const formData = axios.post.mock.calls[0][1]
    expect(formData.get('photo')).toBe(mockFile)
    expect(formData.get('country')).toBe('India')
  })

  it('includes serialised customSpec in FormData when provided', async () => {
    axios.post.mockResolvedValue({ data: {} })
    const spec = { widthPx: 413, heightPx: 531, dpi: 300 }
    await analyzePhoto(mockFile, 'Custom', spec)

    const formData = axios.post.mock.calls[0][1]
    expect(formData.get('customSpec')).toBe(JSON.stringify(spec))
  })

  it('does NOT include customSpec in FormData when not provided', async () => {
    axios.post.mockResolvedValue({ data: {} })
    await analyzePhoto(mockFile, 'US')

    const formData = axios.post.mock.calls[0][1]
    expect(formData.get('customSpec')).toBeNull()
  })

  it('uses multipart/form-data content type header', async () => {
    axios.post.mockResolvedValue({ data: {} })
    await analyzePhoto(mockFile, 'US')

    const config = axios.post.mock.calls[0][2]
    expect(config.headers['Content-Type']).toBe('multipart/form-data')
  })
})

describe('correctPhoto', () => {
  beforeEach(() => { vi.clearAllMocks() })

  it('POSTs to /api/correct and returns response data', async () => {
    const fakeBlob = new Blob(['jpeg-bytes'], { type: 'image/jpeg' })
    axios.post.mockResolvedValue({ data: fakeBlob })

    const result = await correctPhoto(mockFile, 'US')
    expect(result).toBe(fakeBlob)
    const [url] = axios.post.mock.calls[0]
    expect(url).toContain('/api/correct')
  })

  it('requests a blob response type', async () => {
    axios.post.mockResolvedValue({ data: new Blob() })
    await correctPhoto(mockFile, 'UK')

    const config = axios.post.mock.calls[0][2]
    expect(config.responseType).toBe('blob')
  })

  it('includes serialised customSpec when provided', async () => {
    axios.post.mockResolvedValue({ data: new Blob() })
    const spec = { widthPx: 600, heightPx: 600, dpi: 300 }
    await correctPhoto(mockFile, 'Custom', spec)

    const formData = axios.post.mock.calls[0][1]
    expect(formData.get('customSpec')).toBe(JSON.stringify(spec))
  })

  it('does NOT include customSpec when not provided', async () => {
    axios.post.mockResolvedValue({ data: new Blob() })
    await correctPhoto(mockFile, 'US')

    const formData = axios.post.mock.calls[0][1]
    expect(formData.get('customSpec')).toBeNull()
  })
})
