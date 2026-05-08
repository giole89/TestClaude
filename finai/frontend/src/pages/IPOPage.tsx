import { useState } from 'react'
import { useUpcomingIPOs, useRecentIPOs, useIPOWatchlist, WatchlistItem } from '@/hooks/useIPO'
import { useAppStore } from '@/store/useAppStore'
import { formatNumber, formatPct, formatLargeNumber } from '@/lib/formatters'

type Tab = 'upcoming' | 'recent' | 'watchlist'

function PctBadge({ value }: { value?: number }) {
  if (value == null) return <span style={{ color: 'var(--muted)', fontFamily: 'JetBrains Mono', fontSize: 12 }}>—</span>
  const pos = value >= 0
  return (
    <span style={{ fontFamily: 'JetBrains Mono', fontSize: 12, fontWeight: 700, color: pos ? 'var(--acc)' : 'var(--red)' }}>
      {formatPct(value)}
    </span>
  )
}

function UpcomingTab() {
  const { data, isLoading } = useUpcomingIPOs()

  if (isLoading) return <LoadingRows />
  if (!data?.length) return <Empty msg="Nessuna IPO imminente trovata per il periodo corrente." />

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
      {data.map((ipo, i) => (
        <div key={i} style={{
          background: 'var(--s2)', border: '1px solid var(--border)',
          borderRadius: 10, padding: '14px 18px',
          display: 'grid', gridTemplateColumns: '2fr 1fr 1fr 1fr 1fr',
          alignItems: 'center', gap: 12,
        }}>
          <div>
            <div style={{ fontFamily: 'Syne', fontWeight: 700, fontSize: 14, color: 'var(--text)' }}>
              {ipo.companyName}
            </div>
            {ipo.proposedTicker && (
              <div style={{ fontFamily: 'JetBrains Mono', fontSize: 11, color: 'var(--acc)', marginTop: 2 }}>
                {ipo.proposedTicker}
              </div>
            )}
          </div>
          <div style={{ textAlign: 'center' }}>
            <div style={{ fontFamily: 'Syne', fontSize: 10, color: 'var(--muted)', marginBottom: 2 }}>Borsa</div>
            <div style={{ fontFamily: 'JetBrains Mono', fontSize: 12, color: 'var(--text)' }}>
              {ipo.exchange ?? '—'}
            </div>
          </div>
          <div style={{ textAlign: 'center' }}>
            <div style={{ fontFamily: 'Syne', fontSize: 10, color: 'var(--muted)', marginBottom: 2 }}>Prezzo range</div>
            <div style={{ fontFamily: 'JetBrains Mono', fontSize: 12, color: 'var(--acc2)' }}>
              {ipo.priceRange ?? '—'}
            </div>
          </div>
          <div style={{ textAlign: 'center' }}>
            <div style={{ fontFamily: 'Syne', fontSize: 10, color: 'var(--muted)', marginBottom: 2 }}>Azioni offerte</div>
            <div style={{ fontFamily: 'JetBrains Mono', fontSize: 12, color: 'var(--text)' }}>
              {ipo.sharesOffered ?? '—'}
            </div>
          </div>
          <div style={{ textAlign: 'center' }}>
            <div style={{ fontFamily: 'Syne', fontSize: 10, color: 'var(--muted)', marginBottom: 2 }}>Data prevista</div>
            <div style={{ fontFamily: 'JetBrains Mono', fontSize: 12, color: 'var(--acc3)' }}>
              {ipo.expectedDate ?? '—'}
            </div>
          </div>
        </div>
      ))}
    </div>
  )
}

function RecentTab() {
  const { data, isLoading } = useRecentIPOs()
  const { setActiveTab } = useAppStore()

  if (isLoading) return <LoadingRows />
  if (!data?.length) return <Empty msg="Nessuna quotazione recente trovata." />

  const handleAnalyze = (ticker: string) => {
    sessionStorage.setItem('finai_analyze_ticker', ticker)
    setActiveTab('analyze')
  }

  return (
    <div>
      <div style={{
        display: 'grid', gridTemplateColumns: '2fr 1fr 1fr 1fr 1fr 1fr',
        padding: '8px 18px', marginBottom: 4,
        fontFamily: 'Syne', fontSize: 11, color: 'var(--muted)', fontWeight: 700,
      }}>
        {['Società', 'Borsa', 'Data IPO', 'Prezzo IPO', 'Prezzo att.', 'Performance'].map(h => (
          <div key={h} style={{ textAlign: h === 'Società' ? 'left' : 'center' }}>{h}</div>
        ))}
      </div>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
        {data.map((ipo, i) => (
          <div key={i} style={{
            background: 'var(--s2)', border: '1px solid var(--border)',
            borderRadius: 10, padding: '12px 18px',
            display: 'grid', gridTemplateColumns: '2fr 1fr 1fr 1fr 1fr 1fr',
            alignItems: 'center', gap: 12,
          }}>
            <div>
              <button
                onClick={() => ipo.ticker && handleAnalyze(ipo.ticker)}
                style={{
                  background: 'none', border: 'none', cursor: ipo.ticker ? 'pointer' : 'default',
                  padding: 0, textAlign: 'left',
                }}
              >
                <div style={{ fontFamily: 'Syne', fontWeight: 700, fontSize: 13, color: 'var(--text)' }}>
                  {ipo.companyName}
                </div>
                {ipo.ticker && (
                  <div style={{ fontFamily: 'JetBrains Mono', fontSize: 11, color: 'var(--acc)', marginTop: 1 }}>
                    {ipo.ticker}
                  </div>
                )}
              </button>
            </div>
            <div style={{ textAlign: 'center', fontFamily: 'JetBrains Mono', fontSize: 12, color: 'var(--muted2)' }}>
              {ipo.exchange ?? '—'}
            </div>
            <div style={{ textAlign: 'center', fontFamily: 'JetBrains Mono', fontSize: 12, color: 'var(--text)' }}>
              {ipo.ipoDate || '—'}
            </div>
            <div style={{ textAlign: 'center', fontFamily: 'JetBrains Mono', fontSize: 12, color: 'var(--text)' }}>
              {ipo.ipoPrice ? formatNumber(ipo.ipoPrice, 2) : '—'}
            </div>
            <div style={{ textAlign: 'center', fontFamily: 'JetBrains Mono', fontSize: 12, color: 'var(--text)' }}>
              {ipo.currentPrice ? formatNumber(ipo.currentPrice, 2) : '—'}
            </div>
            <div style={{ textAlign: 'center' }}>
              <PctBadge value={ipo.performance} />
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}

function WatchlistTab() {
  const { items, add, remove, update } = useIPOWatchlist()
  const [form, setForm] = useState({
    companyName: '', ticker: '', expectedDate: '', exchange: '',
    sector: '', lockupDays: '180', ipoPrice: '', notes: '',
  })
  const [editing, setEditing] = useState<string | null>(null)
  const [editNotes, setEditNotes] = useState('')

  const handleAdd = async () => {
    if (!form.companyName.trim()) return
    await add({
      id: `ipo-${Date.now()}`,
      companyName: form.companyName.trim(),
      ticker: form.ticker.trim().toUpperCase() || undefined,
      expectedDate: form.expectedDate || undefined,
      exchange: form.exchange || undefined,
      sector: form.sector || undefined,
      lockupDays: parseInt(form.lockupDays) || 180,
      ipoPrice: form.ipoPrice ? parseFloat(form.ipoPrice) : undefined,
      notes: form.notes || undefined,
    })
    setForm({ companyName: '', ticker: '', expectedDate: '', exchange: '', sector: '', lockupDays: '180', ipoPrice: '', notes: '' })
  }

  const lockupStatus = (item: WatchlistItem) => {
    if (!item.expectedDate || !item.ipoPrice) return null
    const ipoMs = new Date(item.expectedDate).getTime()
    const lockupEnd = ipoMs + item.lockupDays * 86_400_000
    const now = Date.now()
    if (now < ipoMs) return { label: 'Pre-IPO', color: 'var(--acc3)' }
    if (now < lockupEnd) {
      const daysLeft = Math.ceil((lockupEnd - now) / 86_400_000)
      return { label: `Lock-up: ${daysLeft}gg`, color: 'var(--red)' }
    }
    return { label: 'Lock-up scaduto', color: 'var(--acc)' }
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
      <div style={{
        background: 'var(--s2)', border: '1px solid var(--border)',
        borderRadius: 12, padding: '16px 20px',
      }}>
        <div style={{ fontFamily: 'Syne', fontWeight: 700, fontSize: 14, color: 'var(--text)', marginBottom: 12 }}>
          + Aggiungi alla watchlist
        </div>
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
          {[
            { key: 'companyName', ph: 'Società *', w: 200 },
            { key: 'ticker', ph: 'Ticker (opz.)', w: 110 },
            { key: 'expectedDate', ph: 'Data prevista', w: 140, type: 'date' },
            { key: 'exchange', ph: 'Borsa', w: 110 },
            { key: 'sector', ph: 'Settore', w: 130 },
            { key: 'ipoPrice', ph: 'Prezzo IPO', w: 110, type: 'number' },
            { key: 'lockupDays', ph: 'Lock-up gg', w: 100, type: 'number' },
          ].map(f => (
            <input
              key={f.key}
              value={form[f.key as keyof typeof form]}
              type={f.type ?? 'text'}
              placeholder={f.ph}
              onChange={e => setForm(p => ({ ...p, [f.key]: f.key === 'ticker' ? e.target.value.toUpperCase() : e.target.value }))}
              style={{
                width: f.w, background: 'var(--s3)', border: '1px solid var(--border)',
                borderRadius: 8, padding: '8px 12px', color: 'var(--text)',
                fontFamily: 'JetBrains Mono', fontSize: 12, outline: 'none',
              }}
            />
          ))}
          <button
            onClick={handleAdd}
            style={{
              padding: '8px 18px', borderRadius: 8, background: 'var(--acc)',
              border: 'none', color: '#07080a', fontFamily: 'Syne', fontWeight: 700,
              fontSize: 13, cursor: 'pointer',
            }}
          >
            Aggiungi
          </button>
        </div>
      </div>

      {items.length === 0 && <Empty msg="Nessuna IPO nella watchlist. Aggiungine una sopra." />}

      <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
        {items.map(item => {
          const status = lockupStatus(item)
          return (
            <div key={item.id} style={{
              background: 'var(--s2)', border: '1px solid var(--border)',
              borderRadius: 10, padding: '12px 16px',
            }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 12, flexWrap: 'wrap' }}>
                <div style={{ flex: 1, minWidth: 180 }}>
                  <div style={{ fontFamily: 'Syne', fontWeight: 700, fontSize: 14, color: 'var(--text)' }}>
                    {item.companyName}
                  </div>
                  <div style={{ display: 'flex', gap: 8, marginTop: 2, flexWrap: 'wrap' }}>
                    {item.ticker && (
                      <span style={{ fontFamily: 'JetBrains Mono', fontSize: 11, color: 'var(--acc)' }}>
                        {item.ticker}
                      </span>
                    )}
                    {item.exchange && (
                      <span style={{ fontFamily: 'Syne', fontSize: 11, color: 'var(--muted)' }}>{item.exchange}</span>
                    )}
                    {item.sector && (
                      <span style={{ fontFamily: 'Syne', fontSize: 11, color: 'var(--muted2)' }}>{item.sector}</span>
                    )}
                  </div>
                </div>

                <div style={{ display: 'flex', gap: 16, alignItems: 'center', flexWrap: 'wrap' }}>
                  {item.expectedDate && (
                    <div style={{ textAlign: 'center' }}>
                      <div style={{ fontFamily: 'Syne', fontSize: 10, color: 'var(--muted)' }}>Data IPO</div>
                      <div style={{ fontFamily: 'JetBrains Mono', fontSize: 12, color: 'var(--text)' }}>{item.expectedDate}</div>
                    </div>
                  )}
                  {item.ipoPrice && (
                    <div style={{ textAlign: 'center' }}>
                      <div style={{ fontFamily: 'Syne', fontSize: 10, color: 'var(--muted)' }}>Prezzo IPO</div>
                      <div style={{ fontFamily: 'JetBrains Mono', fontSize: 12, color: 'var(--text)' }}>{formatNumber(item.ipoPrice, 2)}</div>
                    </div>
                  )}
                  {status && (
                    <span style={{
                      padding: '3px 10px', borderRadius: 6, background: status.color,
                      color: '#07080a', fontFamily: 'Syne', fontWeight: 700, fontSize: 11,
                    }}>
                      {status.label}
                    </span>
                  )}
                </div>

                <div style={{ display: 'flex', gap: 6 }}>
                  <button
                    onClick={() => { setEditing(editing === item.id ? null : item.id); setEditNotes(item.notes ?? '') }}
                    style={{
                      padding: '4px 10px', borderRadius: 6, border: '1px solid var(--border2)',
                      background: 'var(--s3)', color: 'var(--muted2)', cursor: 'pointer',
                      fontFamily: 'Syne', fontSize: 11,
                    }}
                  >
                    Note
                  </button>
                  <button
                    onClick={() => remove(item.id)}
                    style={{
                      padding: '4px 10px', borderRadius: 6, border: '1px solid var(--red)',
                      background: 'none', color: 'var(--red)', cursor: 'pointer',
                      fontFamily: 'Syne', fontSize: 11,
                    }}
                  >
                    ×
                  </button>
                </div>
              </div>

              {editing === item.id && (
                <div style={{ marginTop: 10, display: 'flex', gap: 8 }}>
                  <textarea
                    value={editNotes}
                    onChange={e => setEditNotes(e.target.value)}
                    placeholder="Note libere…"
                    rows={2}
                    style={{
                      flex: 1, background: 'var(--s3)', border: '1px solid var(--border)',
                      borderRadius: 8, padding: '8px 12px', color: 'var(--text)',
                      fontFamily: 'Syne', fontSize: 12, resize: 'none', outline: 'none',
                    }}
                  />
                  <button
                    onClick={() => { update({ id: item.id, notes: editNotes }); setEditing(null) }}
                    style={{
                      padding: '6px 14px', borderRadius: 8, background: 'var(--acc2)',
                      border: 'none', color: '#07080a', fontFamily: 'Syne', fontWeight: 700,
                      fontSize: 12, cursor: 'pointer',
                    }}
                  >
                    Salva
                  </button>
                </div>
              )}
              {item.notes && editing !== item.id && (
                <div style={{ marginTop: 8, fontFamily: 'Syne', fontSize: 12, color: 'var(--muted2)', fontStyle: 'italic' }}>
                  {item.notes}
                </div>
              )}
            </div>
          )
        })}
      </div>
    </div>
  )
}

function LoadingRows() {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
      {[...Array(5)].map((_, i) => (
        <div key={i} style={{
          height: 56, background: 'var(--s2)', borderRadius: 10,
          animation: 'pulse 1.5s infinite',
        }} />
      ))}
    </div>
  )
}

function Empty({ msg }: { msg: string }) {
  return (
    <div style={{ textAlign: 'center', padding: 60, color: 'var(--muted)', fontFamily: 'Syne', fontSize: 14 }}>
      {msg}
    </div>
  )
}

const TABS: Array<{ id: Tab; label: string; icon: string }> = [
  { id: 'upcoming', label: 'Prossime IPO', icon: '📅' },
  { id: 'recent', label: 'Quotazioni Recenti', icon: '🆕' },
  { id: 'watchlist', label: 'Watchlist Personale', icon: '👀' },
]

export function IPOPage() {
  const [tab, setTab] = useState<Tab>('upcoming')

  return (
    <div style={{ padding: 24, maxWidth: 1200, margin: '0 auto' }}>
      <div style={{ display: 'flex', alignItems: 'baseline', gap: 12, marginBottom: 20 }}>
        <h2 style={{ fontFamily: 'Syne', fontWeight: 800, fontSize: 22, color: 'var(--text)', margin: 0 }}>
          🏛️ IPO & Nuove Quotazioni
        </h2>
        <span style={{ fontFamily: 'Syne', fontSize: 12, color: 'var(--muted)' }}>
          dati NASDAQ + Yahoo Finance
        </span>
      </div>

      <div style={{ display: 'flex', gap: 4, marginBottom: 20 }}>
        {TABS.map(t => (
          <button
            key={t.id}
            onClick={() => setTab(t.id)}
            style={{
              display: 'flex', alignItems: 'center', gap: 6,
              padding: '8px 16px', borderRadius: 8, cursor: 'pointer',
              fontFamily: 'Syne', fontWeight: 600, fontSize: 13,
              background: tab === t.id ? 'var(--acc)' : 'var(--s2)',
              color: tab === t.id ? '#07080a' : 'var(--muted2)',
              outline: `1px solid ${tab === t.id ? 'transparent' : 'var(--border)'}`,
              transition: 'all 0.15s',
            }}
          >
            <span>{t.icon}</span>
            <span>{t.label}</span>
          </button>
        ))}
      </div>

      {tab === 'upcoming' && <UpcomingTab />}
      {tab === 'recent' && <RecentTab />}
      {tab === 'watchlist' && <WatchlistTab />}
    </div>
  )
}
