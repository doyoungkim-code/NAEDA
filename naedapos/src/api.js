const BASE_URL = '/api'

let token = localStorage.getItem('pos_token') || ''

export function setToken(t) {
  token = t
  localStorage.setItem('pos_token', t)
}

export function getToken() {
  return token
}

export function clearToken() {
  token = ''
  localStorage.removeItem('pos_token')
}

async function request(method, path, body) {
  const headers = { 'Content-Type': 'application/json' }
  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }
  const res = await fetch(`${BASE_URL}${path}`, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined,
  })
  if (!res.ok) {
    const err = await res.json().catch(() => ({ message: `HTTP ${res.status}` }))
    throw new Error(err.message || `HTTP ${res.status}`)
  }
  if (res.status === 204) return null
  return res.json()
}

export async function login(userId, password) {
  const res = await fetch(`${BASE_URL}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ userId, password }),
  })
  if (!res.ok) {
    const err = await res.json().catch(() => ({ message: '로그인 실패' }))
    throw new Error(err.message || '로그인 실패')
  }
  const data = await res.json()
  const accessToken = data.accessToken || res.headers.get('Authorization')?.replace('Bearer ', '')
  if (accessToken) {
    setToken(accessToken)
  }
  return data
}

export async function createPaymentRequest(storeId, amount) {
  return request('POST', '/payment-requests', { storeId, amount })
}

export async function getPaymentRequest(requestId) {
  return request('GET', `/payment-requests/${requestId}`)
}

export async function getStorePaymentRequests(storeId) {
  return request('GET', `/payment-requests?storeId=${storeId}`)
}

export async function getMyStores() {
  return request('GET', '/stores')
}
