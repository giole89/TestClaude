import { useState } from 'react'
import { usePortfolio } from '@/hooks/usePortfolio'
import { usePortfolioAnalytics } from '@/hooks/usePortfolioAnalytics'
import { useEarnings } from '@/hooks/useEarnings'
import { ChatPanel } from '@/components/chat/ChatPanel'
import { chatKey } from '@/store/useChatStore'
import { useAppStore } from '@/store/useAppStore'
import { formatNumber, formatPct } from '@/lib/formatters'

// ── Helpers ────────────────────────────────────────────────────────────────────

function HhiBar({ hhi }: { hhi: number }) {
  const pct = Math.min(hhi / 100, 100)
  const color = hhi < 1500 ? 'var(--acc)' : hhi < 2500 ? '#f59e0b' : 'var(--red)'
  const label = hhi < 1500 ? 'Diversificato' : hhi < 2500 ? 'Moderato' : 'Concentrato'
  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
        <span style={{ fontFamily: 'Syne', fontSize: 11, color: 'var(--muted)' }}>Concentrazione HHI</span>
        <span style={{ fontFamily: 'JetBrains Mono', fontSize: 11, color }}>{label} ({Math.round(hhi)})</span>
      </div>
      <div style={{ height: 6, background: 'var(--s3)', borderRadius: 3, overflow: 'hidden' }}>
        <div style={{ height: '100%', width: `${pct}%`, background: color, borderRadius: 3, transition: 'width 0.4s' }} />
      </div>
    </div>
  )
}

function WeightBar({ pct }: { pct: number }) {
  return (
    <div style={{ height: 4, background: 'var(--s3)', borderRadius: 2, overflow: 'hidden', marginTop: 3 }}>
      <div style={{ height: '100%', width: `${Math.min(pct, 100)}%`, background: 'var(--acc)', borderRadius: 2 }} />
    </div>
  )
}

export function PortfolioPage() {
  const { items, isLoading, isRefreshing, add, remove, refresh } = usePortfolio()
  const { setActiveTab } = useAppStore()
  const hasItems = items.length > 0
  const analytics = usePortfolioAnalytics(hasItems)
  const tickers = items.map(i => i.ticker)
  const earnings = useEarnings(tickers)
  const [tInput, setTInput] = useState('')
  const [qty, setQty] = useState('')
  const [load, setLoad] = useState('')
  const [nameInput, setNameInput] = useState('')

  const totalInvested = items.reduce((acc, i) => acc + i.qty * i.loadPrice, 0)
  const totalValue = items.reduce((acc, i) => acc + i.qty * (i.currentPrice ?? i.loadPrice), 0)
  const totalPL = totalValue - totalInvested
  const totalPLPct = totalInvested > 0 ? (totalPL / totalInvested) * 100 : 0

  const handleAdd = async () => {
    const t = tInput.trim().toUpperCase()
    const q = parseFloat(qty)
    const l = parseFloat(load)
    if (!t || isNaN(q) || isNaN(l) || q <= 0 || l <= 0) return
    await add({ id: `${t}-${Date.now()}`, ticker: t, name: nameInput || t, qty: q, loadPrice: l, currency: 'USD' })
    setTInput(''); setQty(''); setLoad(''); setNameInput('')
  }

  const tabKey = chatKey('portfolio', 'main')
  const context = {
    tab: 'portfolio',
    portfolio: items.map(i => ({
      ...i,
      value: i.qty * (i.currentPrice ?? i.loadPrice),
      pl: i.qty * ((i.currentPrice ?? i.loadPrice) - i.loadPrice),
      plPct: ((i.currentPrice ?? i.loadPrice) - i.loadPrice) / i.loadPrice * 100,
    })),
    summary: { totalInvested, totalValue, totalPL, totalPLPct },
  }

  return (
    <div style={{ display: 'flex', gap: 16, padding: 24, height: 'calc(100vh - 112px)' }}>
      <div style={{ flex: 2, display: 'flex', flexDirection: 'column', gap: 16, overflowY: 'auto' }}>
        <div style={{ background: 'var(--s2)', border: '1px solid var(--border)', borderRadius: 12, padding: '16px 20px' }}>
          <div style={{ fontFamily: 'Syne', fontWeight: 700, fontSize: 14, color: 'var(--text)', marginBottom: 12 }}>
            Aggiungi posizione
          </div>
          <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
            {[
              { val: tInput, set: setTInput, ph: 'Ticker', w: 110, upper: true, mono: true },
              { val: qty, set: setQty, ph: 'Quantità', w: 100, upper: false, mono: true },
              { val: load, set: setLoad, ph: 'Prezzo carico', w: 130, upper: false, mono: true },
              { val: nameInput, set: setNameInput, ph: 'Nome (opz.)', w: 160, upper: false, mono: false },
            ].map((f, i) => (
              <input
                key={i}
                value={f.val}
                onChange={e => f.set(f.upper ? e.target.value.toUpperCase() : e.target.value)}
                placeholder={f.ph}
                type={f.upper || f.ph === 'Nome (opz.)' ? 'text' : 'number'}
                style={{
                  width: f.w, background: 'var(--s3)', border: '1px solid var(--border)',
                  borderRadius: 8, padding: '10px 12px', color: 'var(--text)',
                  fontFamily: f.mono ? 'JetBrains Mono' : 'Syne', fontSize: 13, outline: 'none',
                }}
              />
            ))}
            <button onClick={handleAdd} style={{ padding: '10px 20px', borderRadius: 8, background: 'var(--acc)', border: 'none', color: '#07080a', fontFamily: 'Syne', fontWeight: 700, fontSize: 13, cursor: 'pointer' }}>
              + Aggiungi
            </button>
          </div>
        </div>

        {isLoading && (
          <div style={{ textAlign: 'center', padding: 40, color: 'var(--muted)', fontFamily: 'Syne' }}>Caricamento portafoglio…</div>
        )}

        {!isLoading && items.length > 0 && (
          <>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 12 }}>
              {[
                { label: 'Valore attuale', value: totalValue, color: 'var(--acc)' },
                { label: 'Investito totale', value: totalInvested, color: 'var(--text)' },
                { label: 'P&L (€)', value: totalPL, color: totalPL >= 0 ? 'var(--acc)' : 'var(--red)' },
                { label: 'P&L (%)', value: totalPLPct, color: totalPLPct >= 0 ? 'var(--acc)' : 'var(--red)', isPct: true },
              ].map(s => (
                <div key={s.label} style={{ background: 'var(--s2)', border: '1px solid var(--border)', borderRadius: 10, padding: '14px 16px', textAlign: 'center' }}>
                  <div style={{ fontFamily: 'Syne', fontSize: 11, color: 'var(--muted)', marginBottom: 6 }}>{s.label}</div>
                  <div style={{ fontFamily: 'Instrument Serif', fontSize: 22, color: s.color }}>
                    {s.isPct ? formatPct(s.value) : formatNumber(s.value, 2)}
                  </div>
                </div>
              ))}
            </div>

            <div>
              <button onClick={() => refresh()} disabled={isRefreshing} style={{ padding: '8px 16px', borderRadius: 8, background: 'var(--s3)', border: '1px solid var(--border)', color: 'var(--text)', fontFamily: 'Syne', fontWeight: 600, fontSize: 12, cursor: isRefreshing ? 'default' : 'pointer', opacity: isRefreshing ? 0.6 : 1 }}>
                {isRefreshing ? '⟳ Aggiornamento…' : '↻ Aggiorna prezzi'}
              </button>
            </div>

            <div style={{ background: 'var(--s2)', border: '1px solid var(--border)', borderRadius: 12, overflow: 'hidden' }}>
              <div style={{ display: 'grid', gridTemplateColumns: '80px 1fr 60px 80px 80px 80px 80px 60px 50px 40px', gap: 0, padding: '10px 16px', background: 'var(--s3)', borderBottom: '1px solid var(--border)', fontFamily: 'Syne', fontSize: 11, color: 'var(--muted)', fontWeight: 700 }}>
                {['Ticker', 'Nome', 'Qty', 'Carico', 'Att.', 'Oggi%', 'Valore', 'P&L€', 'Peso', ''].map(h => (
                  <div key={h} style={{ textAlign: h === '' ? 'center' : 'right', paddingRight: 8 }}>{h}</div>
                ))}
              </div>
              {items.map(item => {
                const cp = item.currentPrice ?? item.loadPrice
                const value = item.qty * cp
                const pl = item.qty * (cp - item.loadPrice)
                const plPct = ((cp - item.loadPrice) / item.loadPrice) * 100
                const weight = totalValue > 0 ? (value / totalValue) * 100 : 0
                return (
                  <div key={item.id} style={{ display: 'grid', gridTemplateColumns: '80px 1fr 60px 80px 80px 80px 80px 60px 50px 40px', gap: 0, padding: '10px 16px', borderBottom: '1px solid var(--border)', alignItems: 'center' }}>
                    <button onClick={() => { sessionStorage.setItem('finai_analyze_ticker', item.ticker); setActiveTab('analyze') }} style={{ background: 'none', border: 'none', cursor: 'pointer', fontFamily: 'JetBrains Mono', fontWeight: 700, fontSize: 13, color: 'var(--acc)', padding: 0, textAlign: 'left' }}>
                      {item.ticker}
                    </button>
                    <div style={{ fontFamily: 'Syne', fontSize: 12, color: 'var(--muted)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', paddingRight: 8 }}>{item.name}</div>
                    {[
                      { v: item.qty.toString(), c: 'var(--text)' },
                      { v: formatNumber(item.loadPrice, 2), c: 'var(--text)' },
                      { v: formatNumber(cp, 2), c: 'var(--text)' },
                      { v: formatPct(plPct), c: pl >= 0 ? 'var(--acc)' : 'var(--red)' },
                      { v: formatNumber(value, 2), c: 'var(--text)' },
                      { v: (pl >= 0 ? '+' : '') + formatNumber(pl, 2), c: pl >= 0 ? 'var(--acc)' : 'var(--red)' },
                      { v: `${formatNumber(weight, 1)}%`, c: 'var(--muted2)' },
                    ].map((cell, i) => (
                      <div key={i} style={{ fontFamily: 'JetBrains Mono', fontSize: 12, color: cell.c, textAlign: 'right', paddingRight: 8 }}>{cell.v}</div>
                    ))}
                    <button onClick={() => remove(item.id)} style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'var(--muted)', fontSize: 16, textAlign: 'center', padding: 0 }}>×</button>
                  </div>
                )
              })}
              <div style={{ display: 'grid', gridTemplateColumns: '80px 1fr 60px 80px 80px 80px 80px 60px 50px 40px', gap: 0, padding: '10px 16px', background: 'var(--s3)', fontWeight: 700 }}>
                <div style={{ fontFamily: 'Syne', fontSize: 12, color: 'var(--text)', gridColumn: '1/7' }}>TOTALE</div>
                <div style={{ fontFamily: 'JetBrains Mono', fontSize: 12, color: 'var(--text)', textAlign: 'right', paddingRight: 8 }}>{formatNumber(totalValue, 2)}</div>
                <div style={{ fontFamily: 'JetBrains Mono', fontSize: 12, color: totalPL >= 0 ? 'var(--acc)' : 'var(--red)', textAlign: 'right', paddingRight: 8 }}>{(totalPL >= 0 ? '+' : '') + formatNumber(totalPL, 2)}</div>
                <div style={{ fontFamily: 'JetBrains Mono', fontSize: 12, color: totalPLPct >= 0 ? 'var(--acc)' : 'var(--red)', textAlign: 'right', paddingRight: 8 }}>{formatPct(totalPLPct)}</div>
                <div />
              </div>
            </div>
          </>
        )}

        {!isLoading && items.length === 0 && (
          <div style={{ textAlign: 'center', padding: 60, color: 'var(--muted)', fontFamily: 'Syne', fontSize: 15 }}>
            Il portafoglio è vuoto. Aggiungi la tua prima posizione.
          </div>
        )}

        {/* ── Analytics Panel ─────────────────────────────────────────────── */}
        {hasItems && analytics.data && (
          <div style={{ background: 'var(--s2)', border: '1px solid var(--border)', borderRadius: 12, padding: '16px 20px', display: 'flex', flexDirection: 'column', gap: 16 }}>
            <div style={{ fontFamily: 'Syne', fontWeight: 700, fontSize: 13, color: 'var(--text)' }}>Analytics portafoglio</div>

            {/* KPI row */}
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 10 }}>
              {[
                {
                  label: 'Miglior performer',
                  value: analytics.data.bestTicker ?? '—',
                  sub: analytics.data.bestGainPct != null ? formatPct(analytics.data.bestGainPct) : '',
                  color: 'var(--acc)',
                },
                {
                  label: 'Peggior performer',
                  value: analytics.data.worstTicker ?? '—',
                  sub: analytics.data.worstGainPct != null ? formatPct(analytics.data.worstGainPct) : '',
                  color: (analytics.data.worstGainPct ?? 0) >= 0 ? 'var(--acc)' : 'var(--red)',
                },
                {
                  label: 'Posizione dominante',
                  value: analytics.data.topWeightTicker ?? '—',
                  sub: analytics.data.topWeightPct != null ? `${formatNumber(analytics.data.topWeightPct, 1)}% del portafoglio` : '',
                  color: 'var(--text)',
                },
              ].map(k => (
                <div key={k.label} style={{ background: 'var(--s3)', borderRadius: 8, padding: '10px 12px' }}>
                  <div style={{ fontFamily: 'Syne', fontSize: 10, color: 'var(--muted)', marginBottom: 4 }}>{k.label}</div>
                  <div style={{ fontFamily: 'JetBrains Mono', fontWeight: 700, fontSize: 15, color: k.color }}>{k.value}</div>
                  {k.sub && <div style={{ fontFamily: 'JetBrains Mono', fontSize: 11, color: k.color, marginTop: 2 }}>{k.sub}</div>}
                </div>
              ))}
            </div>

            {/* HHI bar */}
            <HhiBar hhi={analytics.data.concentrationHhi} />

            {/* Breakdown peso posizioni */}
            <div>
              <div style={{ fontFamily: 'Syne', fontSize: 11, color: 'var(--muted)', marginBottom: 8 }}>Breakdown peso posizioni</div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
                {analytics.data.positions.map(p => (
                  <div key={p.ticker}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 2 }}>
                      <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
                        <span style={{ fontFamily: 'JetBrains Mono', fontWeight: 700, fontSize: 12, color: 'var(--acc)', minWidth: 72 }}>{p.ticker}</span>
                        <span style={{ fontFamily: 'Syne', fontSize: 11, color: 'var(--muted)' }}>{p.name}</span>
                      </div>
                      <div style={{ display: 'flex', gap: 16 }}>
                        <span style={{ fontFamily: 'JetBrains Mono', fontSize: 11, color: 'var(--muted2)' }}>{formatNumber(p.weight, 1)}%</span>
                        <span style={{ fontFamily: 'JetBrains Mono', fontSize: 11, color: p.gainPct >= 0 ? 'var(--acc)' : 'var(--red)', minWidth: 60, textAlign: 'right' }}>
                          {formatPct(p.gainPct)}
                        </span>
                        <span style={{ fontFamily: 'JetBrains Mono', fontSize: 11, color: p.contributionPct >= 0 ? 'var(--acc)' : 'var(--red)', minWidth: 64, textAlign: 'right' }}>
                          {p.contributionPct >= 0 ? '+' : ''}{formatNumber(p.contributionPct, 2)}pp
                        </span>
                      </div>
                    </div>
                    <WeightBar pct={p.weight} />
                  </div>
                ))}
              </div>
              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 16, marginTop: 6 }}>
                <span style={{ fontFamily: 'Syne', fontSize: 10, color: 'var(--muted)' }}>Peso%</span>
                <span style={{ fontFamily: 'Syne', fontSize: 10, color: 'var(--muted)', minWidth: 60, textAlign: 'right' }}>P&L%</span>
                <span style={{ fontFamily: 'Syne', fontSize: 10, color: 'var(--muted)', minWidth: 64, textAlign: 'right' }}>Contribuzione</span>
              </div>
            </div>
          </div>
        )}

        {/* ── Earnings Calendar ───────────────────────────────────────────── */}
        {hasItems && (
          <div style={{ background: 'var(--s2)', border: '1px solid var(--border)', borderRadius: 12, padding: '16px 20px' }}>
            <div style={{ fontFamily: 'Syne', fontWeight: 700, fontSize: 13, color: 'var(--text)', marginBottom: 12 }}>
              Prossimi earnings
            </div>

            {earnings.isLoading && (
              <div style={{ color: 'var(--muted)', fontFamily: 'Syne', fontSize: 13, padding: '8px 0' }}>
                Caricamento date earnings…
              </div>
            )}

            {!earnings.isLoading && (earnings.data ?? []).length === 0 && (
              <div style={{ color: 'var(--muted)', fontFamily: 'Syne', fontSize: 13, padding: '8px 0' }}>
                Nessuna data earnings disponibile per i titoli in portafoglio.
              </div>
            )}

            {!earnings.isLoading && (earnings.data ?? []).length > 0 && (
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(260px, 1fr))', gap: 10 }}>
                {(earnings.data ?? [])
                  .filter(e => e.earningsDate != null)
                  .sort((a, b) => (a.earningsDate ?? '').localeCompare(b.earningsDate ?? ''))
                  .map(e => {
                    const isUpcoming = e.earningsDate && e.earningsDate >= new Date().toISOString().slice(0, 10)
                    return (
                      <div key={e.ticker} style={{ background: 'var(--s3)', border: `1px solid ${isUpcoming ? 'var(--acc)' : 'var(--border)'}`, borderRadius: 8, padding: '12px 14px' }}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 6 }}>
                          <div>
                            <span style={{ fontFamily: 'JetBrains Mono', fontWeight: 700, fontSize: 13, color: 'var(--acc)' }}>{e.ticker}</span>
                            {e.quarter && <span style={{ fontFamily: 'Syne', fontSize: 10, color: 'var(--muted)', marginLeft: 8 }}>{e.quarter}</span>}
                          </div>
                          {isUpcoming && (
                            <span style={{ fontFamily: 'Syne', fontSize: 10, color: 'var(--acc)', background: 'rgba(0,217,126,0.1)', borderRadius: 4, padding: '2px 6px' }}>
                              Prossimo
                            </span>
                          )}
                        </div>
                        <div style={{ fontFamily: 'Syne', fontSize: 11, color: 'var(--muted)', marginBottom: 6, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                          {e.companyName}
                        </div>
                        <div style={{ fontFamily: 'JetBrains Mono', fontSize: 13, color: 'var(--text)', marginBottom: 8 }}>
                          {e.earningsDate ?? '—'}
                        </div>
                        <div style={{ display: 'flex', gap: 12 }}>
                          {e.epsForward != null && (
                            <div>
                              <div style={{ fontFamily: 'Syne', fontSize: 10, color: 'var(--muted)' }}>EPS fwd</div>
                              <div style={{ fontFamily: 'JetBrains Mono', fontSize: 12, color: e.epsForward >= 0 ? 'var(--acc)' : 'var(--red)' }}>
                                {e.epsForward >= 0 ? '+' : ''}{formatNumber(e.epsForward, 2)}
                              </div>
                            </div>
                          )}
                          {e.epsTrailing != null && (
                            <div>
                              <div style={{ fontFamily: 'Syne', fontSize: 10, color: 'var(--muted)' }}>EPS ttm</div>
                              <div style={{ fontFamily: 'JetBrains Mono', fontSize: 12, color: 'var(--text)' }}>
                                {formatNumber(e.epsTrailing, 2)}
                              </div>
                            </div>
                          )}
                          {e.forwardPE != null && (
                            <div>
                              <div style={{ fontFamily: 'Syne', fontSize: 10, color: 'var(--muted)' }}>P/E fwd</div>
                              <div style={{ fontFamily: 'JetBrains Mono', fontSize: 12, color: 'var(--text)' }}>
                                {formatNumber(e.forwardPE, 1)}x
                              </div>
                            </div>
                          )}
                        </div>
                      </div>
                    )
                  })}
              </div>
            )}
          </div>
        )}
      </div>
      <div style={{ flex: 1, minWidth: 320 }}>
        <ChatPanel tabKey={tabKey} context={context} quickActions={['Analizza il mio portafoglio', 'Come diversifico meglio?', 'Quali sono i rischi principali?']} placeholder="Chiedimi del tuo portafoglio…" />
      </div>
    </div>
  )
}
