import axios from 'axios'

const BASE_URL = 'http://localhost:8080/api'

export async function analyzePhoto(photo, country) {
  const form = new FormData()
  form.append('photo', photo)
  form.append('country', country)
  const res = await axios.post(`${BASE_URL}/analyze`, form, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  return res.data
}

export async function correctPhoto(photo, country) {
  const form = new FormData()
  form.append('photo', photo)
  form.append('country', country)
  const res = await axios.post(`${BASE_URL}/correct`, form, {
    headers: { 'Content-Type': 'multipart/form-data' },
    responseType: 'blob',
  })
  return res.data
}
