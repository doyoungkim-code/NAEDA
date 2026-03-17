import { useState, useEffect, useCallback, useRef, createContext, useContext } from 'react'
import * as api from '../api'

// ── Toast Context ──
const ToastContext = createContext()

function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([])
  const idRef = useRef(0)

  const addToast = useCallback((msg, type = 'info') => {
    const id = ++idRef.current
    setToasts(prev => [...prev, { id, msg, type }])
    setTimeout(() => {
      setToasts(prev => prev.map(t => t.id === id ? { ...t, leaving: true } : t))
      setTimeout(() => setToasts(prev => prev.filter(t => t.id !== id)), 300)
    }, 3500)
  }, [])

  const toast = useCallback({
    success: (msg) => addToast(msg, 'success'),
    error: (msg) => addToast(msg, 'error'),
    info: (msg) => addToast(msg, 'info'),
  }, [addToast])

  return (
    <ToastContext.Provider value={toast}>
      {children}
      <div className="toast-container">
        {toasts.map(t => (
          <div key={t.id} className={`toast ${t.type} ${t.leaving ? 'leaving' : ''}`}>
            <span className="toast-icon">
              {t.type === 'success' ? '✓' : t.type === 'error' ? '✕' : 'ℹ'}
            </span>
            <span className="toast-msg">{t.msg}</span>
            <button className="toast-close" onClick={() => setToasts(prev => prev.filter(x => x.id !== t.id))}>×</button>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  )
}

function useToast() { return useContext(ToastContext) }

// ── 공통 유틸 ──
function JsonViewer({ data }) {
  const [open, setOpen] = useState(false)
  if (data === null || data === undefined) return null
  return (
    <div className="json-viewer">
      <button className="json-toggle" onClick={() => setOpen(p => !p)}>
        {open ? '▼ JSON 접기' : '▶ 원본 JSON 보기'}
      </button>
      {open && (
        <pre className="json-pre">{JSON.stringify(data, null, 2)}</pre>
      )}
    </div>
  )
}

function DataTable({ columns, data, onRowClick, actions }) {
  if (!data || data.length === 0) return <div className="empty-table">데이터가 없습니다</div>
  return (
    <div className="data-table-wrap">
      <table className="data-table">
        <thead>
          <tr>
            {columns.map(c => <th key={c.key}>{c.label}</th>)}
            {actions && <th>작업</th>}
          </tr>
        </thead>
        <tbody>
          {data.map((row, i) => (
            <tr key={i} onClick={() => onRowClick?.(row)} className={onRowClick ? 'clickable' : ''}>
              {columns.map(c => (
                <td key={c.key}>{c.render ? c.render(row[c.key], row) : String(row[c.key] ?? '-')}</td>
              ))}
              {actions && <td className="action-cell">{actions(row)}</td>}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

function StatusBadge({ status }) {
  const colors = {
    SUCCESS: '#10b981', COMPLETED: '#10b981', ACTIVE: '#10b981', EARN: '#10b981',
    PENDING: '#f59e0b', PROCESSING: '#3b82f6',
    FAILED: '#ef4444', BLOCKED: '#ef4444', INACTIVE: '#999', USE: '#ef4444',
    EXPIRED: '#999', DEPOSIT: '#10b981', WITHDRAW: '#f59e0b', TRANSFER: '#3b82f6',
    WEEKLY: '#8b5cf6', MONTHLY: '#3b82f6',
  }
  return (
    <span className="status-badge" style={{ background: colors[status] || '#888' }}>
      {status}
    </span>
  )
}

function LoadingSpinner() {
  return <div className="admin-spinner" />
}

// ── 개별 관리 패널 ──

function DashboardPanel({ g, onNavigate }) {
  const [stats, setStats] = useState(null)
  const [loading, setLoading] = useState(false)
  const toast = useToast()

  async function loadStats() {
    if (!g.userNo) { toast.error('상단의 UserNo를 먼저 설정하세요'); return }
    setLoading(true)
    try {
      const results = await Promise.allSettled([
        api.getAccounts(g.userNo),
        api.getPointWallet(g.userNo),
        api.getPayments(g.userNo),
        api.getStores(),
      ])
      setStats({
        accounts: results[0].status === 'fulfilled' ? results[0].value : [],
        wallet: results[1].status === 'fulfilled' ? results[1].value : null,
        payments: results[2].status === 'fulfilled' ? results[2].value : [],
        stores: results[3].status === 'fulfilled' ? results[3].value : [],
      })
      toast.success('대시보드 통계를 불러왔습니다')
    } catch { toast.error('통계 조회 실패') }
    setLoading(false)
  }

  const now = new Date()
  const greeting = now.getHours() < 12 ? '좋은 아침이에요' : now.getHours() < 18 ? '좋은 오후에요' : '좋은 저녁이에요'

  return (
    <>
      <div className="welcome-banner">
        <div className="welcome-title">{greeting} 👋</div>
        <div className="welcome-sub">NAEDA Admin Console — {now.toLocaleDateString('ko-KR', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' })}</div>
      </div>

      <div className="quick-actions">
        <button className="quick-action" onClick={() => onNavigate?.('settings')}>
          <div className="qa-icon teal">🔑</div>
          <div className="qa-text"><div className="qa-label">로그인</div><div className="qa-desc">인증 및 토큰 설정</div></div>
        </button>
        <button className="quick-action" onClick={() => onNavigate?.('user')}>
          <div className="qa-icon blue">👤</div>
          <div className="qa-text"><div className="qa-label">회원가입</div><div className="qa-desc">새 테스트 계정 생성</div></div>
        </button>
        <button className="quick-action" onClick={() => onNavigate?.('store')}>
          <div className="qa-icon amber">🏪</div>
          <div className="qa-text"><div className="qa-label">매장 등록</div><div className="qa-desc">새 매장 추가</div></div>
        </button>
        <button className="quick-action" onClick={() => onNavigate?.('payment')}>
          <div className="qa-icon rose">💰</div>
          <div className="qa-text"><div className="qa-label">결제 테스트</div><div className="qa-desc">결제 요청 생성</div></div>
        </button>
        <button className="quick-action" onClick={() => onNavigate?.('point')}>
          <div className="qa-icon purple">⭐</div>
          <div className="qa-text"><div className="qa-label">포인트</div><div className="qa-desc">적립 및 사용</div></div>
        </button>
        <button className="quick-action" onClick={() => onNavigate?.('monitoring')}>
          <div className="qa-icon green">🛡️</div>
          <div className="qa-text"><div className="qa-label">시스템 상태</div><div className="qa-desc">AI & FDS 모니터링</div></div>
        </button>
      </div>

      <div className="panel">
        <div className="panel-header">
          <h3>📊 Overview</h3>
          <button className="btn-sm btn-primary" onClick={loadStats} disabled={loading}>
            {loading ? <><LoadingSpinner /> 조회 중...</> : '통계 불러오기'}
          </button>
        </div>
        {stats ? (
          <div className="stat-cards">
            <div className="stat-card">
              <div className="stat-value">{Array.isArray(stats.accounts) ? stats.accounts.length : 0}</div>
              <div className="stat-label">보유 계좌</div>
            </div>
            <div className="stat-card accent">
              <div className="stat-value">{stats.wallet?.balance?.toLocaleString() ?? '-'}</div>
              <div className="stat-label">포인트 잔액</div>
            </div>
            <div className="stat-card">
              <div className="stat-value">{Array.isArray(stats.payments) ? stats.payments.length : 0}</div>
              <div className="stat-label">결제 건수</div>
            </div>
            <div className="stat-card">
              <div className="stat-value">{Array.isArray(stats.stores) ? stats.stores.length : 0}</div>
              <div className="stat-label">등록 매장</div>
            </div>
          </div>
        ) : (
          <div className="sub-section">
            <p className="hint">UserNo를 설정하고 '통계 불러오기'를 클릭하세요</p>
          </div>
        )}
      </div>
    </>
  )
}

function UserPanel({ g }) {
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [signupForm, setSignupForm] = useState({ userId: '', password: '', username: '', residentNo: '', phone: '', institutionCode: '00100', pin: '' })
  const [signupResult, setSignupResult] = useState(null)

  async function loadUser() {
    if (!g.userNo) return
    setLoading(true); setError('')
    try { setUser(await api.getUser(g.userNo)) }
    catch (e) { setError(e.message) }
    setLoading(false)
  }

  async function handleSignup(e) {
    e.preventDefault(); setError('')
    try {
      const res = await api.signup(signupForm)
      setSignupResult(res)
    } catch (e) { setError(e.message) }
  }

  return (
    <div className="panel">
      <div className="panel-header"><h3>회원 관리</h3></div>

      <div className="sub-section">
        <h4>회원 조회</h4>
        <div className="inline-form">
          <button className="btn-sm btn-primary" onClick={loadUser} disabled={!g.userNo || loading}>
            {loading ? '조회 중...' : '조회'}
          </button>
          {!g.userNo && <span className="hint">UserNo를 설정하세요</span>}
        </div>
        {error && <div className="error-msg">{error}</div>}
        {user && (
          <div className="info-grid">
            <div className="info-item"><span className="info-label">User No</span><span>{user.userNo}</span></div>
            <div className="info-item"><span className="info-label">아이디</span><span>{user.userId}</span></div>
            <div className="info-item"><span className="info-label">이름</span><span>{user.username}</span></div>
            <div className="info-item"><span className="info-label">FCM</span><span className="mono">{user.fcmToken || '(없음)'}</span></div>
          </div>
        )}
        <JsonViewer data={user} />
      </div>

      <div className="sub-section">
        <h4>회원가입</h4>
        <form className="form-grid" onSubmit={handleSignup}>
          <div className="field"><label>이메일</label><input value={signupForm.userId} onChange={e => setSignupForm(p => ({ ...p, userId: e.target.value }))} placeholder="user@example.com" required /></div>
          <div className="field"><label>비밀번호</label><input type="password" value={signupForm.password} onChange={e => setSignupForm(p => ({ ...p, password: e.target.value }))} required /></div>
          <div className="field"><label>이름</label><input value={signupForm.username} onChange={e => setSignupForm(p => ({ ...p, username: e.target.value }))} required /></div>
          <div className="field"><label>주민번호</label><input value={signupForm.residentNo} onChange={e => setSignupForm(p => ({ ...p, residentNo: e.target.value }))} placeholder="000101-3000000" required /></div>
          <div className="field"><label>전화번호</label><input value={signupForm.phone} onChange={e => setSignupForm(p => ({ ...p, phone: e.target.value }))} placeholder="010-1234-5678" required /></div>
          <div className="field"><label>기관코드</label><input value={signupForm.institutionCode} onChange={e => setSignupForm(p => ({ ...p, institutionCode: e.target.value }))} /></div>
          <div className="field"><label>PIN</label><input value={signupForm.pin} onChange={e => setSignupForm(p => ({ ...p, pin: e.target.value }))} placeholder="6자리" required /></div>
          <div className="field full"><button type="submit" className="btn-sm btn-primary">가입하기</button></div>
        </form>
        {signupResult && (
          <div className="success-box">
            가입 완료! userNo: <strong>{signupResult.userNo}</strong>, userId: {signupResult.userId}
          </div>
        )}
        <JsonViewer data={signupResult} />
      </div>
    </div>
  )
}

function AccountPanel({ g }) {
  const [accounts, setAccounts] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [transferForm, setTransferForm] = useState({ withdrawalAccountNo: '', depositAccountNo: '', amount: '', memo: '' })
  const [transferResult, setTransferResult] = useState(null)

  async function loadAccounts() {
    if (!g.userNo) return
    setLoading(true); setError('')
    try { setAccounts(await api.getAccounts(g.userNo)) }
    catch (e) { setError(e.message); setAccounts([]) }
    setLoading(false)
  }

  async function handleTransfer(e) {
    e.preventDefault(); setError('')
    try {
      const res = await api.createTransfer(g.userNo, {
        withdrawalAccountNo: transferForm.withdrawalAccountNo,
        depositAccountNo: transferForm.depositAccountNo,
        amount: Number(transferForm.amount),
        memo: transferForm.memo || undefined,
      })
      setTransferResult(res)
      loadAccounts()
    } catch (e) { setError(e.message) }
  }

  return (
    <div className="panel">
      <div className="panel-header">
        <h3>계좌 / 이체</h3>
        <button className="btn-sm btn-primary" onClick={loadAccounts} disabled={!g.userNo || loading}>계좌 조회</button>
      </div>
      {error && <div className="error-msg">{error}</div>}

      <div className="sub-section">
        <h4>보유 계좌</h4>
        <DataTable
          columns={[
            { key: 'accountId', label: 'ID' },
            { key: 'bankName', label: '은행' },
            { key: 'accountNo', label: '계좌번호', render: v => <span className="mono">{v}</span> },
            { key: 'accountBalance', label: '잔액', render: v => <strong>{Number(v).toLocaleString()}원</strong> },
          ]}
          data={accounts}
          onRowClick={(row) => setTransferForm(p => ({ ...p, withdrawalAccountNo: row.accountNo }))}
        />
        <p className="hint">계좌를 클릭하면 출금계좌에 자동 입력됩니다</p>
        <JsonViewer data={accounts} />
      </div>

      <div className="sub-section">
        <h4>계좌 이체</h4>
        <form className="form-grid" onSubmit={handleTransfer}>
          <div className="field"><label>출금 계좌</label><input value={transferForm.withdrawalAccountNo} onChange={e => setTransferForm(p => ({ ...p, withdrawalAccountNo: e.target.value }))} required /></div>
          <div className="field"><label>입금 계좌</label><input value={transferForm.depositAccountNo} onChange={e => setTransferForm(p => ({ ...p, depositAccountNo: e.target.value }))} required /></div>
          <div className="field"><label>금액</label><input type="number" value={transferForm.amount} onChange={e => setTransferForm(p => ({ ...p, amount: e.target.value }))} required /></div>
          <div className="field"><label>메모</label><input value={transferForm.memo} onChange={e => setTransferForm(p => ({ ...p, memo: e.target.value }))} placeholder="선택" /></div>
          <div className="field full"><button type="submit" className="btn-sm btn-primary">이체 실행</button></div>
        </form>
        {transferResult && <div className="success-box">이체 완료!</div>}
        <JsonViewer data={transferResult} />
      </div>
    </div>
  )
}

function StorePanel({ g }) {
  const [stores, setStores] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [form, setForm] = useState({ userNo: '', accountId: '', storeName: '', categoryId: '', roadAddress: '', numberAddress: '', latitude: '', longitude: '', phone: '', facePayEnabled: 'true' })
  const [createResult, setCreateResult] = useState(null)
  const [detail, setDetail] = useState(null)

  async function loadStores() {
    setLoading(true); setError('')
    try { setStores(await api.getStores()) }
    catch (e) { setError(e.message) }
    setLoading(false)
  }

  async function handleCreate(e) {
    e.preventDefault(); setError(''); setCreateResult(null)
    try {
      const res = await api.createStore({
        userNo: Number(form.userNo || g.userNo),
        accountId: Number(form.accountId),
        storeName: form.storeName,
        categoryId: Number(form.categoryId),
        roadAddress: form.roadAddress,
        numberAddress: form.numberAddress,
        latitude: parseFloat(form.latitude),
        longitude: parseFloat(form.longitude),
        phone: form.phone,
        facePayEnabled: form.facePayEnabled === 'true',
      })
      setCreateResult(res)
      loadStores()
    } catch (e) { setError(e.message) }
  }

  useEffect(() => {
    if (g.userNo) setForm(p => ({ ...p, userNo: g.userNo }))
  }, [g.userNo])

  return (
    <div className="panel">
      <div className="panel-header">
        <h3>매장 관리</h3>
        <button className="btn-sm btn-primary" onClick={loadStores} disabled={loading}>매장 목록 조회</button>
      </div>
      {error && <div className="error-msg">{error}</div>}

      <div className="sub-section">
        <h4>매장 목록</h4>
        <DataTable
          columns={[
            { key: 'storeId', label: 'ID' },
            { key: 'storeName', label: '매장명' },
            { key: 'categoryName', label: '카테고리' },
            { key: 'roadAddress', label: '주소' },
            { key: 'phone', label: '전화' },
            { key: 'facePayEnabled', label: 'FacePay', render: v => v ? '✅' : '❌' },
            { key: 'rating', label: '평점' },
          ]}
          data={stores}
          onRowClick={(row) => setDetail(row)}
        />
        {detail && (
          <div className="detail-box">
            <div className="detail-header">
              <strong>{detail.storeName}</strong>
              <button className="btn-xs" onClick={() => setDetail(null)}>닫기</button>
            </div>
            <div className="info-grid">
              <div className="info-item"><span className="info-label">Store ID</span><span>{detail.storeId}</span></div>
              <div className="info-item"><span className="info-label">카테고리</span><span>{detail.categoryName}</span></div>
              <div className="info-item"><span className="info-label">도로명</span><span>{detail.roadAddress}</span></div>
              <div className="info-item"><span className="info-label">지번</span><span>{detail.numberAddress}</span></div>
              <div className="info-item"><span className="info-label">위도</span><span>{detail.latitude}</span></div>
              <div className="info-item"><span className="info-label">경도</span><span>{detail.longitude}</span></div>
              <div className="info-item"><span className="info-label">전화</span><span>{detail.phone}</span></div>
              <div className="info-item"><span className="info-label">지역업체</span><span>{detail.isLocalBusiness ? '예' : '아니오'}</span></div>
            </div>
            <JsonViewer data={detail} />
          </div>
        )}
      </div>

      <div className="sub-section">
        <h4>매장 등록</h4>
        <form className="form-grid" onSubmit={handleCreate}>
          <div className="field"><label>사장님 UserNo</label><input type="number" value={form.userNo} onChange={e => setForm(p => ({ ...p, userNo: e.target.value }))} required /></div>
          <div className="field"><label>정산 계좌 ID</label><input type="number" value={form.accountId} onChange={e => setForm(p => ({ ...p, accountId: e.target.value }))} required /></div>
          <div className="field"><label>매장명</label><input value={form.storeName} onChange={e => setForm(p => ({ ...p, storeName: e.target.value }))} required /></div>
          <div className="field"><label>카테고리 ID</label><input type="number" value={form.categoryId} onChange={e => setForm(p => ({ ...p, categoryId: e.target.value }))} required /></div>
          <div className="field full"><label>도로명 주소</label><input value={form.roadAddress} onChange={e => setForm(p => ({ ...p, roadAddress: e.target.value }))} /></div>
          <div className="field full"><label>지번 주소</label><input value={form.numberAddress} onChange={e => setForm(p => ({ ...p, numberAddress: e.target.value }))} /></div>
          <div className="field"><label>위도</label><input value={form.latitude} onChange={e => setForm(p => ({ ...p, latitude: e.target.value }))} placeholder="36.1071" /></div>
          <div className="field"><label>경도</label><input value={form.longitude} onChange={e => setForm(p => ({ ...p, longitude: e.target.value }))} placeholder="128.4164" /></div>
          <div className="field"><label>전화번호</label><input value={form.phone} onChange={e => setForm(p => ({ ...p, phone: e.target.value }))} /></div>
          <div className="field">
            <label>FacePay</label>
            <select value={form.facePayEnabled} onChange={e => setForm(p => ({ ...p, facePayEnabled: e.target.value }))}>
              <option value="true">활성화</option>
              <option value="false">비활성화</option>
            </select>
          </div>
          <div className="field full"><button type="submit" className="btn-sm btn-primary">매장 등록</button></div>
        </form>
        {createResult && <div className="success-box">매장 등록 완료! ID: <strong>{createResult.storeId}</strong></div>}
        <JsonViewer data={createResult} />
      </div>
    </div>
  )
}

function PaymentPanel({ g }) {
  const [payments, setPayments] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [dateRange, setDateRange] = useState({ from: '', to: '' })
  const [detail, setDetail] = useState(null)
  const [reqForm, setReqForm] = useState({ storeId: '', amount: '' })
  const [reqResult, setReqResult] = useState(null)
  const [reqStatus, setReqStatus] = useState(null)
  const [pollId, setPollId] = useState(null)

  useEffect(() => {
    if (g.storeId) setReqForm(p => ({ ...p, storeId: g.storeId }))
  }, [g.storeId])

  async function loadPayments() {
    if (!g.userNo) return
    setLoading(true); setError('')
    try { setPayments(await api.getPayments(g.userNo, dateRange.from || undefined, dateRange.to || undefined)) }
    catch (e) { setError(e.message) }
    setLoading(false)
  }

  async function createReq(e) {
    e.preventDefault(); setError(''); setReqResult(null); setReqStatus(null)
    try {
      const res = await api.createPaymentRequest(Number(reqForm.storeId), Number(reqForm.amount))
      setReqResult(res)
      // 폴링 시작
      const id = setInterval(async () => {
        try {
          const st = await api.getPaymentRequest(res.requestId)
          setReqStatus(st)
          if (['SUCCESS', 'FAILED', 'BLOCKED', 'EXPIRED'].includes(st.status)) {
            clearInterval(id)
            setPollId(null)
          }
        } catch {
          clearInterval(id)
          setPollId(null)
        }
      }, 1500)
      setPollId(id)
    } catch (e) { setError(e.message) }
  }

  useEffect(() => () => { if (pollId) clearInterval(pollId) }, [pollId])

  return (
    <div className="panel">
      <div className="panel-header"><h3>결제 관리</h3></div>
      {error && <div className="error-msg">{error}</div>}

      <div className="sub-section">
        <h4>결제 내역 조회</h4>
        <div className="inline-form">
          <input type="date" value={dateRange.from} onChange={e => setDateRange(p => ({ ...p, from: e.target.value }))} />
          <span>~</span>
          <input type="date" value={dateRange.to} onChange={e => setDateRange(p => ({ ...p, to: e.target.value }))} />
          <button className="btn-sm btn-primary" onClick={loadPayments} disabled={!g.userNo || loading}>조회</button>
        </div>
        <DataTable
          columns={[
            { key: 'paymentId', label: 'ID' },
            { key: 'storeId', label: '매장' },
            { key: 'amount', label: '금액', render: v => <strong>{Number(v).toLocaleString()}원</strong> },
            { key: 'status', label: '상태', render: v => <StatusBadge status={v} /> },
            { key: 'authMethod', label: '인증방식' },
            { key: 'earnedPoints', label: '적립포인트' },
          ]}
          data={payments}
          onRowClick={row => setDetail(row)}
        />
        {detail && (
          <div className="detail-box">
            <div className="detail-header"><strong>결제 #{detail.paymentId}</strong><button className="btn-xs" onClick={() => setDetail(null)}>닫기</button></div>
            <div className="info-grid">
              <div className="info-item"><span className="info-label">금액</span><span>{Number(detail.amount).toLocaleString()}원</span></div>
              <div className="info-item"><span className="info-label">상태</span><StatusBadge status={detail.status} /></div>
              <div className="info-item"><span className="info-label">인증방식</span><span>{detail.authMethod}</span></div>
              <div className="info-item"><span className="info-label">인증레벨</span><span>{detail.authLevel}</span></div>
              <div className="info-item"><span className="info-label">유사도</span><span>{detail.similarity}</span></div>
              <div className="info-item"><span className="info-label">FDS 점수</span><span>{detail.fdsScore}</span></div>
              <div className="info-item"><span className="info-label">적립포인트</span><span>{detail.earnedPoints}</span></div>
              <div className="info-item"><span className="info-label">SSAFY TX</span><span className="mono">{detail.ssafyTransactionId}</span></div>
            </div>
            <JsonViewer data={detail} />
          </div>
        )}
      </div>

      <div className="sub-section">
        <h4>결제 요청 생성 (POS 테스트)</h4>
        <form className="inline-form" onSubmit={createReq}>
          <div className="field compact"><label>매장 ID</label><input type="number" value={reqForm.storeId} onChange={e => setReqForm(p => ({ ...p, storeId: e.target.value }))} required /></div>
          <div className="field compact"><label>금액</label><input type="number" value={reqForm.amount} onChange={e => setReqForm(p => ({ ...p, amount: e.target.value }))} required /></div>
          <button type="submit" className="btn-sm btn-primary">결제요청</button>
        </form>
        {reqResult && (
          <div className="result-inline">
            <span>Request ID: <strong className="mono">{reqResult.requestId}</strong></span>
            {reqStatus && <StatusBadge status={reqStatus.status} />}
            {reqStatus?.status === 'PENDING' && <LoadingSpinner />}
            {reqStatus?.failureReason && <span className="error-text">{reqStatus.failureReason}</span>}
          </div>
        )}
        <JsonViewer data={reqStatus || reqResult} />
      </div>
    </div>
  )
}

function PointPanel({ g }) {
  const [wallet, setWallet] = useState(null)
  const [histories, setHistories] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [earnForm, setEarnForm] = useState({ amount: '', description: '' })
  const [useForm, setUseForm] = useState({ amount: '', description: '' })

  async function loadWallet() {
    if (!g.userNo) return
    setLoading(true); setError('')
    try {
      setWallet(await api.getPointWallet(g.userNo))
      setHistories(await api.getPointHistories(g.userNo))
    } catch (e) { setError(e.message) }
    setLoading(false)
  }

  async function handleEarn(e) {
    e.preventDefault(); setError('')
    try {
      await api.earnPoints(g.userNo, { amount: Number(earnForm.amount), description: earnForm.description || undefined })
      setEarnForm({ amount: '', description: '' })
      loadWallet()
    } catch (e) { setError(e.message) }
  }

  async function handleUse(e) {
    e.preventDefault(); setError('')
    try {
      await api.usePoints(g.userNo, { amount: Number(useForm.amount), description: useForm.description || undefined })
      setUseForm({ amount: '', description: '' })
      loadWallet()
    } catch (e) { setError(e.message) }
  }

  async function createWallet() {
    setError('')
    try { await api.createPointWallet(g.userNo); loadWallet() }
    catch (e) { setError(e.message) }
  }

  return (
    <div className="panel">
      <div className="panel-header">
        <h3>포인트 관리</h3>
        <div className="btn-group">
          <button className="btn-sm btn-primary" onClick={loadWallet} disabled={!g.userNo}>조회</button>
          <button className="btn-sm btn-outline" onClick={createWallet} disabled={!g.userNo}>지갑 생성</button>
        </div>
      </div>
      {error && <div className="error-msg">{error}</div>}

      {wallet && (
        <div className="stat-cards">
          <div className="stat-card accent">
            <div className="stat-value">{wallet.balance?.toLocaleString()}</div>
            <div className="stat-label">잔액</div>
          </div>
          <div className="stat-card">
            <div className="stat-value">{wallet.totalEarned?.toLocaleString()}</div>
            <div className="stat-label">총 적립</div>
          </div>
          <div className="stat-card">
            <div className="stat-value">{wallet.totalUsed?.toLocaleString()}</div>
            <div className="stat-label">총 사용</div>
          </div>
        </div>
      )}

      <div className="two-col">
        <div className="sub-section">
          <h4>포인트 적립</h4>
          <form className="form-stack" onSubmit={handleEarn}>
            <input type="number" placeholder="적립 금액" value={earnForm.amount} onChange={e => setEarnForm(p => ({ ...p, amount: e.target.value }))} required />
            <input placeholder="설명 (선택)" value={earnForm.description} onChange={e => setEarnForm(p => ({ ...p, description: e.target.value }))} />
            <button type="submit" className="btn-sm btn-primary">적립</button>
          </form>
        </div>
        <div className="sub-section">
          <h4>포인트 사용</h4>
          <form className="form-stack" onSubmit={handleUse}>
            <input type="number" placeholder="사용 금액" value={useForm.amount} onChange={e => setUseForm(p => ({ ...p, amount: e.target.value }))} required />
            <input placeholder="설명 (선택)" value={useForm.description} onChange={e => setUseForm(p => ({ ...p, description: e.target.value }))} />
            <button type="submit" className="btn-sm btn-primary">사용</button>
          </form>
        </div>
      </div>

      <div className="sub-section">
        <h4>포인트 내역</h4>
        <DataTable
          columns={[
            { key: 'historyId', label: 'ID' },
            { key: 'type', label: '유형', render: v => <StatusBadge status={v} /> },
            { key: 'amount', label: '금액', render: (v, row) => <strong style={{ color: row.type === 'EARN' ? '#10b981' : '#ef4444' }}>{row.type === 'EARN' ? '+' : '-'}{Number(v).toLocaleString()}</strong> },
            { key: 'balanceAfter', label: '잔액', render: v => Number(v).toLocaleString() },
            { key: 'description', label: '설명' },
            { key: 'created', label: '일시', render: v => v ? new Date(v).toLocaleString('ko-KR') : '-' },
          ]}
          data={histories}
        />
      </div>
    </div>
  )
}

function ProductPanel() {
  const [products, setProducts] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [form, setForm] = useState({ productName: '', description: '', category: '', pointPrice: '', stockQuantity: '', imageUrl: '' })
  const [editId, setEditId] = useState(null)

  async function loadProducts() {
    setLoading(true); setError('')
    try { setProducts(await api.getProducts()) }
    catch (e) { setError(e.message) }
    setLoading(false)
  }

  async function handleSubmit(e) {
    e.preventDefault(); setError('')
    const body = { ...form, pointPrice: Number(form.pointPrice), stockQuantity: Number(form.stockQuantity) }
    try {
      if (editId) {
        await api.updateProduct(editId, body)
      } else {
        await api.createProduct(body)
      }
      setForm({ productName: '', description: '', category: '', pointPrice: '', stockQuantity: '', imageUrl: '' })
      setEditId(null)
      loadProducts()
    } catch (e) { setError(e.message) }
  }

  async function handleDelete(id) {
    if (!confirm('정말 삭제하시겠습니까?')) return
    try { await api.deleteProduct(id); loadProducts() }
    catch (e) { setError(e.message) }
  }

  function startEdit(p) {
    setEditId(p.productId)
    setForm({ productName: p.productName, description: p.description || '', category: p.category || '', pointPrice: p.pointPrice, stockQuantity: p.stockQuantity, imageUrl: p.imageUrl || '' })
  }

  return (
    <div className="panel">
      <div className="panel-header">
        <h3>포인트 상품</h3>
        <button className="btn-sm btn-primary" onClick={loadProducts} disabled={loading}>상품 목록 조회</button>
      </div>
      {error && <div className="error-msg">{error}</div>}

      <div className="sub-section">
        <h4>{editId ? `상품 수정 (#${editId})` : '상품 등록'}</h4>
        <form className="form-grid" onSubmit={handleSubmit}>
          <div className="field"><label>상품명</label><input value={form.productName} onChange={e => setForm(p => ({ ...p, productName: e.target.value }))} required /></div>
          <div className="field"><label>카테고리</label><input value={form.category} onChange={e => setForm(p => ({ ...p, category: e.target.value }))} /></div>
          <div className="field"><label>포인트 가격</label><input type="number" value={form.pointPrice} onChange={e => setForm(p => ({ ...p, pointPrice: e.target.value }))} required /></div>
          <div className="field"><label>재고</label><input type="number" value={form.stockQuantity} onChange={e => setForm(p => ({ ...p, stockQuantity: e.target.value }))} required /></div>
          <div className="field full"><label>설명</label><input value={form.description} onChange={e => setForm(p => ({ ...p, description: e.target.value }))} /></div>
          <div className="field full"><label>이미지 URL</label><input value={form.imageUrl} onChange={e => setForm(p => ({ ...p, imageUrl: e.target.value }))} /></div>
          <div className="field full btn-group">
            <button type="submit" className="btn-sm btn-primary">{editId ? '수정' : '등록'}</button>
            {editId && <button type="button" className="btn-sm btn-outline" onClick={() => { setEditId(null); setForm({ productName: '', description: '', category: '', pointPrice: '', stockQuantity: '', imageUrl: '' }) }}>취소</button>}
          </div>
        </form>
      </div>

      <DataTable
        columns={[
          { key: 'productId', label: 'ID' },
          { key: 'productName', label: '상품명' },
          { key: 'category', label: '카테고리' },
          { key: 'pointPrice', label: '가격', render: v => `${Number(v).toLocaleString()}P` },
          { key: 'stockQuantity', label: '재고' },
          { key: 'status', label: '상태', render: v => <StatusBadge status={v} /> },
        ]}
        data={products}
        actions={row => (
          <>
            <button className="btn-xs btn-outline" onClick={(e) => { e.stopPropagation(); startEdit(row) }}>수정</button>
            <button className="btn-xs btn-danger" onClick={(e) => { e.stopPropagation(); handleDelete(row.productId) }}>삭제</button>
          </>
        )}
      />
    </div>
  )
}

function TransactionPanel({ g }) {
  const [txns, setTxns] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [filter, setFilter] = useState({ accountId: '', type: '', from: '', to: '' })

  async function loadTxns() {
    if (!g.userNo || !filter.accountId) return
    setLoading(true); setError('')
    try {
      if (filter.type) {
        setTxns(await api.getTransactionsByType(g.userNo, filter.accountId, filter.type))
      } else if (filter.from && filter.to) {
        setTxns(await api.getTransactionsByPeriod(g.userNo, filter.accountId, filter.from, filter.to))
      } else {
        setTxns(await api.getTransactions(g.userNo, filter.accountId))
      }
    } catch (e) { setError(e.message) }
    setLoading(false)
  }

  return (
    <div className="panel">
      <div className="panel-header"><h3>거래내역</h3></div>
      {error && <div className="error-msg">{error}</div>}
      <div className="inline-form">
        <div className="field compact"><label>계좌 ID</label><input type="number" value={filter.accountId} onChange={e => setFilter(p => ({ ...p, accountId: e.target.value }))} required /></div>
        <div className="field compact">
          <label>유형</label>
          <select value={filter.type} onChange={e => setFilter(p => ({ ...p, type: e.target.value }))}>
            <option value="">전체</option>
            <option value="DEPOSIT">입금</option>
            <option value="WITHDRAW">출금</option>
            <option value="TRANSFER">이체</option>
          </select>
        </div>
        <div className="field compact"><label>시작일</label><input type="date" value={filter.from} onChange={e => setFilter(p => ({ ...p, from: e.target.value }))} /></div>
        <div className="field compact"><label>종료일</label><input type="date" value={filter.to} onChange={e => setFilter(p => ({ ...p, to: e.target.value }))} /></div>
        <button className="btn-sm btn-primary" onClick={loadTxns} disabled={loading || !filter.accountId}>조회</button>
      </div>
      <DataTable
        columns={[
          { key: 'logId', label: 'ID' },
          { key: 'transactionType', label: '유형', render: v => <StatusBadge status={v} /> },
          { key: 'amount', label: '금액', render: v => <strong>{Number(v).toLocaleString()}원</strong> },
          { key: 'balanceAfter', label: '잔액', render: v => Number(v).toLocaleString() },
          { key: 'counterpart', label: '상대' },
          { key: 'memo', label: '메모' },
          { key: 'category', label: '카테고리' },
          { key: 'transacted', label: '일시', render: v => v ? new Date(v).toLocaleString('ko-KR') : '-' },
        ]}
        data={txns}
      />
    </div>
  )
}

function ReportPanel({ g }) {
  const [reports, setReports] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [periodType, setPeriodType] = useState('MONTHLY')
  const [genMonth, setGenMonth] = useState('')
  const [detail, setDetail] = useState(null)

  async function loadReports() {
    if (!g.userNo) return
    setLoading(true); setError('')
    try { setReports(await api.getReports(g.userNo, periodType)) }
    catch (e) { setError(e.message) }
    setLoading(false)
  }

  async function generate() {
    if (!g.userNo || !genMonth) return
    setError('')
    try {
      await api.generateMonthlyReport(g.userNo, genMonth)
      loadReports()
    } catch (e) { setError(e.message) }
  }

  return (
    <div className="panel">
      <div className="panel-header"><h3>소비 리포트</h3></div>
      {error && <div className="error-msg">{error}</div>}
      <div className="inline-form">
        <select value={periodType} onChange={e => setPeriodType(e.target.value)}>
          <option value="WEEKLY">주간</option>
          <option value="MONTHLY">월간</option>
        </select>
        <button className="btn-sm btn-primary" onClick={loadReports} disabled={!g.userNo || loading}>리포트 조회</button>
        <span className="divider">|</span>
        <input type="month" value={genMonth} onChange={e => setGenMonth(e.target.value)} />
        <button className="btn-sm btn-outline" onClick={generate} disabled={!g.userNo || !genMonth}>월간 리포트 생성</button>
      </div>
      <DataTable
        columns={[
          { key: 'reportId', label: 'ID' },
          { key: 'periodType', label: '유형' },
          { key: 'periodStart', label: '시작' },
          { key: 'periodEnd', label: '종료' },
          { key: 'totalSpending', label: '총 지출', render: v => <strong>{Number(v).toLocaleString()}원</strong> },
          { key: 'localSpending', label: '지역 소비', render: v => Number(v).toLocaleString() },
          { key: 'localRatio', label: '지역비율', render: v => `${v}%` },
          { key: 'localGrade', label: '등급' },
        ]}
        data={reports}
        onRowClick={row => setDetail(row)}
      />
      {detail && (
        <div className="detail-box">
          <div className="detail-header"><strong>리포트 #{detail.reportId}</strong><button className="btn-xs" onClick={() => setDetail(null)}>닫기</button></div>
          <p><strong>인사이트:</strong> {detail.insights}</p>
          <JsonViewer data={detail} />
        </div>
      )}
    </div>
  )
}

function FestivalPanel() {
  const toast = useToast()
  const [festivals, setFestivals] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [form, setForm] = useState({ title: '', description: '', startDate: '', endDate: '', location: '', imageUrl: '' })
  const [editId, setEditId] = useState(null)

  async function loadFestivals() {
    setLoading(true); setError('')
    try { setFestivals(await api.getFestivals()) }
    catch (e) { setError(e.message) }
    setLoading(false)
  }

  async function handleSubmit(e) {
    e.preventDefault(); setError('')
    try {
      if (editId) {
        await api.updateFestival(editId, form)
      } else {
        await api.createFestival(form)
      }
      setForm({ title: '', description: '', startDate: '', endDate: '', location: '', imageUrl: '' })
      setEditId(null)
      loadFestivals()
    } catch (e) { setError(e.message) }
  }

  async function handleDelete(id) {
    if (!confirm('삭제하시겠습니까?')) return
    try { await api.deleteFestival(id); loadFestivals() }
    catch (e) { setError(e.message) }
  }

  async function handleNotify(id) {
    try {
      const res = await api.notifyFestival(id)
      toast.success(`축제 알림 발송 완료! 성공: ${res.successCount}건`)
    } catch (e) { setError(e.message) }
  }

  return (
    <div className="panel">
      <div className="panel-header">
        <h3>축제/이벤트</h3>
        <button className="btn-sm btn-primary" onClick={loadFestivals} disabled={loading}>목록 조회</button>
      </div>
      {error && <div className="error-msg">{error}</div>}

      <div className="sub-section">
        <h4>{editId ? `축제 수정 (#${editId})` : '축제 등록'}</h4>
        <form className="form-grid" onSubmit={handleSubmit}>
          <div className="field full"><label>제목</label><input value={form.title} onChange={e => setForm(p => ({ ...p, title: e.target.value }))} required /></div>
          <div className="field full"><label>설명</label><textarea value={form.description} onChange={e => setForm(p => ({ ...p, description: e.target.value }))} rows={2} /></div>
          <div className="field"><label>시작일</label><input type="date" value={form.startDate} onChange={e => setForm(p => ({ ...p, startDate: e.target.value }))} required /></div>
          <div className="field"><label>종료일</label><input type="date" value={form.endDate} onChange={e => setForm(p => ({ ...p, endDate: e.target.value }))} required /></div>
          <div className="field"><label>장소</label><input value={form.location} onChange={e => setForm(p => ({ ...p, location: e.target.value }))} /></div>
          <div className="field"><label>이미지 URL</label><input value={form.imageUrl} onChange={e => setForm(p => ({ ...p, imageUrl: e.target.value }))} /></div>
          <div className="field full btn-group">
            <button type="submit" className="btn-sm btn-primary">{editId ? '수정' : '등록'}</button>
            {editId && <button type="button" className="btn-sm btn-outline" onClick={() => { setEditId(null); setForm({ title: '', description: '', startDate: '', endDate: '', location: '', imageUrl: '' }) }}>취소</button>}
          </div>
        </form>
      </div>

      <DataTable
        columns={[
          { key: 'festivalId', label: 'ID' },
          { key: 'title', label: '제목' },
          { key: 'startDate', label: '시작' },
          { key: 'endDate', label: '종료' },
          { key: 'location', label: '장소' },
        ]}
        data={festivals}
        actions={row => (
          <>
            <button className="btn-xs btn-outline" onClick={e => { e.stopPropagation(); setEditId(row.festivalId); setForm({ title: row.title, description: row.description || '', startDate: row.startDate, endDate: row.endDate, location: row.location || '', imageUrl: row.imageUrl || '' }) }}>수정</button>
            <button className="btn-xs btn-primary" onClick={e => { e.stopPropagation(); handleNotify(row.festivalId) }}>알림</button>
            <button className="btn-xs btn-danger" onClick={e => { e.stopPropagation(); handleDelete(row.festivalId) }}>삭제</button>
          </>
        )}
      />
    </div>
  )
}

function NotificationPanel({ g }) {
  const toast = useToast()
  const [notifs, setNotifs] = useState([])
  const [error, setError] = useState('')
  const [unread, setUnread] = useState(null)
  const [form, setForm] = useState({ title: '', body: '' })

  async function loadNotifs() {
    if (!g.userNo) return
    setError('')
    try {
      setNotifs(await api.getNotifications(g.userNo))
      setUnread(await api.getUnreadCount(g.userNo))
    } catch (e) { setError(e.message) }
  }

  async function sendAnnounce(e) {
    e.preventDefault(); setError('')
    try {
      const res = await api.sendAnnouncement(form)
      toast.success(`공지 발송 완료! 성공: ${res.successCount}건 / 실패: ${res.failCount}건`)
      setForm({ title: '', body: '' })
    } catch (e) { setError(e.message) }
  }

  async function markRead(id) {
    try { await api.markNotificationRead(id); loadNotifs() }
    catch (e) { setError(e.message) }
  }

  return (
    <div className="panel">
      <div className="panel-header">
        <h3>알림 관리 {unread && <span className="badge-count">{unread.unreadCount}</span>}</h3>
        <button className="btn-sm btn-primary" onClick={loadNotifs} disabled={!g.userNo}>알림 조회</button>
      </div>
      {error && <div className="error-msg">{error}</div>}

      <div className="sub-section">
        <h4>전체 공지 발송</h4>
        <form className="form-grid" onSubmit={sendAnnounce}>
          <div className="field full"><label>제목</label><input value={form.title} onChange={e => setForm(p => ({ ...p, title: e.target.value }))} required /></div>
          <div className="field full"><label>내용</label><textarea value={form.body} onChange={e => setForm(p => ({ ...p, body: e.target.value }))} rows={3} required /></div>
          <div className="field full"><button type="submit" className="btn-sm btn-primary">발송</button></div>
        </form>
      </div>

      <DataTable
        columns={[
          { key: 'notificationId', label: 'ID' },
          { key: 'title', label: '제목' },
          { key: 'body', label: '내용' },
          { key: 'type', label: '유형' },
          { key: 'isRead', label: '읽음', render: v => v ? '✅' : '❌' },
          { key: 'createdAt', label: '일시', render: v => v ? new Date(v).toLocaleString('ko-KR') : '-' },
        ]}
        data={notifs}
        actions={row => !row.isRead ? (
          <button className="btn-xs btn-outline" onClick={e => { e.stopPropagation(); markRead(row.notificationId) }}>읽음</button>
        ) : null}
      />
    </div>
  )
}

function MonitoringPanel({ g }) {
  const [fdsLogs, setFdsLogs] = useState([])
  const [fdsVersion, setFdsVersion] = useState(null)
  const [aiMetrics, setAiMetrics] = useState(null)
  const [faceHealth, setFaceHealth] = useState(null)
  const [error, setError] = useState('')

  async function loadFds() {
    if (!g.userNo) return
    setError('')
    try { setFdsLogs(await api.getFdsLogs(g.userNo)) }
    catch (e) { setError(e.message) }
  }

  async function loadSystem() {
    setError('')
    const results = await Promise.allSettled([
      api.getFdsVersion(),
      api.getAiMonitoring(),
      api.faceHealth(),
    ])
    if (results[0].status === 'fulfilled') setFdsVersion(results[0].value)
    if (results[1].status === 'fulfilled') setAiMetrics(results[1].value)
    if (results[2].status === 'fulfilled') setFaceHealth(results[2].value)
  }

  return (
    <div className="panel">
      <div className="panel-header">
        <h3>모니터링</h3>
        <div className="btn-group">
          <button className="btn-sm btn-primary" onClick={loadSystem}>시스템 상태</button>
          <button className="btn-sm btn-outline" onClick={loadFds} disabled={!g.userNo}>FDS 로그</button>
        </div>
      </div>
      {error && <div className="error-msg">{error}</div>}

      <div className="stat-cards">
        <div className={`stat-card ${faceHealth?.status === 'UP' ? 'good' : ''}`}>
          <div className="stat-value">{faceHealth?.status || '-'}</div>
          <div className="stat-label">Face AI</div>
        </div>
        {fdsVersion && (
          <>
            <div className="stat-card"><div className="stat-value">{fdsVersion.featureVersion || '-'}</div><div className="stat-label">Feature Ver.</div></div>
            <div className="stat-card"><div className="stat-value">{fdsVersion.ruleVersion || '-'}</div><div className="stat-label">Rule Ver.</div></div>
            <div className="stat-card"><div className="stat-value">{fdsVersion.modelVersion || '-'}</div><div className="stat-label">Model Ver.</div></div>
          </>
        )}
        {aiMetrics && (
          <>
            <div className="stat-card good"><div className="stat-value">{aiMetrics.successCount}</div><div className="stat-label">AI 성공</div></div>
            <div className="stat-card bad"><div className="stat-value">{aiMetrics.failCount}</div><div className="stat-label">AI 실패</div></div>
            <div className="stat-card"><div className="stat-value">{aiMetrics.averageLatencyMs}ms</div><div className="stat-label">평균 지연</div></div>
          </>
        )}
      </div>

      <div className="sub-section">
        <h4>FDS 이상거래 로그</h4>
        <DataTable
          columns={[
            { key: 'fdsId', label: 'ID' },
            { key: 'paymentId', label: '결제' },
            { key: 'anomalyScore', label: '이상점수', render: v => <strong style={{ color: v > 0.7 ? '#ef4444' : v > 0.4 ? '#f59e0b' : '#10b981' }}>{v}</strong> },
            { key: 'triggeredRules', label: '규칙' },
            { key: 'actionTaken', label: '조치' },
            { key: 'userConfirmed', label: '확인', render: v => v ? '✅' : '❌' },
            { key: 'detected', label: '탐지일', render: v => v ? new Date(v).toLocaleString('ko-KR') : '-' },
          ]}
          data={fdsLogs}
        />
      </div>
    </div>
  )
}

function CardPanel({ g }) {
  const [cards, setCards] = useState([])
  const [error, setError] = useState('')
  const [form, setForm] = useState({ cardUniqueNo: '', withdrawalAccountNo: '', withdrawalDate: '15', cardTypeCode: '1' })

  async function loadCards() {
    if (!g.userNo) return
    setError('')
    try { setCards(await api.getCards(g.userNo)) }
    catch (e) { setError(e.message) }
  }

  async function handleRegister(e) {
    e.preventDefault(); setError('')
    try {
      await api.registerCard(g.userNo, form)
      setForm({ cardUniqueNo: '', withdrawalAccountNo: '', withdrawalDate: '15', cardTypeCode: '1' })
      loadCards()
    } catch (e) { setError(e.message) }
  }

  async function handleDelete(card) {
    if (!confirm(`카드 ${card.cardNo}를 삭제하시겠습니까?`)) return
    try { await api.deleteCard(card.cardId, g.userNo, card.cardType); loadCards() }
    catch (e) { setError(e.message) }
  }

  return (
    <div className="panel">
      <div className="panel-header">
        <h3>카드 관리</h3>
        <button className="btn-sm btn-primary" onClick={loadCards} disabled={!g.userNo}>카드 조회</button>
      </div>
      {error && <div className="error-msg">{error}</div>}

      <DataTable
        columns={[
          { key: 'cardId', label: 'ID' },
          { key: 'cardName', label: '카드명' },
          { key: 'cardNo', label: '카드번호', render: v => <span className="mono">{v}</span> },
          { key: 'cardType', label: '유형' },
          { key: 'cardIssuerName', label: '발급사' },
          { key: 'cardExpiryDate', label: '만료일' },
        ]}
        data={cards}
        actions={row => <button className="btn-xs btn-danger" onClick={e => { e.stopPropagation(); handleDelete(row) }}>삭제</button>}
      />

      <div className="sub-section">
        <h4>카드 등록</h4>
        <form className="form-grid" onSubmit={handleRegister}>
          <div className="field"><label>카드 고유번호</label><input value={form.cardUniqueNo} onChange={e => setForm(p => ({ ...p, cardUniqueNo: e.target.value }))} required /></div>
          <div className="field"><label>출금 계좌번호</label><input value={form.withdrawalAccountNo} onChange={e => setForm(p => ({ ...p, withdrawalAccountNo: e.target.value }))} required /></div>
          <div className="field"><label>출금일</label><input value={form.withdrawalDate} onChange={e => setForm(p => ({ ...p, withdrawalDate: e.target.value }))} /></div>
          <div className="field">
            <label>카드 타입</label>
            <select value={form.cardTypeCode} onChange={e => setForm(p => ({ ...p, cardTypeCode: e.target.value }))}>
              <option value="1">체크카드</option>
              <option value="2">신용카드</option>
            </select>
          </div>
          <div className="field full"><button type="submit" className="btn-sm btn-primary">등록</button></div>
        </form>
      </div>
    </div>
  )
}

function SettingsPanel({ g, setG }) {
  const toast = useToast()
  const [loginForm, setLoginForm] = useState({ userId: '', password: '' })
  const [loginResult, setLoginResult] = useState(null)
  const [error, setError] = useState('')
  const [tokenInput, setTokenInput] = useState(api.getToken())

  async function handleLogin(e) {
    e.preventDefault(); setError('')
    try {
      const res = await api.login(loginForm.userId, loginForm.password)
      setLoginResult(res)
      if (res.userNo) setG(p => ({ ...p, userNo: String(res.userNo) }))
    } catch (e) { setError(e.message) }
  }

  function saveToken() {
    api.setToken(tokenInput)
    toast.success('토큰이 저장되었습니다')
  }

  return (
    <div className="panel">
      <div className="panel-header"><h3>시스템 설정</h3></div>
      {error && <div className="error-msg">{error}</div>}

      <div className="sub-section">
        <h4>로그인</h4>
        <form className="inline-form" onSubmit={handleLogin}>
          <div className="field compact"><label>아이디</label><input value={loginForm.userId} onChange={e => setLoginForm(p => ({ ...p, userId: e.target.value }))} placeholder="user@example.com" required /></div>
          <div className="field compact"><label>비밀번호</label><input type="password" value={loginForm.password} onChange={e => setLoginForm(p => ({ ...p, password: e.target.value }))} required /></div>
          <button type="submit" className="btn-sm btn-primary">로그인</button>
        </form>
        {loginResult && (
          <div className="success-box">
            로그인 성공! userNo: <strong>{loginResult.userNo}</strong> / 이름: {loginResult.username}
            <br /><span className="mono" style={{ fontSize: 11 }}>Token: {loginResult.accessToken?.slice(0, 40)}...</span>
          </div>
        )}
      </div>

      <div className="sub-section">
        <h4>JWT 토큰 직접 설정</h4>
        <div className="inline-form">
          <input className="wide-input" value={tokenInput} onChange={e => setTokenInput(e.target.value)} placeholder="Bearer token" />
          <button className="btn-sm btn-primary" onClick={saveToken}>저장</button>
          <button className="btn-sm btn-danger" onClick={() => { api.clearToken(); setTokenInput('') }}>삭제</button>
        </div>
      </div>

      <div className="sub-section">
        <h4>현재 설정 값</h4>
        <div className="info-grid">
          <div className="info-item"><span className="info-label">Global UserNo</span><span>{g.userNo || '(미설정)'}</span></div>
          <div className="info-item"><span className="info-label">Global StoreId</span><span>{g.storeId || '(미설정)'}</span></div>
          <div className="info-item"><span className="info-label">Token</span><span className="mono">{api.getToken() ? `${api.getToken().slice(0, 30)}...` : '(없음)'}</span></div>
        </div>
      </div>
    </div>
  )
}


// ── 메인 Admin 컴포넌트 ──
const MENU = [
  { id: 'dashboard', label: '대시보드', icon: '📊' },
  { id: 'settings', label: '로그인/설정', icon: '⚙️' },
  { id: 'user', label: '회원 관리', icon: '👤' },
  { id: 'account', label: '계좌/이체', icon: '🏦' },
  { id: 'card', label: '카드', icon: '💳' },
  { id: 'store', label: '매장 관리', icon: '🏪' },
  { id: 'payment', label: '결제 관리', icon: '💰' },
  { id: 'point', label: '포인트', icon: '⭐' },
  { id: 'product', label: '포인트 상품', icon: '🎁' },
  { id: 'transaction', label: '거래내역', icon: '📋' },
  { id: 'report', label: '소비 리포트', icon: '📈' },
  { id: 'notification', label: '알림', icon: '🔔' },
  { id: 'festival', label: '축제/이벤트', icon: '🎉' },
  { id: 'monitoring', label: '모니터링', icon: '🛡️' },
]

export default function AdminPage({ onGoPOS }) {
  const [active, setActive] = useState('dashboard')
  const [collapsed, setCollapsed] = useState(false)
  const [globals, setGlobals] = useState({
    userNo: localStorage.getItem('admin_userNo') || '',
    storeId: localStorage.getItem('admin_storeId') || '',
  })

  const updateGlobals = useCallback((updater) => {
    setGlobals(prev => {
      const next = typeof updater === 'function' ? updater(prev) : updater
      localStorage.setItem('admin_userNo', next.userNo)
      localStorage.setItem('admin_storeId', next.storeId)
      return next
    })
  }, [])

  function renderPanel() {
    switch (active) {
      case 'dashboard': return <DashboardPanel g={globals} onNavigate={setActive} />
      case 'settings': return <SettingsPanel g={globals} setG={updateGlobals} />
      case 'user': return <UserPanel g={globals} />
      case 'account': return <AccountPanel g={globals} />
      case 'card': return <CardPanel g={globals} />
      case 'store': return <StorePanel g={globals} />
      case 'payment': return <PaymentPanel g={globals} />
      case 'point': return <PointPanel g={globals} />
      case 'product': return <ProductPanel />
      case 'transaction': return <TransactionPanel g={globals} />
      case 'report': return <ReportPanel g={globals} />
      case 'notification': return <NotificationPanel g={globals} />
      case 'festival': return <FestivalPanel />
      case 'monitoring': return <MonitoringPanel g={globals} />
      default: return null
    }
  }

  const hasToken = !!api.getToken()

  return (
    <ToastProvider>
      <div className="admin">
        <div className="admin-topbar">
          <div className="admin-topbar-left">
            <span className="admin-logo">NAEDA</span>
            <span className="admin-subtitle">Admin Console</span>
          </div>
          <div className="admin-topbar-center">
            <div className="global-field">
              <label>UserNo</label>
              <input
                type="number"
                value={globals.userNo}
                onChange={e => updateGlobals(p => ({ ...p, userNo: e.target.value }))}
                placeholder="1"
              />
            </div>
            <div className="global-field">
              <label>StoreId</label>
              <input
                type="number"
                value={globals.storeId}
                onChange={e => updateGlobals(p => ({ ...p, storeId: e.target.value }))}
                placeholder="1"
              />
            </div>
            <div className="global-field" style={{ cursor: 'pointer' }} onClick={() => setActive('settings')}>
              <label>Token</label>
              <span style={{ display: 'flex', alignItems: 'center', padding: '5px 8px', fontSize: 12, color: 'rgba(255,255,255,0.5)' }}>
                <span className={`token-dot ${hasToken ? 'active' : 'inactive'}`} />
                {hasToken ? 'Active' : 'None'}
              </span>
            </div>
          </div>
          <div className="admin-topbar-right">
            <button className="btn-go-pos" onClick={onGoPOS}>POS 화면 →</button>
          </div>
        </div>

        <div className="admin-body">
          <nav className={`admin-sidebar ${collapsed ? 'collapsed' : ''}`}>
            <button className="sidebar-toggle" onClick={() => setCollapsed(p => !p)}>
              {collapsed ? '▶' : '◀'}
            </button>
            {MENU.map(m => (
              <button
                key={m.id}
                className={`sidebar-item ${active === m.id ? 'active' : ''}`}
                onClick={() => setActive(m.id)}
                title={m.label}
              >
                <span className="sidebar-icon">{m.icon}</span>
                {!collapsed && <span className="sidebar-label">{m.label}</span>}
              </button>
            ))}
          </nav>
          <main className="admin-content" key={active}>
            {renderPanel()}
          </main>
        </div>
      </div>
    </ToastProvider>
  )
}
