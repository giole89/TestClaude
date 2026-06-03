import { useState } from 'react'
import { useQuote } from '@/hooks/useQuote'
import { formatNumber, formatPct } from '@/lib/formatters'

interface DcaResult {
  totalShares: number
  avgCost: number
  finalValue: number
  gainPct: number
  totalInvested: number
}

export function DcaSimulator() {
  const [ticker, setTicker] = useState('')
  const [monthly, setMonthly] = useState('')
  const [months, setMonths] = useState('')
  const [result, setResult] = useState<DcaResult | null>(null)

  const { data: quote } = useQuote(ticker || null)

  const simulate = () => {
    const m = parseFloat(monthly)
    const n = parseInt(months)
    if (!quote || !quote.price || isNaN(m) || isNaN(n) || m <= 0 || n <= 0) return

    const currentPrice = quote.price
    // Simulazione semplificata: si assume prezzo costante (worst case / illustrativo)
    // Per una simulazione reale servirebbero i dati storici mensili
    let totalShares = 0
    let totalInvested = 0

    for (let i = 0; i < n; i++) {
      // Variazione casuale del prezzo (±5% per rendere la simulazione più realistica)
      // In produzione si userebbe useHistory per i prezzi storici mensili
      const simulatedPrice = currentPrice * (1 + (Math.random() - 0.5) * 0.1)
      totalShares += m / simulatedPrice
      totalInvested += m
    }

    const avgCost = totalInvested / totalShares
    const finalValue = totalShares * currentPrice
    const gainPct = ((finalValue - totalInvested) / totalInvested) * 100

    setResult({ totalShares, avgCost, finalValue, gainPct, totalInvested })
  }

  const inputStyle = {
    background: 'var(--s3)', border: '1px solid var(--border)', borderRadius: 8,
    padding: '10px 12px', color: 'var(--text)', fontFamily: 'JetBrains Mono',
    fontSize: 13, outline: 'none', width: '100%',
  }

  const labelStyle = {
    fontFamily: 'Syne', fontSize: 11, color: 'var(--muted)', marginBottom: 4, display: 'block',
  }

  return (
    <div style={{ background: 'var(--s2)', border: '1px solid var(--border)', borderRadius: 12, padding: '16px 20px' }}>
      <div style={{ fontFamily: 'Syne', fontWeight: 700, fontSize: 13, color: 'var(--text)', marginBottom: 16 }}>
        DCA Simulator
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 12, marginBottom: 12 }}>
        <div>
          <label style={labelStyle}>Ticker</label>
          <input
            value={ticker}
            onChange={e => setTicker(e.target.value.toUpperCase())}
            placeholder="es. AAPL"
            style={inputStyle}
          />
        </div>
        <div>
          <label style={labelStyle}>Investimento mensile (€)</label>
          <input
            type="number"
            value={monthly}
            onChange={e => setMonthly(e.target.value)}
            placeholder="es. 200"
            style={inputStyle}
          />
        </div>
        <div>
          <label style={labelStyle}>Numero di mesi</label>
          <input
            type="number"
            value={months}
            onChange={e => setMonths(e.target.value)}
            placeholder="es. 24"
            style={inputStyle}
          />
        </div>
      </div>

      {quote && (
        <div style={{ fontFamily: 'Syne', fontSize: 12, color: 'var(--muted)', marginBottom: 10 }}>
          Prezzo attuale <span style={{ color: 'var(--acc)', fontFamily: 'JetBrains Mono' }}>
            {formatNumber(quote.price, 2)}
          </span> {quote.currency}
        </div>
      )}

      <button
        onClick={simulate}
        disabled={!quote || !monthly || !months}
        style={{
          padding: '10px 20px', borderRadius: 8, background: 'var(--acc)',
          border: 'none', color: '#07080a', fontFamily: 'Syne', fontWeight: 700,
          fontSize: 13, cursor: !quote || !monthly || !months ? 'default' : 'pointer',
          opacity: !quote || !monthly || !months ? 0.5 : 1,
        }}
      >
        Simula DCA
      </button>

      {result && (
        <div style={{ marginTop: 16, display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 10 }}>
          {[
            { label: 'Quote accumulate', value: formatNumber(result.totalShares, 4), color: 'var(--text)' },
            { label: 'Prezzo medio carico', value: formatNumber(result.avgCost, 2), color: 'var(--text)' },
            { label: 'Valore finale stimato', value: formatNumber(result.finalValue, 2), color: 'var(--acc)' },
            {
              label: 'Gain stimato %',
              value: formatPct(result.gainPct),
              color: result.gainPct >= 0 ? 'var(--acc)' : 'var(--red)',
            },
          ].map(s => (
            <div key={s.label} style={{ background: 'var(--s3)', borderRadius: 8, padding: '12px 14px', textAlign: 'center' }}>
              <div style={{ fontFamily: 'Syne', fontSize: 10, color: 'var(--muted)', marginBottom: 6 }}>{s.label}</div>
              <div style={{ fontFamily: 'Instrument Serif', fontSize: 18, color: s.color }}>{s.value}</div>
            </div>
          ))}
          <div style={{ gridColumn: '1 / -1', fontFamily: 'Syne', fontSize: 10, color: 'var(--muted)', marginTop: 4 }}>
            * Simulazione con variazione prezzo stocastica ±10%. Risultati indicativi, non finanziari.
          </div>
        </div>
      )}
    </div>
  )
}
