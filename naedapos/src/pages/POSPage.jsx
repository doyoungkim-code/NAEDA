import { useState, useEffect, useRef, useCallback } from 'react'
import { createPayRequest, getPayRequest, setToken, getToken } from '../api'
import PaymentStatus from '../components/PaymentStatus'

const SAMPLE_MENU = {
  '커피': [
    { name: 'H)아메리카노', price: 2500 },
    { name: 'H)에스프레소', price: 2500 },
    { name: 'H)카페라떼', price: 3500 },
    { name: 'H)카푸치노', price: 3500 },
    { name: 'H)카페모카', price: 3500 },
    { name: 'H)바닐라라떼', price: 4000 },
    { name: 'I)아메리카노', price: 3000 },
    { name: 'I)에스프레소', price: 3000 },
    { name: 'I)카페라떼', price: 4000 },
    { name: 'I)카푸치노', price: 4000 },
    { name: 'I)카페모카', price: 4000 },
    { name: 'I)바닐라라떼', price: 4500 },
  ],
  '에이드': [
    { name: '레몬에이드', price: 4000 },
    { name: '자몽에이드', price: 4000 },
    { name: '청포도에이드', price: 4000 },
    { name: '패션후르츠', price: 4500 },
    { name: '망고에이드', price: 4500 },
    { name: '딸기에이드', price: 4500 },
  ],
  '스무디': [
    { name: '망고스무디', price: 5000 },
    { name: '딸기스무디', price: 5000 },
    { name: '블루베리', price: 5000 },
    { name: '요거트스무디', price: 5500 },
    { name: '초코스무디', price: 5000 },
    { name: '그린티스무디', price: 5000 },
  ],
  '디저트': [
    { name: '크로와상', price: 3500 },
    { name: '소금빵', price: 2500 },
    { name: '초코머핀', price: 3000 },
    { name: '치즈케이크', price: 5500 },
    { name: '티라미수', price: 6000 },
    { name: '마카롱세트', price: 8000 },
  ],
  '음료': [
    { name: '녹차라떼', price: 4500 },
    { name: '고구마라떼', price: 4500 },
    { name: '밀크티', price: 4500 },
    { name: '초코라떼', price: 4000 },
    { name: '생과일주스', price: 5500 },
    { name: '탄산수', price: 2000 },
  ],
}

export default function POSPage({ onGoAdmin }) {
  const [storeId, setStoreId] = useState(localStorage.getItem('pos_storeId') || '1')
  const [storeName, setStoreName] = useState(localStorage.getItem('pos_storeName') || 'NAEDA 카페')
  const [orderItems, setOrderItems] = useState([])
  const [selectedIdx, setSelectedIdx] = useState(-1)
  const [category, setCategory] = useState('커피')
  const [numInput, setNumInput] = useState('')
  const [loading, setLoading] = useState(false)
  const [paymentState, setPaymentState] = useState(null)
  const [showSettings, setShowSettings] = useState(false)
  const [settingsForm, setSettingsForm] = useState({ storeId, storeName, token: getToken() })
  const [now, setNow] = useState(new Date())
  const pollingRef = useRef(null)

  useEffect(() => {
    const t = setInterval(() => setNow(new Date()), 1000)
    return () => clearInterval(t)
  }, [])

  useEffect(() => {
    localStorage.setItem('pos_storeId', storeId)
    localStorage.setItem('pos_storeName', storeName)
  }, [storeId, storeName])

  useEffect(() => {
    return () => { if (pollingRef.current) clearInterval(pollingRef.current) }
  }, [])

  const totalAmount = orderItems.reduce((s, i) => s + i.price * i.qty, 0)
  const totalQty = orderItems.reduce((s, i) => s + i.qty, 0)

  function addItem(menuItem) {
    setOrderItems(prev => {
      const idx = prev.findIndex(o => o.name === menuItem.name)
      if (idx >= 0) {
        const next = [...prev]
        next[idx] = { ...next[idx], qty: next[idx].qty + 1 }
        setSelectedIdx(idx)
        return next
      }
      setSelectedIdx(prev.length)
      return [...prev, { name: menuItem.name, price: menuItem.price, qty: 1 }]
    })
  }

  function removeSelected() {
    if (selectedIdx < 0) return
    setOrderItems(prev => prev.filter((_, i) => i !== selectedIdx))
    setSelectedIdx(-1)
  }

  function clearAll() {
    setOrderItems([])
    setSelectedIdx(-1)
    setNumInput('')
  }

  function changeQty(delta) {
    if (selectedIdx < 0) return
    setOrderItems(prev => {
      const next = [...prev]
      const newQty = next[selectedIdx].qty + delta
      if (newQty <= 0) {
        setSelectedIdx(-1)
        return next.filter((_, i) => i !== selectedIdx)
      }
      next[selectedIdx] = { ...next[selectedIdx], qty: newQty }
      return next
    })
  }

  function handleNumpad(val) {
    if (val === 'C') { setNumInput(''); return }
    if (val === 'BS') { setNumInput(p => p.slice(0, -1)); return }
    setNumInput(p => p + val)
  }

  function handleEnter() {
    const num = parseInt(numInput)
    if (!num || selectedIdx < 0) { setNumInput(''); return }
    setOrderItems(prev => {
      const next = [...prev]
      next[selectedIdx] = { ...next[selectedIdx], qty: num }
      return next
    })
    setNumInput('')
  }

  const startPolling = useCallback((requestId) => {
    pollingRef.current = setInterval(async () => {
      try {
        const data = await getPayRequest(requestId)
        setPaymentState(data)
        if (['SUCCESS', 'FAILED', 'BLOCKED'].includes(data.status)) {
          clearInterval(pollingRef.current)
          pollingRef.current = null
        }
      } catch {
        setPaymentState(prev => ({ ...prev, status: 'EXPIRED', failureReason: '결제 요청이 만료되었습니다.' }))
        clearInterval(pollingRef.current)
        pollingRef.current = null
      }
    }, 1500)
  }, [])

  async function handleFacePay() {
    if (totalAmount <= 0) return
    if (!getToken()) {
      alert('JWT 토큰이 설정되지 않았습니다.\n설정 버튼에서 토큰을 먼저 입력해주세요.')
      return
    }
    setLoading(true)
    try {
      const data = await createPayRequest({ storeId: Number(storeId), amount: totalAmount })
      setPaymentState(data)
      startPolling(data.requestId)
    } catch (err) { alert(err.message) }
    finally { setLoading(false) }
  }

  function handleStatusClose() {
    if (pollingRef.current) { clearInterval(pollingRef.current); pollingRef.current = null }
    if (paymentState?.status === 'SUCCESS') clearAll()
    setPaymentState(null)
  }

  function saveSettings() {
    setStoreId(settingsForm.storeId)
    setStoreName(settingsForm.storeName)
    if (settingsForm.token) setToken(settingsForm.token)
    setShowSettings(false)
  }

  const dateStr = now.toLocaleDateString('ko-KR', { year: 'numeric', month: '2-digit', day: '2-digit', weekday: 'short' })
  const timeStr = now.toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit', hour12: false })

  return (
    <div className="pos">
      {/* ── Top Bar ── */}
      <div className="pos-topbar">
        <div className="left-section">
          <span className="logo">NAEDA POS</span>
          <span className="store-label">매장명 : <span>{storeName}</span> &nbsp; 매장 ID : <span>{storeId}</span></span>
        </div>
        <div className="right-section">
          <span className="datetime">{dateStr}</span>
          <span className="time-main">{timeStr}</span>
          <button className="settings-btn" onClick={() => {
            setSettingsForm({ storeId, storeName, token: getToken() })
            setShowSettings(true)
          }}>설정</button>
          {onGoAdmin && <button className="settings-btn" onClick={onGoAdmin}>관리자</button>}
        </div>
      </div>

      {/* ── Body ── */}
      <div className="pos-body">
        {/* ====== LEFT ====== */}
        <div className="pos-left">
          {/* Order Table */}
          <div className="order-block">
            {orderItems.length === 0 ? (
              <div className="empty-order-msg">우측 메뉴를 선택하세요</div>
            ) : (
              <div className="order-table-wrap">
                <table className="order-table">
                  <thead>
                    <tr>
                      <th style={{ width: 32 }}>No</th>
                      <th>메뉴명</th>
                      <th style={{ width: 65 }}>단가</th>
                      <th style={{ width: 40 }}>수량</th>
                      <th style={{ width: 50 }}>할인</th>
                      <th style={{ width: 75 }}>금액</th>
                    </tr>
                  </thead>
                  <tbody>
                    {orderItems.map((item, i) => (
                      <tr key={i} className={selectedIdx === i ? 'selected' : ''} onClick={() => setSelectedIdx(i)}>
                        <td>{i + 1}</td>
                        <td className="name">{item.name}</td>
                        <td className="price">{item.price.toLocaleString()}</td>
                        <td>{item.qty}</td>
                        <td>0</td>
                        <td className="price">{(item.price * item.qty).toLocaleString()}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>

          {/* Total Bar */}
          <div className="order-total-bar">
            <span className="total-label">합 계</span>
            <div className="total-nums">
              <span>{totalQty}건</span>
              <span className="total-amount">{totalAmount.toLocaleString()}</span>
            </div>
          </div>

          {/* Action Buttons */}
          <div className="order-actions-block">
            <button className="danger" onClick={clearAll}>전체취소</button>
            <button className="danger" onClick={removeSelected}>선택삭제</button>
            <button onClick={() => {}}>할인처리</button>
            <button onClick={() => {}}>수량변경</button>
            <button onClick={() => changeQty(-1)}>−</button>
            <button onClick={() => changeQty(1)}>+</button>
          </div>

          {/* Bottom: Info + Numpad + Side */}
          <div className="pos-left-bottom">
            {/* Payment Info */}
            <div className="payment-info-block">
              <div className="info-tabs">
                <button className="active">결제정보</button>
                <button>결제내역</button>
              </div>
              <div className="info-body">
                <div className="info-title">Payment Info</div>
                <div className="info-row">
                  <span className="label">총 금 액</span>
                  <span className="value">{totalAmount.toLocaleString()}</span>
                </div>
                <div className="info-row">
                  <span className="label">할인금액</span>
                  <span className="value">0</span>
                </div>
                <div className="info-row highlight">
                  <span className="label">받을금액</span>
                  <span className="value">{totalAmount.toLocaleString()}</span>
                </div>
                <div className="info-row">
                  <span className="label">받은금액</span>
                  <span className="value">0</span>
                </div>
              </div>
            </div>

            {/* Numpad */}
            <div className="numpad-block">
              <div className="numpad-grid">
                {['7','8','9','4','5','6','1','2','3'].map(k => (
                  <button key={k} onClick={() => handleNumpad(k)}>{k}</button>
                ))}
              </div>
              <div className="numpad-row">
                <button onClick={() => handleNumpad('0')}>0</button>
                <button onClick={() => handleNumpad('00')}>00</button>
                <button className="enter-btn" onClick={handleEnter}>
                  {numInput ? `수량 ${numInput}` : 'Enter'}
                </button>
              </div>
              <div className="numpad-row">
                <button onClick={() => handleNumpad('BS')}>←</button>
                <button className="clear-btn" onClick={() => handleNumpad('C')}>C</button>
                <button className="enter-btn" onClick={handleEnter}>수량변경</button>
              </div>
            </div>

            {/* Side Buttons */}
            <div className="side-buttons-block">
              <button>모바일<br/>오더</button>
              <button className="highlight-side">웹정보</button>
              <button>환전</button>
              <button>주문접수</button>
            </div>
          </div>
        </div>

        {/* ====== RIGHT ====== */}
        <div className="pos-right">
          {/* Category Tabs */}
          <div className="category-tabs">
            {Object.keys(SAMPLE_MENU).map(cat => (
              <button key={cat} className={category === cat ? 'active' : ''} onClick={() => setCategory(cat)}>
                {cat}
              </button>
            ))}
            <button className="tab-arrow">◀</button>
            <button className="tab-arrow">▶</button>
          </div>

          {/* Menu Grid */}
          <div className="menu-grid-block">
            {(SAMPLE_MENU[category] || []).map((item, i) => {
              const inOrder = orderItems.find(o => o.name === item.name)
              return (
                <div key={i} className="menu-item" onClick={() => addItem(item)}>
                  {inOrder && <span className="qty-badge">{inOrder.qty}</span>}
                  <span className="item-name">{item.name}</span>
                  <span className="item-price">{item.price.toLocaleString()}</span>
                </div>
              )
            })}
          </div>

          {/* Payment Buttons */}
          <div className="pay-buttons-block">
            <button className="pay-facepay" onClick={handleFacePay} disabled={loading || totalAmount <= 0}>
              {loading ? '요청중...' : '페이스페이'}
            </button>
            <button className="pay-card">신용카드</button>
            <button className="pay-cash">현금</button>
            <button className="pay-etc">복합결제</button>
            <button className="pay-etc">영수증관리</button>
          </div>
        </div>
      </div>

      {/* Settings Modal */}
      {showSettings && (
        <div className="modal-overlay" onClick={() => setShowSettings(false)}>
          <div className="modal-card" onClick={e => e.stopPropagation()}>
            <h3>매장 설정</h3>
            <div className="form-group">
              <label>매장 ID</label>
              <input type="number" value={settingsForm.storeId} onChange={e => setSettingsForm(p => ({ ...p, storeId: e.target.value }))} />
            </div>
            <div className="form-group">
              <label>매장명</label>
              <input value={settingsForm.storeName} onChange={e => setSettingsForm(p => ({ ...p, storeName: e.target.value }))} />
            </div>
            <div className="form-group">
              <label>JWT 토큰 (선택)</label>
              <input value={settingsForm.token} onChange={e => setSettingsForm(p => ({ ...p, token: e.target.value }))} placeholder="인증 필요시 입력" />
            </div>
            <div className="btn-row">
              <button className="btn-cancel" onClick={() => setShowSettings(false)}>취소</button>
              <button className="btn-save" onClick={saveSettings}>저장</button>
            </div>
          </div>
        </div>
      )}

      {paymentState && <PaymentStatus payment={paymentState} onClose={handleStatusClose} />}
    </div>
  )
}
