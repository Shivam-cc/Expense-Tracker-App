// Ensure the API URL always has a protocol prefix.
// If VITE_API_URL is set without https:// (e.g. in Vercel dashboard) the browser
// would treat it as a relative path on the current origin — so we force https://.
function resolveBaseUrl() {
  const raw = import.meta.env.VITE_API_URL
  if (!raw) return 'http://localhost:8080/api'
  const url = raw.startsWith('http://') || raw.startsWith('https://')
    ? raw
    : 'https://' + raw
  return url.replace(/\/$/, '') + '/api'   // strip trailing slash, add /api
}

const BASE_URL = resolveBaseUrl()

function getToken() {
  return localStorage.getItem('token')
}

async function request(path, options = {}) {
  const token = getToken()
  const res = await fetch(`${BASE_URL}${path}`, {
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    ...options,
  })
  if (!res.ok) {
    const err = await res.json().catch(() => ({}))
    throw new Error(err.message || `Request failed with status ${res.status}`)
  }
  if (res.status === 204) return null
  return res.json()
}

export const authApi = {
  login: (data) =>
    request('/auth/login', { method: 'POST', body: JSON.stringify(data) }),
  register: (data) =>
    request('/auth/register', { method: 'POST', body: JSON.stringify(data) }),
  sendOtp: (data) =>
    request('/auth/send-otp', { method: 'POST', body: JSON.stringify(data) }),
}

export const expensesApi = {
  list: (params = {}) => {
    const qs = new URLSearchParams(params).toString()
    return request(`/expenses?${qs}`)
  },
  create: (data) =>
    request('/expenses', { method: 'POST', body: JSON.stringify(data) }),
  update: (id, data) =>
    request(`/expenses/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
  delete: (id) =>
    request(`/expenses/${id}`, { method: 'DELETE' }),
  summary: (startDate, endDate) =>
    request(`/expenses/summary?startDate=${startDate}&endDate=${endDate}`),
}

export const categoriesApi = {
  list: () => request('/categories'),
  create: (data) =>
    request('/categories', { method: 'POST', body: JSON.stringify(data) }),
  update: (id, data) =>
    request(`/categories/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
  delete: (id) =>
    request(`/categories/${id}`, { method: 'DELETE' }),
}
