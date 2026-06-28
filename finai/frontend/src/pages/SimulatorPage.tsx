import { useState } from 'react'
import { useSimulator } from '@/hooks/useSimulator'
import { useQuote } from '@/hooks/useQuote'
import { formatNumber, formatPct, colorForChange, formatDateTime } from '@/lib/formatters'

function errorMessage(e: any): string {
  return e?.response?.data?.error || e?.message || 'Operazione non riuscita'
}

function Verdict({ totalPnlPct }: { totalPnlPct: number }) {
  const isGood = totalPnlPct > 2
  const isBad = totalPnlPct < -2
  const color = isGood ? 'var(--acc)' : isBad ? 'var(--red)' : 'var(--acc3)'
  const label = isGood ? 'ANDAMENTO POSITIVO' : isBad ? 'ANDAMENTO NEGATIVO' : 'ANDAMENTO NEUTRO'
  const reason = isGood
    ? `Il portafoglio simulato sta sovraperformando il capitale iniziale di ${formatPct(totalPnlPct)}. Le scelte effettuate finora si stanno rivelando profittevoli.`
    : isBad
    ? `Il portafoglio simulato è sotto il capitale iniziale di ${formatPct(Math.abs(totalPnlPct) * -1)}. Vale la pena rivedere le posizioni in perdita.`
    : `Il portafoglio simulato è vicino al pareggio (${formatPct(totalPnlPct)}). Non ci sono ancora segnali chiari sulla bontà dell'investimento.`

  return (
    <div style={{
      background: 'var(--s2)', border: `1px solid ${color}`,
      borderRadius: 12, padding: '14px 18px',
      display: 'flex', alignItems: 'center', gap: 16,
    }}>
      <div style={{
        padding: '6px 16px', borderRadius: 8, background: color,
        color: '#07080a', fontFamily: 'Syne', fontWeight: 800, fontSize: 14, whiteSpace: 'nowrap',
      }}>
        {label}
      </div>
      <p style={{ fontFamily: 'Syne', fontSize: 13, color: 'var(--muted2)', margin: 0, flex: 1 }}>
        {reason}
      </p>
    </div>
  )
}

export function SimulatorPage() {
  const { summary, isLoading, trades, buy, isBuying, sell, isSelling, reset, isResetting } = useSimulator()
  const [tickerInput, setTickerInput] = useState('')
  const [qtyInput, setQtyInput] = useState('')
  const [sellQty, setSellQty] = useState<Record<string, string>>({})
  const [error, setError] = useState<string | null>(null)
  const [showTrades, setShowTrades] = useState(false)

  const previewTicker = tickerInput.trim().toUpperCase() || null
  const preview = useQuote(previewTicker)

  const handleBuy = async () => {
    setError(null)
    const ticker = tickerInput.trim().toUpperCase()
    const qty = parseFloat(qtyInput)
    if (!ticker || isNaN(qty) || qty <= 0) return
    try {
      await buy({ ticker, qty })
      setTickerInput(''); setQtyInput('')
    } catch (e: any) {
      setError(errorMessage(e))
    }
  }

  const handleSell = async (ticker: string, maxQty: number) => {
    setError(null)
    const raw = sellQty[ticker]
    const qty = raw ? parseFloat(raw) : maxQty
    if (isNaN(qty) || qty <= 0) return
    try {
      await sell({ ticker, qty })
      setSellQty(prev => ({ ...prev, [ticker]: '' }))
    } catch (e: any) {
      setError(errorMessage(e))
    }
  }

  const handleReset = async () => {
    if (!window.confirm('Azzerare la simulazione? Posizioni e storico verranno eliminati e la liquidità riportata al capitale iniziale.')) return
    setError(null)
    try {
      await reset(undefined)
    } catch (e: any) {
      setError(errorMessage(e))
    }
  }

  if (isLoading || !summary) {
    return (
      <div style={{ textAlign: 'center', padding: 40, color: 'var(--muted)', fontFamily: 'Syne' }}>
        Caricamento simulazione…
      </div>
    )
  }

  const buyEstimate = preview.data?.price != null && qtyInput ? preview.data.price * parseFloat(qtyInput || '0') : null

  return (
    <div style={{ padding: 24, overflowY: 'auto', height: 'calc(100vh - 112px)', display: 'flex', flexDirection: 'column', gap: 16 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <div style={{ fontFamily: 'Syne', fontWeight: 700, fontSize: 18, color: 'var(--text)' }}>
            Simulazione Investimenti
          </div>
          <div style={{ fontFamily: 'Syne', fontSize: 12, color: 'var(--muted)', marginTop: 2 }}>
            Caso di studio con moneta virtuale — nessun rischio reale, prezzi live di mercato
          </div>
        </div>
        <button
          onClick={handleReset}
          disabled={isResetting}
          style={{
            padding: '8px 16px', borderRadius: 8, background: 'var(--s3)', border: '1px solid var(--border)',
            color: 'var(--red)', fontFamily: 'Syne', fontWeight: 600, fontSize: 12,
            cursor: isResetting ? 'default' : 'pointer', opacity: isResetting ? 0.6 : 1,
          }}
        >
          {isResetting ? '⟳ Reset…' : '↺ Azzera simulazione'}
        </button>
      </div>

      {error && (
        <div style={{
          background: 'rgba(239,68,68,0.1)', border: '1px solid var(--red)', borderRadius: 10,
          padding: '10px 16px', color: 'var(--red)', fontFamily: 'Syne', fontSize: 12,
        }}>
          {error}
        </div>
      )}

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 12 }}>
        {[
          { label: 'Liquidità disponibile', value: summary.cashBalance, color: 'var(--text)' },
          { label: 'Valore posizioni', value: summary.positionsValue, color: 'var(--text)' },
          { label: 'Valore totale', value: summary.totalValue, color: 'var(--acc)' },
          { label: 'P&L totale (€)', value: summary.totalPnl, color: colorForChange(summary.totalPnl) },
        ].map(s => (
          <div key={s.label} style={{ background: 'var(--s2)', border: '1px solid var(--border)', borderRadius: 10, padding: '14px 16px', textAlign: 'center' }}>
            <div style={{ fontFamily: 'Syne', fontSize: 11, color: 'var(--muted)', marginBottom: 6 }}>{s.label}</div>
            <div style={{ fontFamily: 'Instrument Serif', fontSize: 22, color: s.color }}>
              {formatNumber(s.value, 2)}
            </div>
          </div>
        ))}
      </div>

      <Verdict totalPnlPct={summary.totalPnlPct} />

      <div style={{ background: 'var(--s2)', border: '1px solid var(--border)', borderRadius: 12, padding: '16px 20px' }}>
        <div style={{ fontFamily: 'Syne', fontWeight: 700, fontSize: 14, color: 'var(--text)', marginBottom: 12 }}>
          Compra al prezzo live
        </div>
        <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', alignItems: 'center' }}>
          <input
            value={tickerInput}
            onChange={e => setTickerInput(e.target.value.toUpperCase())}
            placeholder="Ticker"
            style={{
              width: 110, background: 'var(--s3)', border: '1px solid var(--border)',
              borderRadius: 8, padding: '10px 12px', color: 'var(--text)',
              fontFamily: 'JetBrains Mono', fontSize: 13, outline: 'none',
            }}
          />
          <input
            value={qtyInput}
            onChange={e => setQtyInput(e.target.value)}
            placeholder="Quantità"
            type="number"
            style={{
              width: 100, background: 'var(--s3)', border: '1px solid var(--border)',
              borderRadius: 8, padding: '10px 12px', color: 'var(--text)',
              fontFamily: 'JetBrains Mono', fontSize: 13, outline: 'none',
            }}
          />
          {preview.data?.price != null && (
            <span style={{ fontFamily: 'JetBrains Mono', fontSize: 12, color: 'var(--muted)' }}>
              Prezzo: {formatNumber(preview.data.price, 2)} {preview.data.currency}
              {buyEstimate != null && ` · Costo stimato: ${formatNumber(buyEstimate, 2)}`}
            </span>
          )}
          <button
            onClick={handleBuy}
            disabled={isBuying || !tickerInput.trim() || !qtyInput}
            style={{
              padding: '10px 20px', borderRadius: 8, background: 'var(--acc)', border: 'none',
              color: '#07080a', fontFamily: 'Syne', fontWeight: 700, fontSize: 13,
              cursor: isBuying ? 'default' : 'pointer', opacity: isBuying ? 0.6 : 1,
            }}
          >
            {isBuying ? 'Acquisto…' : '+ Compra'}
          </button>
        </div>
      </div>

      {summary.positions.length === 0 ? (
        <div style={{ textAlign: 'center', padding: 40, color: 'var(--muted)', fontFamily: 'Syne', background: 'var(--s2)', border: '1px solid var(--border)', borderRadius: 12 }}>
          Nessuna posizione simulata. Inserisci un ticker e una quantità per iniziare il caso di studio.
        </div>
      ) : (
        <div style={{ background: 'var(--s2)', border: '1px solid var(--border)', borderRadius: 12, overflow: 'hidden' }}>
          <div style={{ display: 'grid', gridTemplateColumns: '80px 1fr 70px 90px 90px 90px 80px 100px 140px', gap: 0, padding: '10px 16px', background: 'var(--s3)', borderBottom: '1px solid var(--border)', fontFamily: 'Syne', fontSize: 11, color: 'var(--muted)', fontWeight: 700 }}>
            {['Ticker', 'Nome', 'Qty', 'Carico', 'Attuale', 'Valore', 'P&L%', 'P&L€', 'Vendi'].map(h => (
              <div key={h} style={{ textAlign: h === 'Vendi' ? 'center' : 'right', paddingRight: 8 }}>{h}</div>
            ))}
          </div>
          {summary.positions.map(p => (
            <div key={p.id} style={{ display: 'grid', gridTemplateColumns: '80px 1fr 70px 90px 90px 90px 80px 100px 140px', gap: 0, padding: '10px 16px', borderBottom: '1px solid var(--border)', alignItems: 'center', fontFamily: 'JetBrains Mono', fontSize: 12 }}>
              <div style={{ color: 'var(--text)', fontWeight: 700 }}>{p.ticker}</div>
              <div style={{ color: 'var(--muted2)', fontSize: 11, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{p.name}</div>
              <div style={{ textAlign: 'right', color: 'var(--text)' }}>{formatNumber(p.qty, 2)}</div>
              <div style={{ textAlign: 'right', color: 'var(--muted2)' }}>{formatNumber(p.avgPrice, 2)}</div>
              <div style={{ textAlign: 'right', color: 'var(--text)' }}>{p.currentPrice != null ? formatNumber(p.currentPrice, 2) : '—'}</div>
              <div style={{ textAlign: 'right', color: 'var(--text)' }}>{formatNumber(p.value, 2)}</div>
              <div style={{ textAlign: 'right', color: colorForChange(p.pnlPct) }}>{formatPct(p.pnlPct)}</div>
              <div style={{ textAlign: 'right', color: colorForChange(p.pnl) }}>{formatNumber(p.pnl, 2)}</div>
              <div style={{ display: 'flex', gap: 4, justifyContent: 'center' }}>
                <input
                  value={sellQty[p.ticker] ?? ''}
                  onChange={e => setSellQty(prev => ({ ...prev, [p.ticker]: e.target.value }))}
                  placeholder={formatNumber(p.qty, 2)}
                  type="number"
                  style={{
                    width: 60, background: 'var(--s3)', border: '1px solid var(--border)',
                    borderRadius: 6, padding: '5px 6px', color: 'var(--text)',
                    fontFamily: 'JetBrains Mono', fontSize: 11, outline: 'none',
                  }}
                />
                <button
                  onClick={() => handleSell(p.ticker, p.qty)}
                  disabled={isSelling}
                  style={{
                    padding: '5px 10px', borderRadius: 6, background: 'var(--red)', border: 'none',
                    color: '#07080a', fontFamily: 'Syne', fontWeight: 700, fontSize: 11,
                    cursor: isSelling ? 'default' : 'pointer', opacity: isSelling ? 0.6 : 1,
                  }}
                >
                  Vendi
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      <div>
        <button
          onClick={() => setShowTrades(s => !s)}
          style={{ background: 'none', border: 'none', color: 'var(--muted2)', fontFamily: 'Syne', fontSize: 12, cursor: 'pointer', padding: 0 }}
        >
          {showTrades ? '▾' : '▸'} Storico operazioni ({trades.length})
        </button>
        {showTrades && (
          <div style={{ background: 'var(--s2)', border: '1px solid var(--border)', borderRadius: 12, overflow: 'hidden', marginTop: 8 }}>
            <div style={{ display: 'grid', gridTemplateColumns: '140px 80px 70px 70px 90px 90px 90px', gap: 0, padding: '10px 16px', background: 'var(--s3)', borderBottom: '1px solid var(--border)', fontFamily: 'Syne', fontSize: 11, color: 'var(--muted)', fontWeight: 700 }}>
              {['Data', 'Ticker', 'Tipo', 'Qty', 'Prezzo', 'Importo', 'P&L real.'].map(h => (
                <div key={h} style={{ textAlign: h === 'Data' || h === 'Ticker' || h === 'Tipo' ? 'left' : 'right', paddingRight: 8 }}>{h}</div>
              ))}
            </div>
            {trades.length === 0 && (
              <div style={{ padding: 16, textAlign: 'center', color: 'var(--muted)', fontFamily: 'Syne', fontSize: 12 }}>
                Nessuna operazione effettuata.
              </div>
            )}
            {trades.map(t => (
              <div key={t.id} style={{ display: 'grid', gridTemplateColumns: '140px 80px 70px 70px 90px 90px 90px', gap: 0, padding: '8px 16px', borderBottom: '1px solid var(--border)', fontFamily: 'JetBrains Mono', fontSize: 11 }}>
                <div style={{ color: 'var(--muted2)' }}>{formatDateTime(new Date(t.executedAt).getTime())}</div>
                <div style={{ color: 'var(--text)', fontWeight: 700 }}>{t.ticker}</div>
                <div style={{ color: t.side === 'BUY' ? 'var(--acc)' : 'var(--red)' }}>{t.side}</div>
                <div style={{ textAlign: 'right', color: 'var(--text)' }}>{formatNumber(t.qty, 2)}</div>
                <div style={{ textAlign: 'right', color: 'var(--text)' }}>{formatNumber(t.price, 2)}</div>
                <div style={{ textAlign: 'right', color: 'var(--text)' }}>{formatNumber(t.amount, 2)}</div>
                <div style={{ textAlign: 'right', color: t.realizedPnl != null ? colorForChange(t.realizedPnl) : 'var(--muted)' }}>
                  {t.realizedPnl != null ? formatNumber(t.realizedPnl, 2) : '—'}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
