import { useState } from 'react'
import { useFullQuote, FullQuoteData } from '@/hooks/useQuote'
import { ChatPanel } from '@/components/chat/ChatPanel'
import { chatKey } from '@/store/useChatStore'
import { formatNumber, formatPct } from '@/lib/formatters'

function CompareRow({ label, v1, v2, higherIsBetter = true }: {
  label: string; v1: number; v2: number; higherIsBetter?: boolean
}) {
  const winner = higherIsBetter ? (v1 > v2 ? 1 : v2 > v1 ? 2 : 0) : (v1 < v2 ? 1 : v2 < v1 ? 2 : 0)
  return (
    <div style={{ display: 'grid', gridTemplateColumns: '1fr 140px 1fr', gap: 8, alignItems: 'center', padding: '8px 0', borderBottom: '1px solid var(--border)' }}>
      <div style={{
        fontFamily: 'JetBrains Mono', fontSize: 13, textAlign: 'right',
        color: winner === 1 ? 'var(--acc)' : 'var(--text)',
        fontWeight: winner === 1 ? 700 : 400,
      }}>
        {formatNumber(v1, 2)}{winner === 1 ? ' ✓' : ''}
      </div>
      <div style={{ fontFamily: 'Syne', fontSize: 11, color: 'var(--muted)', textAlign: 'center' }}>{label}</div>
      <div style={{
        fontFamily: 'JetBrains Mono', fontSize: 13, textAlign: 'left',
        color: winner === 2 ? 'var(--acc)' : 'var(--text)',
        fontWeight: winner === 2 ? 700 : 400,
      }}>
        {winner === 2 ? '✓ ' : ''}{formatNumber(v2, 2)}
      </div>
    </div>
  )
}

export function ComparePage() {
  const [t1Input, setT1Input] = useState('')
  const [t2Input, setT2Input] = useState('')
  const [t1, setT1] = useState<string | null>(null)
  const [t2, setT2] = useState<string | null>(null)

  const q1 = useFullQuote(t1)
  const q2 = useFullQuote(t2)

  const handleCompare = () => {
    if (t1Input.trim()) setT1(t1Input.trim().toUpperCase())
    if (t2Input.trim()) setT2(t2Input.trim().toUpperCase())
  }

  const tabKey = chatKey('compare', `${t1}-${t2}`)
  const context = {
    tab: 'compare',
    compareData: q1.data && q2.data ? { t1: q1.data, t2: q2.data } : undefined,
  }

  const d1 = q1.data as FullQuoteData | undefined
  const d2 = q2.data as FullQuoteData | undefined

  return (
    <div style={{ display: 'flex', gap: 16, padding: 24, height: 'calc(100vh - 112px)' }}>
      <div style={{ flex: 2, display: 'flex', flexDirection: 'column', gap: 16, overflowY: 'auto' }}>
        <div style={{
          background: 'var(--s2)', border: '1px solid var(--border)',
          borderRadius: 12, padding: '16px 20px',
        }}>
          <div style={{ display: 'flex', gap: 8 }}>
            <input
              value={t1Input}
              onChange={e => setT1Input(e.target.value.toUpperCase())}
              placeholder="Ticker 1 (es. AAPL)"
              style={{
                flex: 1, background: 'var(--s3)', border: '1px solid var(--border)',
                borderRadius: 8, padding: '10px 14px', color: 'var(--text)',
                fontFamily: 'JetBrains Mono', fontSize: 14, outline: 'none',
              }}
            />
            <input
              value={t2Input}
              onChange={e => setT2Input(e.target.value.toUpperCase())}
              placeholder="Ticker 2 (es. MSFT)"
              style={{
                flex: 1, background: 'var(--s3)', border: '1px solid var(--border)',
                borderRadius: 8, padding: '10px 14px', color: 'var(--text)',
                fontFamily: 'JetBrains Mono', fontSize: 14, outline: 'none',
              }}
            />
            <button
              onClick={handleCompare}
              style={{
                padding: '10px 20px', borderRadius: 8,
                background: 'var(--acc)', border: 'none',
                color: '#07080a', fontFamily: 'Syne', fontWeight: 700,
                fontSize: 13, cursor: 'pointer',
              }}
            >
              Confronta
            </button>
          </div>
        </div>

        {(q1.isLoading || q2.isLoading) && (
          <div style={{ textAlign: 'center', padding: 40, color: 'var(--muted)', fontFamily: 'Syne' }}>
            Caricamento dati…
          </div>
        )}

        {d1 && d2 && (
          <>
            <div style={{
              background: 'var(--s2)', border: '1px solid var(--acc)',
              borderRadius: 12, padding: '16px 20px',
            }}>
              <div style={{ fontFamily: 'Syne', fontWeight: 700, fontSize: 14, color: 'var(--acc)', marginBottom: 8 }}>
                Verdetto
              </div>
              <p style={{ fontFamily: 'Syne', fontSize: 13, color: 'var(--text)', margin: 0 }}>
                {d1.bullScore > d2.bullScore
                  ? `📊 ${d1.ticker} ha un bull score superiore (${d1.bullScore} vs ${d2.bullScore}), con migliori segnali tecnici complessivi.`
                  : d2.bullScore > d1.bullScore
                  ? `📊 ${d2.ticker} ha un bull score superiore (${d2.bullScore} vs ${d1.bullScore}), con migliori segnali tecnici complessivi.`
                  : `📊 I due titoli hanno un bull score equivalente (${d1.bullScore}/100). Situazione paritaria.`}
              </p>
            </div>

            <div style={{
              background: 'var(--s2)', border: '1px solid var(--border)',
              borderRadius: 12, overflow: 'hidden',
            }}>
              <div style={{
                display: 'grid', gridTemplateColumns: '1fr 140px 1fr',
                padding: '12px 20px', borderBottom: '1px solid var(--border)',
                background: 'var(--s3)',
              }}>
                <div style={{ fontFamily: 'Syne', fontWeight: 700, fontSize: 14, color: 'var(--acc)', textAlign: 'right' }}>
                  {d1.ticker}
                </div>
                <div style={{ fontFamily: 'Syne', fontSize: 11, color: 'var(--muted)', textAlign: 'center' }}>
                  Indicatore
                </div>
                <div style={{ fontFamily: 'Syne', fontWeight: 700, fontSize: 14, color: 'var(--acc2)', textAlign: 'left' }}>
                  {d2.ticker}
                </div>
              </div>
              <div style={{ padding: '0 20px' }}>
                <CompareRow label="Prezzo" v1={d1.price} v2={d2.price} higherIsBetter={false} />
                <CompareRow label="Variazione oggi %" v1={d1.dayChangePct} v2={d2.dayChangePct} />
                <CompareRow label="RSI (14)" v1={d1.rsi} v2={d2.rsi} higherIsBetter={false} />
                <CompareRow label="Momentum 30gg %" v1={d1.momentum30} v2={d2.momentum30} />
                <CompareRow label="Volatilità %" v1={d1.volatility} v2={d2.volatility} higherIsBetter={false} />
                <CompareRow label="Bull Score" v1={d1.bullScore} v2={d2.bullScore} />
                <CompareRow label="Range 52W %" v1={d1.rangePosition} v2={d2.rangePosition} higherIsBetter={false} />
              </div>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
              {[d1, d2].map(d => (
                <div key={d.ticker} style={{
                  background: 'var(--s2)', border: '1px solid var(--border)',
                  borderRadius: 12, padding: '16px',
                }}>
                  <div style={{ fontFamily: 'Syne', fontWeight: 700, fontSize: 14, color: 'var(--text)', marginBottom: 12 }}>
                    Scenari 30gg — {d.ticker}
                  </div>
                  {[
                    { label: 'Ottimista', color: 'var(--acc)', delta: d.volatility / 100 * 0.15 },
                    { label: 'Base', color: 'var(--acc2)', delta: d.momentum30 / 100 / 12 * 0.5 },
                    { label: 'Pessimista', color: 'var(--red)', delta: -(d.volatility / 100 * 0.10) },
                  ].map(s => {
                    const price = d.price * (1 + s.delta)
                    const pct = s.delta * 100
                    return (
                      <div key={s.label} style={{
                        display: 'flex', justifyContent: 'space-between',
                        padding: '6px 0', borderBottom: '1px solid var(--border)',
                      }}>
                        <span style={{ fontFamily: 'Syne', fontSize: 12, color: 'var(--muted)' }}>{s.label}</span>
                        <div style={{ textAlign: 'right' }}>
                          <span style={{ fontFamily: 'JetBrains Mono', fontSize: 12, color: s.color, marginRight: 8 }}>
                            {formatNumber(price, 2)}
                          </span>
                          <span style={{ fontFamily: 'JetBrains Mono', fontSize: 11, color: s.color }}>
                            {formatPct(pct)}
                          </span>
                        </div>
                      </div>
                    )
                  })}
                </div>
              ))}
            </div>
          </>
        )}

        {!t1 && !t2 && (
          <div style={{ textAlign: 'center', padding: 60, color: 'var(--muted)', fontFamily: 'Syne', fontSize: 15 }}>
            Inserisci due ticker per confrontarli
          </div>
        )}
      </div>

      <div style={{ flex: 1, minWidth: 320 }}>
        <ChatPanel
          tabKey={tabKey}
          context={context}
          quickActions={['Quale è il migliore ora?', 'Quale ha più potenziale a lungo termine?', 'Cosa dicono gli indicatori tecnici?']}
          placeholder="Chiedimi del confronto…"
        />
      </div>
    </div>
  )
}
