const STATUS_CONFIG = {
  PENDING: {
    icon: null,
    title: '결제 대기 중',
    message: '고객이 얼굴 인증을 진행해주세요.',
    color: '#00635A',
    showSpinner: true,
  },
  PROCESSING: {
    icon: null,
    title: '결제 처리 중',
    message: '얼굴 인증 진행 중...',
    color: '#e6a817',
    showSpinner: true,
  },
  SUCCESS: {
    icon: '✔',
    title: '결제 완료',
    message: '결제가 성공적으로 완료되었습니다.',
    color: '#00835A',
  },
  FAILED: {
    icon: '✖',
    title: '결제 실패',
    message: '',
    color: '#e74c3c',
  },
  BLOCKED: {
    icon: 'X',
    title: '결제 차단',
    message: '이상거래 탐지로 결제가 차단되었습니다.',
    color: '#e74c3c',
  },
  PAUSED: {
    icon: '⏸',
    title: 'FDS 심사 보류',
    message: '이상거래 심사 중입니다. 잠시 후 재시도됩니다.',
    color: '#f59e0b',
    showSpinner: true,
  },
  EXPIRED: {
    icon: '-',
    title: '요청 만료',
    message: '결제 요청 시간이 초과되었습니다.',
    color: '#999',
  },
}

export default function PaymentStatus({ payment, onClose }) {
  const config = STATUS_CONFIG[payment.status] || STATUS_CONFIG.PENDING
  const isTerminal = ['SUCCESS', 'FAILED', 'BLOCKED', 'EXPIRED'].includes(payment.status)
  const formattedAmount = Number(payment.amount).toLocaleString('ko-KR')

  return (
    <div className="status-overlay">
      <div className="status-card">
        {config.showSpinner ? (
          <div className="spinner" />
        ) : (
          <div className="status-icon" style={{ color: config.color }}>
            {config.icon}
          </div>
        )}

        <h2 style={{ color: config.color }}>{config.title}</h2>
        <p className="status-msg">
          {payment.failureReason || config.message}
        </p>

        <div className="status-amount" style={{ color: config.color }}>
          {formattedAmount}<span style={{ fontSize: '18px', color: '#bbb' }}> 원</span>
        </div>

        {payment.status === 'SUCCESS' && payment.transactionId && (
          <p className="status-detail">거래 번호: {payment.transactionId}</p>
        )}

        {payment.status === 'PAUSED' && (
          <p className="pulse" style={{ color: '#f59e0b', fontSize: '13px', marginBottom: '16px' }}>
            FDS 이상거래 심사 대기 중...
          </p>
        )}

        {!isTerminal && payment.status !== 'PAUSED' && (
          <p className="pulse" style={{ color: '#bbb', fontSize: '13px', marginBottom: '16px' }}>
            고객 얼굴 인증 대기 중...
          </p>
        )}

        {isTerminal ? (
          <button className="btn-ok" onClick={onClose}>확인</button>
        ) : (
          <button className="btn-cancel-status" onClick={onClose}>취소</button>
        )}
      </div>
    </div>
  )
}
