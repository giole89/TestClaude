import { useState } from 'react'
import { useWatchlist } from '@/hooks/useWatchlist'
import { useAppStore } from '@/store/useAppStore'
import { formatNumber, formatPct, colorForChange } from '@/lib/formatters'
import { useQuery } from '@tanstack/react-query'
import axios from 'axios'
import { API_BASE } from '@/lib/constants'
import { QuoteData } from '@/hooks/useQuote'

function useBatchPrices(tickers: string[]) {
  return useQuery<QuoteData[]>({
    queryKey: ['watchlistBatch', tickers.join(',')],
    queryFn: () =>
      axios.get(`${API_BASE}/api/batch`, { params: { tickers: tickers.join(',') } }).then(r => r.data),
    enabled: tickers.length > 0,
    staleTime: 60_000,
    refetchInterval: 60_000,
  })
}

export function WatchlistPage() {
  const { items, isLoading, add, remove, isAdding } = useWatchlist()
  const { setActiveTab } = useAppStore()
  const [tickerInput, setTickerInput] = useState('')
  const [nameInput, setNameInput] = useState('')
  const [targetInput, setTargetInput] = useState('')
  const [noteInput, setNoteInput] = useState('')

  const tickers = items.map(i => i.ticker)
  const { data: prices } = useBatchPrices(tickers)
  const priceMap = Object.fromEntries((prices ?? []).map(q => [q.ticker, q]))

  const handleAdd = async () => {
    const t = tickerInput.trim().toUpperCase()
    if (!t) return
    try {
      await add({
        id: `${t}-${Date.now()}`,
        ticker: t,
        name: nameInput || t,
        targetPrice: targetInput ? parseFloat(targetInput) : undefined,
        note: noteInput || undefined,
      })
      setTickerInput(''); setNameInput(''); setTargetInput(''); setNoteInput('')
    } catch (e: any) {
      console.error('Errore aggiunta watchlist:', e?.response?.data?.message || e.message)
    }
  }

  const handleAnalyze = (ticker: string) => {
    sessionStorage.setItem('finai_analyze_ticker', ticker)
    setActiveTab('analyze')
  }

  const inputStyle = {
    background: 'var(--s3)', border: '1px solid var(--border)', borderRadius: 8,
    padding: '10px 12px', color: 'var(--text)', fontFamily: 'JetBrains Mono',
    fontSize: 13, outline: 'none',
  }

  return (
    <div style={{ padding: 24, overflowY: 'auto', height: 'calc(100vh - 112px)' }}>
      {/* Form aggiunta */}
      <div style={{ background: 'var(--s2)', border: '1px solid var(--border)', borderRadius: 12, padding: '16px 20px', marginBottom: 16 }}>
        <div style={{ fontFamily: 'Syne', fontWeight: 700, fontSize: 14, color: 'var(--text)', marginBottom: 12 }}>
          Aggiungi alla Watchlist
        </div>
        <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', alignItems: 'center' }}>
          <input
            value={tickerInput}
            onChange={e => setTickerInput(e.target.value.toUpperCase())}
            placeholder="Ticker *"
            style={{ ...inputStyle, width: 110 }}
          />
          <input
            value={nameInput}
            onChange={e => setNameInput(e.target.value)}
            placeholder="Nome (opz.)"
            style={{ ...inputStyle, width: 180, fontFamily: 'Syne' }}
          />
          <input
            type="number"
            value={targetInput}
            onChange={e => setTargetInput(e.target.value)}
            placeholder="Target price (opz.)"
            style={{ ...inputStyle, width: 160 }}
          />
          <input
            value={noteInput}
            onChange={e => setNoteInput(e.target.value)}
            placeholder="Note (opz.)"
            style={{ ...inputStyle, width: 200, fontFamily: 'Syne' }}
          />
          <button
            onClick={handleAdd}
            disabled={!tickerInput.trim() || isAdding}
            style={{
              padding: '10px 20px', borderRadius: 8, background: 'var(--acc)',
              border: 'none', color: '#07080a', fontFamily: 'Syne', fontWeight: 700,
              fontSize: 13, cursor: !tickerInput.trim() ? 'default' : 'pointer',
              opacity: !tickerInput.trim() ? 0.5 : 1,
            }}
          >
            {isAdding ? '⟳' : '+ Aggiungi'}
          </button>
        </div>
      </div>

      {isLoading && (
        <div style={{ textAlign: 'center', padding: 40, color: 'var(--muted)', fontFamily: 'Syne' }}>
          Caricamento watchlist…
        </div>
      )}

      {!isLoading && items.length === 0 && (
        <div style={{ textAlign: 'center', padding: 60, color: 'var(--muted)', fontFamily: 'Syne', fontSize: 15 }}>
          La watchlist è vuota. Aggiungi il primo ticker.
        </div>
      )}

      {!isLoading && items.length > 0 && (
        <div style={{ background: 'var(--s2)', border: '1px solid var(--border)', borderRadius: 12, overflow: 'hidden' }}>
          {/* Header */}
          <div style={{
            display: 'grid',
            gridTemplateColumns: '80px 1fr 90px 80px 100px 80px 200px 80px',
            padding: '10px 16px', background: 'var(--s3)',
            borderBottom: '1px solid var(--border)',
            fontFamily: 'Syne', fontSize: 11, color: 'var(--muted)', fontWeight: 700,
          }}>
            {['Ticker', 'Nome', 'Prezzo', 'Variaz.', 'Target', '∆ Target', 'Note', ''].map((h, i) => (
              <div key={i} style={{ textAlign: i <= 1 ? 'left' : 'right', paddingRight: 8 }}>{h}</div>
            ))}
          </div>

          {items.map(item => {
            const q = priceMap[item.ticker]
            const price = q?.price
            const dayChangePct = q?.dayChangePct

            let targetDelta: number | null = null
            if (item.targetPrice != null && price != null) {
              targetDelta = ((item.targetPrice - price) / price) * 100
            }

            return (
              <div key={item.id} style={{
                display: 'grid',
                gridTemplateColumns: '80px 1fr 90px 80px 100px 80px 200px 80px',
                padding: '10px 16px', borderBottom: '1px solid var(--border)',
                alignItems: 'center',
              }}>
                <button
                  onClick={() => handleAnalyze(item.ticker)}
                  style={{ background: 'none', border: 'none', cursor: 'pointer', fontFamily: 'JetBrains Mono', fontWeight: 700, fontSize: 13, color: 'var(--acc)', padding: 0, textAlign: 'left' }}
                >
                  {item.ticker}
                </button>
                <div style={{ fontFamily: 'Syne', fontSize: 12, color: 'var(--muted)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', paddingRight: 8 }}>
                  {item.name}
                </div>
                <div style={{ fontFamily: 'JetBrains Mono', fontSize: 13, color: 'var(--text)', textAlign: 'right', paddingRight: 8 }}>
                  {price != null ? formatNumber(price, 2) : <span style={{ color: 'var(--muted)' }}>—</span>}
                </div>
                <div style={{ fontFamily: 'JetBrains Mono', fontSize: 12, color: dayChangePct != null ? colorForChange(dayChangePct) : 'var(--muted)', textAlign: 'right', paddingRight: 8 }}>
                  {dayChangePct != null ? formatPct(dayChangePct) : '—'}
                </div>
                <div style={{ fontFamily: 'JetBrains Mono', fontSize: 12, color: 'var(--muted2)', textAlign: 'right', paddingRight: 8 }}>
                  {item.targetPrice != null ? formatNumber(item.targetPrice, 2) : '—'}
                </div>
                <div style={{ fontFamily: 'JetBrains Mono', fontSize: 12, color: targetDelta == null ? 'var(--muted)' : targetDelta > 0 ? 'var(--acc)' : 'var(--red)', textAlign: 'right', paddingRight: 8 }}>
                  {targetDelta != null ? formatPct(targetDelta) : '—'}
                </div>
                <div style={{ fontFamily: 'Syne', fontSize: 11, color: 'var(--muted)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', paddingRight: 8 }}>
                  {item.note || '—'}
                </div>
                <div style={{ display: 'flex', gap: 4, justifyContent: 'flex-end' }}>
                  <button
                    onClick={() => handleAnalyze(item.ticker)}
                    title="Analizza"
                    style={{ background: 'var(--s3)', border: '1px solid var(--border)', borderRadius: 6, padding: '4px 8px', color: 'var(--acc)', cursor: 'pointer', fontSize: 11 }}
                  >
                    📊
                  </button>
                  <button
                    onClick={() => remove(item.id)}
                    title="Rimuovi"
                    style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'var(--muted)', fontSize: 16 }}
                  >
                    ×
                  </button>
                </div>
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}
