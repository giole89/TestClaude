import { useIndices } from '@/hooks/useMarketBatch'
import { formatNumber, formatPct } from '@/lib/formatters'

const INDEX_NAMES: Record<string, string> = {
  '^GSPC': 'S&P 500',
  '^NDX': 'Nasdaq 100',
  '^DJI': 'Dow Jones',
  '^STOXX50E': 'Euro Stoxx 50',
  'FTSEMIB.MI': 'FTSE MIB',
  '^VIX': 'VIX',
  'EURUSD=X': 'EUR/USD',
  'GC=F': 'Oro',
  'CL=F': 'WTI Oil',
}

export function IndexBar() {
  const { data, isLoading } = useIndices()

  if (isLoading || !data) {
    return (
      <div style={{
        height: 48, background: 'var(--s2)',
        borderBottom: '1px solid var(--border)',
        display: 'flex', alignItems: 'center',
        padding: '0 16px', gap: 4, overflowX: 'auto',
      }}>
        {Array.from({ length: 9 }).map((_, i) => (
          <div key={i} style={{
            height: 28, width: 120, background: 'var(--s3)',
            borderRadius: 6, flexShrink: 0,
            animation: 'pulse 1.5s infinite',
          }} />
        ))}
      </div>
    )
  }

  return (
    <div style={{
      height: 48, background: 'var(--s2)',
      borderBottom: '1px solid var(--border)',
      display: 'flex', alignItems: 'center',
      padding: '0 8px', gap: 4, overflowX: 'auto',
    }}>
      {data.map(q => {
        const isPos = q.dayChangePct >= 0
        const name = INDEX_NAMES[q.ticker] || (q as unknown as Record<string, unknown>).displayName as string || q.ticker
        return (
          <div key={q.ticker} style={{
            display: 'flex', alignItems: 'center', gap: 8,
            padding: '4px 12px', borderRadius: 6,
            background: 'var(--s3)', flexShrink: 0,
            border: '1px solid var(--border)',
          }}>
            <span style={{ fontFamily: 'Syne', fontSize: 11, color: 'var(--muted2)', fontWeight: 600 }}>
              {name}
            </span>
            <span style={{ fontFamily: 'JetBrains Mono', fontSize: 12, color: 'var(--text)', fontWeight: 500 }}>
              {formatNumber(q.price, q.price < 10 ? 4 : 2)}
            </span>
            <span style={{
              fontFamily: 'JetBrains Mono', fontSize: 11, fontWeight: 600,
              color: isPos ? 'var(--acc)' : 'var(--red)',
            }}>
              {formatPct(q.dayChangePct)}
            </span>
          </div>
        )
      })}
    </div>
  )
}
