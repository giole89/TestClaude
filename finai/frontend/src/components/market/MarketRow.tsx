import { QuoteData } from '@/hooks/useQuote'
import { formatNumber, formatPct } from '@/lib/formatters'
import { useAppStore } from '@/store/useAppStore'

interface Props {
  rank: number
  quote: QuoteData
  mode: 'today' | 'ytd'
  onAnalyze?: (ticker: string) => void
}

export function MarketRow({ rank, quote, mode, onAnalyze }: Props) {
  const { setActiveTab } = useAppStore()
  const pct = mode === 'today' ? quote.dayChangePct : quote.ytdChangePct
  const isPos = pct >= 0

  const handleClick = () => {
    if (onAnalyze) onAnalyze(quote.ticker)
    setActiveTab('analyze')
  }

  return (
    <div
      onClick={handleClick}
      style={{
        display: 'flex', alignItems: 'center', gap: 8,
        padding: '8px 12px', borderRadius: 8,
        cursor: 'pointer', transition: 'background 0.15s',
      }}
      onMouseEnter={e => (e.currentTarget.style.background = 'var(--s3)')}
      onMouseLeave={e => (e.currentTarget.style.background = 'transparent')}
    >
      <span style={{ fontFamily: 'JetBrains Mono', fontSize: 11, color: 'var(--muted)', width: 20, textAlign: 'right' }}>
        {rank}
      </span>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ fontFamily: 'Syne', fontWeight: 700, fontSize: 13, color: 'var(--text)' }}>
          {quote.ticker}
        </div>
        <div style={{
          fontFamily: 'Syne', fontSize: 11, color: 'var(--muted)',
          overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap',
        }}>
          {quote.name}
        </div>
      </div>
      <span style={{ fontFamily: 'JetBrains Mono', fontSize: 12, color: 'var(--text)' }}>
        {formatNumber(quote.price, 2)}
      </span>
      <span style={{
        fontFamily: 'JetBrains Mono', fontSize: 12, fontWeight: 700,
        color: isPos ? 'var(--acc)' : 'var(--red)',
        minWidth: 64, textAlign: 'right',
      }}>
        {formatPct(pct)}
      </span>

      <div style={{ width: 60, height: 4, background: 'var(--s3)', borderRadius: 2, overflow: 'hidden' }}>
        <div style={{
          height: '100%',
          width: `${Math.min(100, Math.abs(pct) * 5)}%`,
          background: isPos ? 'var(--acc)' : 'var(--red)',
          borderRadius: 2,
        }} />
      </div>

      <div style={{ width: 40 }}>
        <div style={{
          height: 4, background: 'var(--s3)', borderRadius: 2, overflow: 'hidden', marginBottom: 2,
        }}>
          <div style={{
            height: '100%',
            width: `${quote.rangePosition}%`,
            background: quote.rangePosition > 70 ? 'var(--acc3)' : 'var(--acc2)',
            borderRadius: 2,
          }} />
        </div>
        <div style={{ fontFamily: 'JetBrains Mono', fontSize: 9, color: 'var(--muted)', textAlign: 'center' }}>
          {quote.rangePosition}%
        </div>
      </div>
    </div>
  )
}
