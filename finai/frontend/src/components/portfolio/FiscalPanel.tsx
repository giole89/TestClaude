import { formatNumber, formatPct } from '@/lib/formatters'

interface PortfolioItem {
  ticker: string
  name: string
  qty: number
  loadPrice: number
  currentPrice?: number
}

interface FiscalPanelProps {
  items: PortfolioItem[]
}

const TAX_RATE = 0.26

function calcGain(item: PortfolioItem) {
  const cp = item.currentPrice ?? item.loadPrice
  const grossGain = (cp - item.loadPrice) * item.qty
  const tax = grossGain > 0 ? grossGain * TAX_RATE : 0
  const netGain = grossGain - tax
  const gainPct = ((cp - item.loadPrice) / item.loadPrice) * 100
  return { grossGain, tax, netGain, gainPct }
}

export function FiscalPanel({ items }: FiscalPanelProps) {
  if (items.length === 0) return null

  const positions = items.map(item => ({
    ...item,
    ...calcGain(item),
  }))

  const totalGross = positions.reduce((s, p) => s + p.grossGain, 0)
  const totalTax = positions.reduce((s, p) => s + p.tax, 0)
  const totalNet = positions.reduce((s, p) => s + p.netGain, 0)

  return (
    <div style={{ background: 'var(--s2)', border: '1px solid var(--border)', borderRadius: 12, padding: '16px 20px' }}>
      <div style={{ fontFamily: 'Syne', fontWeight: 700, fontSize: 13, color: 'var(--text)', marginBottom: 12 }}>
        Gain Fiscale (Italia 26%)
      </div>

      {/* Header */}
      <div style={{
        display: 'grid', gridTemplateColumns: '80px 1fr 80px 80px 80px',
        gap: 0, padding: '8px 12px', background: 'var(--s3)',
        borderRadius: '8px 8px 0 0', borderBottom: '1px solid var(--border)',
        fontFamily: 'Syne', fontSize: 11, color: 'var(--muted)', fontWeight: 700,
      }}>
        {['Ticker', 'Nome', 'Gain lordo', 'Imposta 26%', 'Gain netto'].map(h => (
          <div key={h} style={{ textAlign: h === 'Ticker' || h === 'Nome' ? 'left' : 'right', paddingRight: 8 }}>{h}</div>
        ))}
      </div>

      {/* Rows */}
      {positions.map(p => (
        <div key={p.ticker} style={{
          display: 'grid', gridTemplateColumns: '80px 1fr 80px 80px 80px',
          gap: 0, padding: '8px 12px',
          borderBottom: '1px solid var(--border)',
          alignItems: 'center',
        }}>
          <div style={{ fontFamily: 'JetBrains Mono', fontWeight: 700, fontSize: 12, color: 'var(--acc)' }}>{p.ticker}</div>
          <div style={{ fontFamily: 'Syne', fontSize: 11, color: 'var(--muted)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', paddingRight: 8 }}>{p.name}</div>
          <div style={{ fontFamily: 'JetBrains Mono', fontSize: 12, color: p.grossGain >= 0 ? 'var(--acc)' : 'var(--red)', textAlign: 'right', paddingRight: 8 }}>
            {(p.grossGain >= 0 ? '+' : '') + formatNumber(p.grossGain, 2)}
          </div>
          <div style={{ fontFamily: 'JetBrains Mono', fontSize: 12, color: p.tax > 0 ? 'var(--red)' : 'var(--muted)', textAlign: 'right', paddingRight: 8 }}>
            {p.tax > 0 ? '-' + formatNumber(p.tax, 2) : '—'}
          </div>
          <div style={{ fontFamily: 'JetBrains Mono', fontSize: 12, color: p.netGain >= 0 ? 'var(--acc)' : 'var(--red)', textAlign: 'right', paddingRight: 8 }}>
            {(p.netGain >= 0 ? '+' : '') + formatNumber(p.netGain, 2)}
          </div>
        </div>
      ))}

      {/* Totale */}
      <div style={{
        display: 'grid', gridTemplateColumns: '80px 1fr 80px 80px 80px',
        gap: 0, padding: '10px 12px',
        background: 'var(--s3)', borderRadius: '0 0 8px 8px',
        fontWeight: 700,
      }}>
        <div style={{ fontFamily: 'Syne', fontSize: 12, color: 'var(--text)', gridColumn: '1/3' }}>TOTALE</div>
        <div style={{ fontFamily: 'JetBrains Mono', fontSize: 12, color: totalGross >= 0 ? 'var(--acc)' : 'var(--red)', textAlign: 'right', paddingRight: 8 }}>
          {(totalGross >= 0 ? '+' : '') + formatNumber(totalGross, 2)}
        </div>
        <div style={{ fontFamily: 'JetBrains Mono', fontSize: 12, color: 'var(--red)', textAlign: 'right', paddingRight: 8 }}>
          {totalTax > 0 ? '-' + formatNumber(totalTax, 2) : '—'}
        </div>
        <div style={{ fontFamily: 'JetBrains Mono', fontSize: 12, color: totalNet >= 0 ? 'var(--acc)' : 'var(--red)', textAlign: 'right', paddingRight: 8 }}>
          {(totalNet >= 0 ? '+' : '') + formatNumber(totalNet, 2)}
        </div>
      </div>

      <div style={{ marginTop: 8, fontFamily: 'Syne', fontSize: 10, color: 'var(--muted)', fontStyle: 'italic' }}>
        ⚠️ Calcolo indicativo (aliquota 26% su plusvalenze lorde). L'imposta si applica solo su posizioni in gain. Consultare un professionista fiscale per il calcolo definitivo.
      </div>
    </div>
  )
}
