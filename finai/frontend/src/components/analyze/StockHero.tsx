import { FullQuoteData } from '@/hooks/useQuote'
import { formatNumber, formatPct } from '@/lib/formatters'

interface Props {
  data: FullQuoteData
}

export function StockHero({ data }: Props) {
  const isPos = data.dayChangePct >= 0

  return (
    <div style={{
      background: 'var(--s2)', border: '1px solid var(--border)',
      borderRadius: 12, padding: '20px 24px',
    }}>
      <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', marginBottom: 16 }}>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 4 }}>
            <span style={{ fontFamily: 'Syne', fontWeight: 800, fontSize: 22, color: 'var(--text)' }}>
              {data.ticker}
            </span>
            <span style={{
              padding: '2px 8px', borderRadius: 6,
              background: 'var(--s3)', border: '1px solid var(--border)',
              fontFamily: 'JetBrains Mono', fontSize: 11, color: 'var(--muted)',
            }}>
              {data.exchange}
            </span>
            <span style={{
              padding: '2px 8px', borderRadius: 6,
              background: 'var(--s3)', border: '1px solid var(--border)',
              fontFamily: 'JetBrains Mono', fontSize: 11, color: 'var(--muted)',
            }}>
              {data.currency}
            </span>
          </div>
          <div style={{ fontFamily: 'Syne', fontSize: 14, color: 'var(--muted)' }}>
            {data.name}
          </div>
        </div>
        <div style={{ textAlign: 'right' }}>
          <div style={{
            fontFamily: 'Instrument Serif', fontSize: 36, color: 'var(--text)', lineHeight: 1,
          }}>
            {formatNumber(data.price, 2)}
          </div>
          <div style={{ marginTop: 4 }}>
            <span style={{
              fontFamily: 'JetBrains Mono', fontSize: 14, fontWeight: 700,
              color: isPos ? 'var(--acc)' : 'var(--red)',
            }}>
              {isPos ? '+' : ''}{formatNumber(data.dayChange, 2)} ({formatPct(data.dayChangePct)})
            </span>
          </div>
        </div>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 12 }}>
        {[
          { label: 'RSI (14)', value: data.rsi, suffix: '', color: data.rsi > 70 ? 'var(--acc3)' : data.rsi < 30 ? 'var(--red)' : 'var(--acc2)' },
          { label: 'SMA 20', value: data.sma20, suffix: '', color: 'var(--text)' },
          { label: 'SMA 50', value: data.sma50, suffix: '', color: 'var(--text)' },
          { label: 'SMA 200', value: data.sma200, suffix: '', color: 'var(--text)' },
          { label: 'Volatilità', value: data.volatility, suffix: '%', color: data.volatility > 50 ? 'var(--red)' : data.volatility > 30 ? 'var(--acc3)' : 'var(--acc2)' },
          { label: 'Momentum 30gg', value: data.momentum30, suffix: '%', color: data.momentum30 > 0 ? 'var(--acc)' : 'var(--red)' },
          { label: 'Dal Max 52W', value: ((data.price - data.high52w) / data.high52w) * 100, suffix: '%', color: 'var(--muted2)' },
          { label: 'Dal Min 52W', value: ((data.price - data.low52w) / data.low52w) * 100, suffix: '%', color: 'var(--muted2)' },
        ].map(m => (
          <div key={m.label} style={{
            background: 'var(--s3)', borderRadius: 8, padding: '10px 12px',
          }}>
            <div style={{ fontFamily: 'Syne', fontSize: 11, color: 'var(--muted)', marginBottom: 4 }}>
              {m.label}
            </div>
            <div style={{
              fontFamily: 'JetBrains Mono', fontSize: 15, fontWeight: 700, color: m.color,
            }}>
              {m.value > 0 && m.suffix === '%' && m.label !== 'Volatilità' && m.label !== 'RSI (14)' ? '+' : ''}
              {formatNumber(m.value, 2)}{m.suffix}
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}
