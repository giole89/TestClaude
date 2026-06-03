import { useState } from 'react'
import { useScreener } from '@/hooks/useScreener'
import { useAppStore } from '@/store/useAppStore'
import { usePortfolio } from '@/hooks/usePortfolio'
import { formatNumber, formatPct, formatLargeNumber } from '@/lib/formatters'

const MARKETS = [
  { value: '', label: 'Tutti' },
  { value: 'us', label: 'USA' },
  { value: 'it', label: 'Italia' },
  { value: 'de', label: 'Germania' },
  { value: 'fr', label: 'Francia' },
]

function RangeBar({ position }: { position: number | null }) {
  if (position == null) return <span style={{ color: 'var(--muted)', fontFamily: 'JetBrains Mono', fontSize: 10 }}>—</span>
  const pct = Math.max(0, Math.min(100, position))
  const color = pct < 30 ? 'var(--red)' : pct > 70 ? 'var(--acc)' : '#f59e0b'
  return (
    <div style={{ position: 'relative', width: 60 }}>
      <div style={{ height: 4, background: 'var(--s3)', borderRadius: 2, overflow: 'hidden' }}>
        <div style={{ height: '100%', width: `${pct}%`, background: color, borderRadius: 2 }} />
      </div>
      <div style={{ fontFamily: 'JetBrains Mono', fontSize: 9, color: 'var(--muted)', textAlign: 'center', marginTop: 1 }}>
        {Math.round(pct)}%
      </div>
    </div>
  )
}

export function ScreenerPage() {
  const { setActiveTab } = useAppStore()
  const { add } = usePortfolio()
  const [maxPE, setMaxPE] = useState('')
  const [minYield, setMinYield] = useState('')
  const [minYtd, setMinYtd] = useState('')
  const [market, setMarket] = useState('')
  const [applied, setApplied] = useState(false)

  const filters = {
    maxPE: maxPE ? parseFloat(maxPE) : undefined,
    minYield: minYield ? parseFloat(minYield) : undefined,
    minYtd: minYtd ? parseFloat(minYtd) : undefined,
    market: market || undefined,
    limit: 50,
  }

  const { data, isLoading, isFetching } = useScreener(filters, applied)

  const handleAnalyze = (ticker: string) => {
    sessionStorage.setItem('finai_analyze_ticker', ticker)
    setActiveTab('analyze')
  }

  const handleAddToPortfolio = async (item: { ticker: string; name: string; price: number | null }) => {
    if (!item.price) return
    try {
      await add({
        id: `${item.ticker}-${Date.now()}`,
        ticker: item.ticker,
        name: item.name,
        qty: 1,
        loadPrice: item.price,
        currency: 'USD',
      })
    } catch (e) {
      console.error('Errore aggiunta portafoglio', e)
    }
  }

  const inputStyle = {
    background: 'var(--s3)', border: '1px solid var(--border)', borderRadius: 8,
    padding: '8px 12px', color: 'var(--text)', fontFamily: 'JetBrains Mono',
    fontSize: 13, outline: 'none',
  }

  return (
    <div style={{ padding: 24, overflowY: 'auto', height: 'calc(100vh - 112px)' }}>
      {/* Filtri */}
      <div style={{ background: 'var(--s2)', border: '1px solid var(--border)', borderRadius: 12, padding: '16px 20px', marginBottom: 16 }}>
        <div style={{ fontFamily: 'Syne', fontWeight: 700, fontSize: 14, color: 'var(--text)', marginBottom: 12 }}>
          Screener Azionario
        </div>
        <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap', alignItems: 'flex-end' }}>
          {/* Mercato */}
          <div>
            <div style={{ fontFamily: 'Syne', fontSize: 11, color: 'var(--muted)', marginBottom: 4 }}>Mercato</div>
            <select
              value={market}
              onChange={e => setMarket(e.target.value)}
              style={{ ...inputStyle, width: 120 }}
            >
              {MARKETS.map(m => <option key={m.value} value={m.value}>{m.label}</option>)}
            </select>
          </div>

          {/* P/E max */}
          <div>
            <div style={{ fontFamily: 'Syne', fontSize: 11, color: 'var(--muted)', marginBottom: 4 }}>P/E max</div>
            <input
              type="number"
              value={maxPE}
              onChange={e => setMaxPE(e.target.value)}
              placeholder="es. 30"
              style={{ ...inputStyle, width: 100 }}
            />
          </div>

          {/* Yield min */}
          <div>
            <div style={{ fontFamily: 'Syne', fontSize: 11, color: 'var(--muted)', marginBottom: 4 }}>Yield min %</div>
            <input
              type="number"
              value={minYield}
              onChange={e => setMinYield(e.target.value)}
              placeholder="es. 2.0"
              style={{ ...inputStyle, width: 100 }}
            />
          </div>

          {/* YTD min */}
          <div>
            <div style={{ fontFamily: 'Syne', fontSize: 11, color: 'var(--muted)', marginBottom: 4 }}>YTD min %</div>
            <input
              type="number"
              value={minYtd}
              onChange={e => setMinYtd(e.target.value)}
              placeholder="es. 10"
              style={{ ...inputStyle, width: 100 }}
            />
          </div>

          <button
            onClick={() => setApplied(true)}
            style={{
              padding: '9px 20px', borderRadius: 8, background: 'var(--acc)',
              border: 'none', color: '#07080a', fontFamily: 'Syne', fontWeight: 700,
              fontSize: 13, cursor: 'pointer',
            }}
          >
            {isFetching ? '⟳ Caricamento…' : '🔍 Applica filtri'}
          </button>

          {applied && (
            <button
              onClick={() => { setApplied(false); setMaxPE(''); setMinYield(''); setMinYtd(''); setMarket('') }}
              style={{
                padding: '9px 14px', borderRadius: 8, background: 'var(--s3)',
                border: '1px solid var(--border)', color: 'var(--muted)',
                fontFamily: 'Syne', fontSize: 12, cursor: 'pointer',
              }}
            >
              Reset
            </button>
          )}
        </div>
      </div>

      {/* Risultati */}
      {isLoading && applied && (
        <div style={{ textAlign: 'center', padding: 40, color: 'var(--muted)', fontFamily: 'Syne' }}>
          Caricamento dati screener…
        </div>
      )}

      {!applied && (
        <div style={{ textAlign: 'center', padding: 60, color: 'var(--muted)', fontFamily: 'Syne', fontSize: 15 }}>
          Imposta i filtri e premi "Applica filtri" per avviare lo screening
        </div>
      )}

      {applied && !isLoading && data && (
        <>
          <div style={{ fontFamily: 'Syne', fontSize: 12, color: 'var(--muted)', marginBottom: 10 }}>
            {data.length} risultati trovati
          </div>
          <div style={{ background: 'var(--s2)', border: '1px solid var(--border)', borderRadius: 12, overflow: 'hidden' }}>
            {/* Header */}
            <div style={{
              display: 'grid',
              gridTemplateColumns: '80px 1fr 80px 60px 70px 80px 70px 80px',
              padding: '10px 16px', background: 'var(--s3)',
              borderBottom: '1px solid var(--border)',
              fontFamily: 'Syne', fontSize: 11, color: 'var(--muted)', fontWeight: 700,
            }}>
              {['Ticker', 'Nome', 'Prezzo', 'P/E', 'YTD%', 'Mkt Cap', '52w', 'Azioni'].map((h, i) => (
                <div key={h} style={{ textAlign: i <= 1 ? 'left' : 'right', paddingRight: 8 }}>{h}</div>
              ))}
            </div>

            {data.map(item => (
              <div key={item.ticker} style={{
                display: 'grid',
                gridTemplateColumns: '80px 1fr 80px 60px 70px 80px 70px 80px',
                padding: '10px 16px', borderBottom: '1px solid var(--border)',
                alignItems: 'center',
              }}>
                <button
                  onClick={() => handleAnalyze(item.ticker)}
                  style={{ background: 'none', border: 'none', cursor: 'pointer', fontFamily: 'JetBrains Mono', fontWeight: 700, fontSize: 13, color: 'var(--acc)', padding: 0, textAlign: 'left' }}
                >
                  {item.ticker}
                </button>
                <div style={{ fontFamily: 'Syne', fontSize: 11, color: 'var(--muted)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', paddingRight: 8 }}>
                  {item.name}
                </div>
                <div style={{ fontFamily: 'JetBrains Mono', fontSize: 12, color: 'var(--text)', textAlign: 'right', paddingRight: 8 }}>
                  {item.price ? formatNumber(item.price, 2) : '—'}
                </div>
                <div style={{ fontFamily: 'JetBrains Mono', fontSize: 12, color: 'var(--muted2)', textAlign: 'right', paddingRight: 8 }}>
                  {item.pe ? formatNumber(item.pe, 1) + 'x' : '—'}
                </div>
                <div style={{ fontFamily: 'JetBrains Mono', fontSize: 12, color: (item.ytdChangePct ?? 0) >= 0 ? 'var(--acc)' : 'var(--red)', textAlign: 'right', paddingRight: 8 }}>
                  {item.ytdChangePct != null ? formatPct(item.ytdChangePct) : '—'}
                </div>
                <div style={{ fontFamily: 'JetBrains Mono', fontSize: 11, color: 'var(--muted2)', textAlign: 'right', paddingRight: 8 }}>
                  {item.marketCap ? formatLargeNumber(item.marketCap) : '—'}
                </div>
                <div style={{ textAlign: 'right', paddingRight: 8 }}>
                  <RangeBar position={item.rangePosition} />
                </div>
                <div style={{ display: 'flex', gap: 4, justifyContent: 'flex-end' }}>
                  <button
                    onClick={() => handleAnalyze(item.ticker)}
                    title="Analizza"
                    style={{ background: 'var(--s3)', border: '1px solid var(--border)', borderRadius: 6, padding: '4px 8px', color: 'var(--acc)', cursor: 'pointer', fontSize: 11, fontFamily: 'Syne' }}
                  >
                    📊
                  </button>
                  <button
                    onClick={() => handleAddToPortfolio(item)}
                    title="Aggiungi a portafoglio"
                    disabled={!item.price}
                    style={{ background: 'var(--s3)', border: '1px solid var(--border)', borderRadius: 6, padding: '4px 8px', color: 'var(--muted2)', cursor: item.price ? 'pointer' : 'default', fontSize: 11, fontFamily: 'Syne', opacity: item.price ? 1 : 0.4 }}
                  >
                    +
                  </button>
                </div>
              </div>
            ))}

            {data.length === 0 && (
              <div style={{ padding: 40, textAlign: 'center', color: 'var(--muted)', fontFamily: 'Syne' }}>
                Nessun risultato con i filtri applicati.
              </div>
            )}
          </div>
        </>
      )}
    </div>
  )
}
