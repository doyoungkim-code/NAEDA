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

async function request(method, path, body, options = {}) {
  const headers = {}
  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }
  if (options.headers) {
    Object.assign(headers, options.headers)
  }

  let fetchBody
  if (options.multipart) {
    fetchBody = body
  } else if (body) {
    headers['Content-Type'] = 'application/json'
    fetchBody = JSON.stringify(body)
  }

  const res = await fetch(`${BASE_URL}${path}`, {
    method,
    headers,
    body: fetchBody,
  })
  if (!res.ok) {
    const err = await res.json().catch(() => ({ message: `HTTP ${res.status}` }))
    throw new Error(err.message || `HTTP ${res.status}`)
  }
  if (res.status === 204) return null
  const text = await res.text()
  if (!text) return null
  return JSON.parse(text)
}

// ── Auth ──
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
  if (accessToken) setToken(accessToken)
  return data
}

export async function signup(body) {
  return request('POST', '/auth/signup', body)
}

export async function refreshToken(refreshTokenStr) {
  return request('POST', '/auth/refresh', { refreshToken: refreshTokenStr })
}

export async function logout(refreshTokenStr) {
  return request('POST', '/auth/logout', { refreshToken: refreshTokenStr })
}

// ── Users ──
export async function getUser(userNo) {
  return request('GET', `/users/me?userNo=${userNo}`)
}

export async function updateFcmToken(userNo, fcmToken) {
  return request('PUT', `/users/me/fcm-token?userNo=${userNo}`, { fcmToken })
}

export async function updatePin(body) {
  return request('PUT', '/users/me/pin', body)
}

export async function getFacePaySettings() {
  return request('GET', '/users/me/face-pay-settings')
}

export async function updateFacePaySettings(body) {
  return request('PUT', '/users/me/face-pay-settings', body)
}

// ── Accounts ──
export async function getAccounts(userNo) {
  return request('GET', `/accounts?userNo=${userNo}`)
}

export async function getAccount(accountNo, userNo) {
  return request('GET', `/accounts/${accountNo}?userNo=${userNo}`)
}

// ── Transfers ──
export async function createTransfer(userNo, body) {
  return request('POST', `/transfers?userNo=${userNo}`, body)
}

// ── Cards ──
export async function getCards(userNo) {
  return request('GET', `/cards?userNo=${userNo}`)
}

export async function registerCard(userNo, body) {
  return request('POST', `/cards?userNo=${userNo}`, body)
}

export async function deleteCard(cardId, userNo, cardType) {
  return request('DELETE', `/cards/${cardId}?userNo=${userNo}&cardType=${cardType}`)
}

export async function getCardTransactions(cardId, userNo, userKey, startDate, endDate) {
  let path = `/cards/${cardId}/transactions?userNo=${userNo}&userKey=${userKey}`
  if (startDate) path += `&startDate=${startDate}`
  if (endDate) path += `&endDate=${endDate}`
  return request('GET', path)
}

// ── Payments ──
export async function createPayment(formData) {
  return request('POST', '/payments', formData, { multipart: true })
}

export async function getPayments(userNo, from, to, size) {
  let path = `/payments?userNo=${userNo}`
  if (from) path += `&from=${from}`
  if (to) path += `&to=${to}`
  if (size) path += `&size=${size}`
  return request('GET', path)
}

export async function getPaymentDetail(paymentId, userNo) {
  return request('GET', `/payments/${paymentId}?userNo=${userNo}`)
}

// ── Payment Requests ──
export async function createPaymentRequest(storeId, amount) {
  return request('POST', '/payment-requests', { storeId, amount })
}

export async function getPaymentRequest(requestId) {
  return request('GET', `/payment-requests/${requestId}`)
}

export async function processPaymentRequest(requestId, formData) {
  return request('POST', `/payment-requests/${requestId}/process`, formData, { multipart: true })
}

export async function getStorePaymentRequests(storeId) {
  return request('GET', `/payment-requests?storeId=${storeId}`)
}

// ── Payment Methods ──
export async function getPaymentMethods(userNo) {
  return request('GET', `/payment-methods?userNo=${userNo}`)
}

export async function setFacePayMethod(paymentMethodId, userNo) {
  return request('PATCH', `/payment-methods/${paymentMethodId}/face-pay?userNo=${userNo}`)
}

// ── Stores ──
export async function getStores(category, facePayOnly) {
  let path = '/stores'
  const params = []
  if (category) params.push(`category=${category}`)
  if (facePayOnly) params.push(`facePayOnly=true`)
  if (params.length) path += '?' + params.join('&')
  return request('GET', path)
}

export async function getStoresMap() {
  return request('GET', '/stores/map')
}

export async function getStore(storeId) {
  return request('GET', `/stores/${storeId}`)
}

export async function createStore(body) {
  return request('POST', '/stores', body)
}

// ── Points ──
export async function createPointWallet(userNo) {
  return request('POST', `/points/wallet/${userNo}`)
}

export async function getPointWallet(userNo) {
  return request('GET', `/points/wallet/${userNo}`)
}

export async function earnPoints(userNo, body) {
  return request('POST', `/points/wallet/${userNo}/earn`, body)
}

export async function usePoints(userNo, body) {
  return request('POST', `/points/wallet/${userNo}/use`, body)
}

export async function getPointHistories(userNo) {
  return request('GET', `/points/wallet/${userNo}/histories`)
}

// ── Point Products ──
export async function getProducts(size) {
  let path = '/products'
  if (size) path += `?size=${size}`
  return request('GET', path)
}

export async function getProduct(productId) {
  return request('GET', `/products/${productId}`)
}

export async function getAvailableProducts(category, keyword, size) {
  let path = '/products/available'
  const params = []
  if (category) params.push(`category=${category}`)
  if (keyword) params.push(`keyword=${keyword}`)
  if (size) params.push(`size=${size}`)
  if (params.length) path += '?' + params.join('&')
  return request('GET', path)
}

export async function createProduct(body) {
  return request('POST', '/products', body)
}

export async function updateProduct(productId, body) {
  return request('PUT', `/products/${productId}`, body)
}

export async function deleteProduct(productId) {
  return request('DELETE', `/products/${productId}`)
}

// ── Point Orders ──
export async function createOrder(userNo, body) {
  return request('POST', `/orders?userNo=${userNo}`, body)
}

export async function getOrders(userNo, size) {
  let path = `/orders?userNo=${userNo}`
  if (size) path += `&size=${size}`
  return request('GET', path)
}

// ── Transactions ──
export async function getTransactions(userNo, accountId, size) {
  let path = `/transactions?userNo=${userNo}&accountId=${accountId}`
  if (size) path += `&size=${size}`
  return request('GET', path)
}

export async function getTransactionsByPeriod(userNo, accountId, from, to, size) {
  let path = `/transactions/period?userNo=${userNo}&accountId=${accountId}&from=${from}&to=${to}`
  if (size) path += `&size=${size}`
  return request('GET', path)
}

export async function getTransactionsByType(userNo, accountId, transactionType, size) {
  let path = `/transactions/type?userNo=${userNo}&accountId=${accountId}&transactionType=${transactionType}`
  if (size) path += `&size=${size}`
  return request('GET', path)
}

// ── Reports ──
export async function getLatestReport(userNo, periodType) {
  return request('GET', `/reports/latest?userNo=${userNo}&periodType=${periodType}`)
}

export async function getReports(userNo, periodType) {
  return request('GET', `/reports?userNo=${userNo}&periodType=${periodType}`)
}

export async function generateMonthlyReport(userNo, targetMonth) {
  return request('POST', `/reports/monthly/generate?userNo=${userNo}&targetMonth=${targetMonth}`)
}

// ── FDS ──
export async function getFdsLogs(userNo) {
  return request('GET', `/fds/logs?userNo=${userNo}`)
}

export async function getFdsLogByPayment(paymentId) {
  return request('GET', `/fds/logs/payment/${paymentId}`)
}

// ── Notifications ──
export async function sendAnnouncement(body) {
  return request('POST', '/notifications/announce', body)
}

export async function getNotifications(userNo) {
  return request('GET', `/notifications?userNo=${userNo}`)
}

export async function markNotificationRead(notificationId) {
  return request('PATCH', `/notifications/${notificationId}/read`)
}

export async function getUnreadCount(userNo) {
  return request('GET', `/notifications/unread-count?userNo=${userNo}`)
}

// ── Festivals ──
export async function createFestival(body) {
  return request('POST', '/festivals', body)
}

export async function getFestivals() {
  return request('GET', '/festivals')
}

export async function getFestival(festivalId) {
  return request('GET', `/festivals/${festivalId}`)
}

export async function updateFestival(festivalId, body) {
  return request('PUT', `/festivals/${festivalId}`, body)
}

export async function deleteFestival(festivalId) {
  return request('DELETE', `/festivals/${festivalId}`)
}

export async function notifyFestival(festivalId) {
  return request('POST', `/festivals/${festivalId}/notify`)
}

// ── Face ──
export async function faceHealth() {
  return request('GET', '/v1/face/health')
}

export async function faceEnroll(formData) {
  return request('POST', '/v1/face/enroll', formData, { multipart: true })
}

export async function faceSearch(formData) {
  return request('POST', '/v1/face/search', formData, { multipart: true })
}

export async function headPoseCheck(formData) {
  return request('POST', '/v1/face/liveness/headpose/check', formData, { multipart: true })
}

// ── Pay (new system) ──
export async function createPay(userNo, formData) {
  return request('POST', '/pay', formData, { multipart: true, headers: { 'X-User-No': userNo } })
}

export async function getPayList(userNo) {
  return request('GET', '/pay', null, { headers: { 'X-User-No': userNo } })
}

export async function getPayDetail(id) {
  return request('GET', `/pay/${id}`)
}

export async function getPayMethods(userNo) {
  return request('GET', `/pay-methods?userNo=${userNo}`)
}

export async function setPayFacePay(id, userNo) {
  return request('PATCH', `/pay-methods/${id}/face-pay?userNo=${userNo}`)
}

export async function createPayRequest(body) {
  return request('POST', '/pay-requests', body)
}

export async function getPayRequest(id) {
  return request('GET', `/pay-requests/${id}`)
}

export async function processPayRequest(id, formData) {
  return request('POST', `/pay-requests/${id}/process`, formData, { multipart: true })
}

export async function getPayRequestsByStore(storeId) {
  return request('GET', `/pay-requests?storeId=${storeId}`)
}

// ── Identity ──
export async function extractResidentId(formData) {
  return request('POST', '/v1/identity/id-card/extract', formData, { multipart: true })
}

export async function confirmResidentId(body) {
  return request('POST', '/v1/identity/id-card/confirm', body)
}

// ── Internal ──
export async function saveReport(body) {
  return request('POST', '/internal/reports', body)
}

export async function getFdsVersion() {
  return request('GET', '/internal/fds/version')
}

export async function getAiMonitoring() {
  return request('GET', '/internal/fds/monitoring/ai')
}

// ── Legacy (used by POS) ──
export async function getMyStores() {
  return request('GET', '/stores')
}
