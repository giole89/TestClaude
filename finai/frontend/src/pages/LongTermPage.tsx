import { useState } from 'react'
import { motion } from 'framer-motion'
import { useFullQuote } from '@/hooks/useQuote'
import { ChatPanel } from '@/components/chat/ChatPanel'
import { chatKey } from '@/store/useChatStore'
import { calcLongTermScore } from '@/lib/indicators'
import { formatNumber, formatPct } from '@/lib/formatters'
import { WATCHLIST_QUICK } from '@/lib/constants'

export function LongTermPage() {
  const [input, setInput] = useState('')
  const [ticker, setTicker] = useState<string | null>(null)
  const { data, isLoading, error } = useFullQuote(ticker)

  const handleEvaluate = () => {
    const t = input.trim().toUpperCase()
    if (t) setTicker(t)
  }

  const ltScore = data
    ? calcLongTermScore({
        price: data.price,
        sma200: data.sma200,
        volatility: data.volatility,
        rangePosition: data.rangePosition,
        momentum30: data.momentum30,
        rsi: data.rsi,
        low52w: data.low52w,
        bullScore: data.bullScore,
      })
    : null

  const scoreColor = ltScore
    ? ltScore.score >= 70 ? 'var(--acc)' : ltScore.score >= 40 ? 'var(--acc3)' : 'var(--red)'
    : 'var(--muted)'

  const tabKey = chatKey('longterm', ticker || 'default')
  const context = {
    tab: 'longterm',
    ticker,
    tickerData: data
      ? {
          ...data,
          ltScore: ltScore?.score,
          criteria: ltScore?.criteria,
        }
      : undefined,
  }

  return (
    <div style={{ display: 'flex', gap: 16, padding: 24, height: 'calc(100vh - 112px)' }}>
      <div style={{ flex: 2, display: 'flex', flexDirection: 'column', gap: 16, overflowY: 'auto' }}>
        <div style={{
          background: 'var(--s2)', border: '1px solid var(--border)',
          borderRadius: 12, padding: '16px 20px',
        }}>
          <div style={{ display: 'flex', gap: 8, marginBottom: 12 }}>
            <input
              value={input}
              onChange={e => setInput(e.target.value.toUpperCase())}
              onKeyDown={e => e.key === 'Enter' && handleEvaluate()}
              placeholder="Ticker (es. VWCE.DE, AAPL)"
              style={{
                flex: 1, background: 'var(--s3)', border: '1px solid var(--border)',
                borderRadius: 8, padding: '10px 14px', color: 'var(--text)',
                fontFamily: 'JetBrains Mono', fontSize: 14, outline: 'none',
              }}
            />
            <button
              onClick={handleEvaluate}
              style={{
                padding: '10px 20px', borderRadius: 8,
                background: 'var(--acc3)', border: 'none',
                color: '#07080a', fontFamily: 'Syne', fontWeight: 700,
                fontSize: 13, cursor: 'pointer',
              }}
            >
              Valuta
            </button>
          </div>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6 }}>
            {['VWCE.DE', 'IWDA.AS', 'QQQ', 'SPY', 'AAPL', 'MSFT', 'NVDA'].map(t => (
              <button
                key={t}
                onClick={() => { setInput(t); setTicker(t) }}
                style={{
                  padding: '3px 10px', borderRadius: 20,
                  border: '1px solid var(--border2)',
                  background: ticker === t ? 'var(--acc3)' : 'var(--s3)',
                  color: ticker === t ? '#07080a' : 'var(--muted2)',
                  fontSize: 12, cursor: 'pointer', fontFamily: 'JetBrains Mono',
                }}
              >
                {t}
              </button>
            ))}
          </div>
        </div>

        {isLoading && ticker && (
          <div style={{ textAlign: 'center', padding: 40, color: 'var(--muted)', fontFamily: 'Syne' }}>
            Analisi lungo termine per {ticker}…
          </div>
        )}

        {error && (
          <div style={{
            padding: 20, background: 'var(--s2)', borderRadius: 12,
            border: '1px solid var(--red)', color: 'var(--red)', fontFamily: 'Syne',
          }}>
            Errore nel caricamento. Verifica il ticker.
          </div>
        )}

        {data && ltScore && (
          <>
            <div style={{
              background: 'var(--s2)', border: '1px solid var(--border)',
              borderRadius: 12, padding: '20px 24px',
              display: 'flex', alignItems: 'center', gap: 24,
            }}>
              <div style={{ position: 'relative', width: 100, height: 100 }}>
                <svg viewBox="0 0 100 100" width={100} height={100}>
                  <circle cx="50" cy="50" r="45" fill="none" stroke="var(--s3)" strokeWidth="10" />
                  <motion.circle
                    cx="50" cy="50" r="45"
                    fill="none" stroke={scoreColor} strokeWidth="10"
                    strokeDasharray={`${ltScore.score * 2.83} 283`}
                    strokeDashoffset="0"
                    transform="rotate(-90 50 50)"
                    initial={{ strokeDasharray: '0 283' }}
                    animate={{ strokeDasharray: `${ltScore.score * 2.83} 283` }}
                    transition={{ duration: 1, ease: 'easeOut' }}
                  />
                </svg>
                <div style={{
                  position: 'absolute', inset: 0, display: 'flex',
                  alignItems: 'center', justifyContent: 'center',
                  fontFamily: 'Instrument Serif', fontSize: 24, color: scoreColor,
                }}>
                  {ltScore.score}
                </div>
              </div>
              <div>
                <div style={{ fontFamily: 'Syne', fontWeight: 800, fontSize: 20, color: 'var(--text)' }}>
                  {data.ticker} — Lungo Termine
                </div>
                <div style={{ fontFamily: 'Syne', fontSize: 14, color: scoreColor, marginTop: 4 }}>
                  {ltScore.score >= 70 ? '✅ Adatto per investimento DCA' : ltScore.score >= 40 ? '⚠️ Situazione mista — valutare con attenzione' : '❌ Non ideale per investimento a lungo termine ora'}
                </div>
                <div style={{ fontFamily: 'Syne', fontSize: 13, color: 'var(--muted)', marginTop: 4 }}>
                  Prezzo attuale: {formatNumber(data.price, 2)} {data.currency}
                </div>
              </div>
            </div>

            <div style={{
              background: 'var(--s2)', border: '1px solid var(--border)',
              borderRadius: 12, padding: '16px 20px',
            }}>
              <div style={{ fontFamily: 'Syne', fontWeight: 700, fontSize: 14, color: 'var(--text)', marginBottom: 12 }}>
                Checklist Criteri
              </div>
              {ltScore.criteria.map(c => (
                <div key={c.label} style={{
                  display: 'flex', alignItems: 'center', justifyContent: 'space-between',
                  padding: '8px 0', borderBottom: '1px solid var(--border)',
                }}>
                  <span style={{ fontFamily: 'Syne', fontSize: 13, color: 'var(--text)', flex: 1 }}>
                    {c.value}
                  </span>
                  <span style={{
                    fontFamily: 'JetBrains Mono', fontSize: 12,
                    color: c.passed ? 'var(--acc)' : 'var(--red)',
                    fontWeight: 700, minWidth: 60, textAlign: 'right',
                  }}>
                    +{c.points} pt
                  </span>
                </div>
              ))}
            </div>

            <div style={{
              background: 'var(--s2)', border: '1px solid var(--acc3)',
              borderRadius: 12, padding: '16px 20px',
            }}>
              <div style={{ fontFamily: 'Syne', fontWeight: 700, fontSize: 14, color: 'var(--acc3)', marginBottom: 8 }}>
                📅 Strategia DCA
              </div>
              <div style={{ fontFamily: 'Syne', fontSize: 13, color: 'var(--text)', lineHeight: 1.7 }}>
                <strong>Frequenza consigliata:</strong> {data.volatility > 40 ? 'Mensile' : 'Trimestrale'} —{' '}
                {data.volatility > 40
                  ? 'Alta volatilità: acquisti mensili per mediare meglio il costo medio.'
                  : 'Volatilità contenuta: acquisti trimestrali sono sufficienti.'}
              </div>
              <div style={{ marginTop: 12, display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 8 }}>
                <div style={{ background: 'var(--s3)', borderRadius: 8, padding: '10px', textAlign: 'center' }}>
                  <div style={{ fontFamily: 'Syne', fontSize: 11, color: 'var(--muted)', marginBottom: 4 }}>Prezzo attuale</div>
                  <div style={{ fontFamily: 'JetBrains Mono', fontSize: 15, color: 'var(--text)' }}>{formatNumber(data.price, 2)}</div>
                </div>
                <div style={{ background: 'var(--s3)', borderRadius: 8, padding: '10px', textAlign: 'center' }}>
                  <div style={{ fontFamily: 'Syne', fontSize: 11, color: 'var(--muted)', marginBottom: 4 }}>SMA 200</div>
                  <div style={{ fontFamily: 'JetBrains Mono', fontSize: 15, color: data.price > data.sma200 ? 'var(--acc)' : 'var(--red)' }}>
                    {formatNumber(data.sma200, 2)}
                  </div>
                </div>
                <div style={{ background: 'var(--s3)', borderRadius: 8, padding: '10px', textAlign: 'center' }}>
                  <div style={{ fontFamily: 'Syne', fontSize: 11, color: 'var(--muted)', marginBottom: 4 }}>vs SMA200</div>
                  <div style={{ fontFamily: 'JetBrains Mono', fontSize: 15, color: data.price > data.sma200 ? 'var(--acc)' : 'var(--red)' }}>
                    {formatPct(((data.price - data.sma200) / data.sma200) * 100)}
                  </div>
                </div>
              </div>
            </div>
          </>
        )}

        {!ticker && (
          <div style={{ textAlign: 'center', padding: 60, color: 'var(--muted)', fontFamily: 'Syne', fontSize: 15 }}>
            Inserisci un ticker per la valutazione a lungo termine
          </div>
        )}
      </div>

      <div style={{ flex: 1, minWidth: 320 }}>
        <ChatPanel
          tabKey={tabKey}
          context={context}
          quickActions={['È adatto per DCA?', 'Qual è il momento migliore per entrare?', 'Quanto dovrei investire?']}
          placeholder="Chiedimi dell'investimento a lungo termine…"
        />
      </div>
    </div>
  )
}
