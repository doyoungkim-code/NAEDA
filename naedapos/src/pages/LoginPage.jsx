import { useState } from 'react'
import { login } from '../api'

export default function LoginPage({ onLogin }) {
  const [userId, setUserId] = useState('')
  const [password, setPassword] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  async function handleSubmit(e) {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      await login(userId, password)
      onLogin()
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="login-container">
      <h1>NAEDA POS</h1>
      <p className="subtitle">매장 포스기 로그인</p>
      <form onSubmit={handleSubmit}>
        <div className="form-group">
          <label>아이디 (이메일)</label>
          <input
            type="text"
            value={userId}
            onChange={e => setUserId(e.target.value)}
            placeholder="store@example.com"
            required
          />
        </div>
        <div className="form-group">
          <label>비밀번호</label>
          <input
            type="password"
            value={password}
            onChange={e => setPassword(e.target.value)}
            placeholder="비밀번호 입력"
            required
          />
        </div>
        <button type="submit" className="btn btn-primary" disabled={loading}>
          {loading ? '로그인 중...' : '로그인'}
        </button>
        {error && <p className="error-msg">{error}</p>}
      </form>
    </div>
  )
}
