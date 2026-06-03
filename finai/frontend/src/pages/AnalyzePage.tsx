import { useState, useEffect } from 'react'
import { useFullQuote } from '@/hooks/useQuote'
import { useNews } from '@/hooks/useNews'
import { StockHero } from '@/components/analyze/StockHero'
import { PriceChart } from '@/components/analyze/PriceChart'
import { SignalBadge } from '@/components/analyze/SignalBadge'
import { PredictionCard } from '@/components/analyze/PredictionCard'
import { ChatPanel } from '@/components/chat/ChatPanel'
import { SearchInput } from '@/components/common/SearchInput'
import { chatKey, useChatStore } from '@/store/useChatStore'
import { WATCHLIST_QUICK } from '@/lib/constants'
import { formatNumber } from '@/lib/formatters'

// ── DCF Semplificato ─────────────────────────────────────────────────────────

interface DcfResult {
  fairValue: number
  upside: number
}

function calcDcf(eps: number, growthRate: number, discountRate: number, terminalGrowth: number): DcfResult {
  // FCF year i = EPS * (1+g)^i
  // DCF = sum(FCF_i / (1+r)^i, i=1..5) + terminal_value / (1+r)^5
  let dcf = 0
  for (let i = 1; i <= 5; i++) {
    const fcf = eps * Math.pow(1 + growthRate / 100, i)
    dcf += fcf / Math.pow(1 + discountRate / 100, i)
  }
  const fcf5 = eps * Math.pow(1 + growthRate / 100, 5)
  const terminalValue = fcf5 * (1 + terminalGrowth / 100) / ((discountRate / 100) - (terminalGrowth / 100))
  const pvTerminal = terminalValue / Math.pow(1 + discountRate / 100, 5)
  const fairValue = dcf + pvTerminal
  return { fairValue, upside: 0 }
}

function DcfSection({ data }: { data: { price: number | null; pe: number | null } }) {
  const eps = data.price && data.pe ? data.price / data.pe : null
  const [growthRate, setGrowthRate] = useState('10')
  const [discountRate, setDiscountRate] = useState('9')
  const [terminalGrowth, setTerminalGrowth] = useState('3')

  if (!eps || eps <= 0) return (
    <div style={{ background: 'var(--s2)', border: '1px solid var(--border)', borderRadius: 12, padding: '16px 20px' }}>
      <div style={{ fontFamily: 'Syne', fontWeight: 700, fontSize: 13, color: 'var(--text)', marginBottom: 8 }}>DCF Semplificato</div>
      <div style={{ color: 'var(--muted)', fontFamily: 'Syne', fontSize: 13 }}>
        Dati EPS non disponibili per questo titolo.
      </div>
    </div>
  )

  const g = parseFloat(growthRate) || 10
  const r = parseFloat(discountRate) || 9
  const tg = parseFloat(terminalGrowth) || 3
  const { fairValue } = calcDcf(eps, g, r, tg)
  const upside = data.price ? ((fairValue - data.price) / data.price) * 100 : 0

  const semaforo = upside > 20 ? { color: 'var(--acc)', label: 'Sottovalutato' }
    : upside > -20 ? { color: '#f59e0b', label: 'Equamente valutato' }
    : { color: 'var(--red)', label: 'Sopravvalutato' }

  const inputStyle = {
    background: 'var(--s3)', border: '1px solid var(--border)', borderRadius: 6,
    padding: '6px 10px', color: 'var(--text)', fontFamily: 'JetBrains Mono',
    fontSize: 12, outline: 'none', width: 80,
  }

  return (
    <div style={{ background: 'var(--s2)', border: '1px solid var(--border)', borderRadius: 12, padding: '16px 20px' }}>
      <div style={{ fontFamily: 'Syne', fontWeight: 700, fontSize: 13, color: 'var(--text)', marginBottom: 12 }}>
        DCF Semplificato
      </div>
      <div style={{ display: 'flex', gap: 20, flexWrap: 'wrap', marginBottom: 16, alignItems: 'flex-end' }}>
        <div>
          <div style={{ fontFamily: 'Syne', fontSize: 10, color: 'var(--muted)', marginBottom: 3 }}>EPS corrente</div>
          <div style={{ fontFamily: 'JetBrains Mono', fontSize: 14, color: 'var(--text)' }}>{formatNumber(eps, 2)}</div>
        </div>
        {[
          { label: 'Crescita 5y %', val: growthRate, set: setGrowthRate },
          { label: 'Tasso sconto %', val: discountRate, set: setDiscountRate },
          { label: 'Terminal growth %', val: terminalGrowth, set: setTerminalGrowth },
        ].map(f => (
          <div key={f.label}>
            <div style={{ fontFamily: 'Syne', fontSize: 10, color: 'var(--muted)', marginBottom: 3 }}>{f.label}</div>
            <input type="number" value={f.val} onChange={e => f.set(e.target.value)} style={inputStyle} />
          </div>
        ))}
      </div>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 10 }}>
        <div style={{ background: 'var(--s3)', borderRadius: 8, padding: '12px 14px', textAlign: 'center' }}>
          <div style={{ fontFamily: 'Syne', fontSize: 10, color: 'var(--muted)', marginBottom: 4 }}>Fair Value stimato</div>
          <div style={{ fontFamily: 'Instrument Serif', fontSize: 22, color: 'var(--text)' }}>{formatNumber(fairValue, 2)}</div>
        </div>
        <div style={{ background: 'var(--s3)', borderRadius: 8, padding: '12px 14px', textAlign: 'center' }}>
          <div style={{ fontFamily: 'Syne', fontSize: 10, color: 'var(--muted)', marginBottom: 4 }}>Upside/Downside</div>
          <div style={{ fontFamily: 'Instrument Serif', fontSize: 22, color: upside >= 0 ? 'var(--acc)' : 'var(--red)' }}>
            {(upside >= 0 ? '+' : '')}{formatNumber(upside, 1)}%
          </div>
        </div>
        <div style={{ background: 'var(--s3)', borderRadius: 8, padding: '12px 14px', textAlign: 'center' }}>
          <div style={{ fontFamily: 'Syne', fontSize: 10, color: 'var(--muted)', marginBottom: 4 }}>Valutazione</div>
          <div style={{ fontFamily: 'Instrument Serif', fontSize: 18, color: semaforo.color }}>{semaforo.label}</div>
        </div>
      </div>
      <div style={{ marginTop: 8, fontFamily: 'Syne', fontSize: 10, color: 'var(--muted)', fontStyle: 'italic' }}>
        * Modello DCF semplificato indicativo. Non costituisce consulenza finanziaria.
      </div>
    </div>
  )
}

export function AnalyzePage() {
  const [ticker, setTicker] = useState<string | null>(() =>
    sessionStorage.getItem('finai_analyze_ticker') || null
  )
  const { data, isLoading, error } = useFullQuote(ticker)
  const { resetHistory } = useChatStore()
  const news = useNews(ticker ? [ticker] : [], 8)

  useEffect(() => {
    const stored = sessionStorage.getItem('finai_analyze_ticker')
    if (stored && stored !== ticker) setTicker(stored)
  }, [])

  const handleSelect = (t: string) => {
    if (t !== ticker) resetHistory(chatKey('analyze', t))
    setTicker(t)
    sessionStorage.setItem('finai_analyze_ticker', t)
  }

  const tabKey = chatKey('analyze', ticker || 'default')
  const context = data
    ? { tab: 'analyze', ticker: data.ticker, tickerData: { name: data.name, price: data.price, dayChangePct: data.dayChangePct, rsi: data.rsi, sma20: data.sma20, sma50: data.sma50, sma200: data.sma200, volatility: data.volatility, momentum30: data.momentum30, bullScore: data.bullScore, high52w: data.high52w, low52w: data.low52w, rangePosition: data.rangePosition } }
    : { tab: 'analyze' }

  return (
    <div style={{ display: 'flex', gap: 16, padding: 24, height: 'calc(100vh - 112px)' }}>
      <div style={{ flex: 2, display: 'flex', flexDirection: 'column', gap: 16, overflowY: 'auto' }}>
        <div style={{ background: 'var(--s2)', border: '1px solid var(--border)', borderRadius: 12, padding: '16px 20px' }}>
          <SearchInput
            value={ticker ?? ''}
            onSelect={handleSelect}
            placeholder="Cerca per ticker o nome (es. Apple, NVDA, ASML…)"
            style={{ marginBottom: 12 }}
          />
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6 }}>
            {WATCHLIST_QUICK.map(t => (
              <button key={t} onClick={() => handleSelect(t)} style={{ padding: '3px 10px', borderRadius: 20, border: '1px solid var(--border2)', background: ticker === t ? 'var(--acc)' : 'var(--s3)', color: ticker === t ? '#07080a' : 'var(--muted2)', fontSize: 12, cursor: 'pointer', fontFamily: 'JetBrains Mono' }}>
                {t}
              </button>
            ))}
          </div>
        </div>

        {isLoading && ticker && <div style={{ textAlign: 'center', padding: 40, color: 'var(--muted)', fontFamily: 'Syne' }}>Caricamento dati per {ticker}…</div>}
        {error && <div style={{ padding: 20, background: 'var(--s2)', borderRadius: 12, border: '1px solid var(--red)', color: 'var(--red)', fontFamily: 'Syne' }}>Errore nel caricamento di {ticker}. Verifica il ticker e riprova.</div>}
        {data && !isLoading && (
          <>
            <StockHero data={data} />
            <SignalBadge data={data} />
            <PriceChart history={data.history} ticker={data.ticker} />
            <PredictionCard data={data} />
            {/* ── DCF Semplificato ───────────────────── */}
            <DcfSection data={{ price: data.price, pe: data.pe ?? null }} />
            {/* ── News ─────────────────────────────── */}
            <div style={{ background: 'var(--s2)', border: '1px solid var(--border)', borderRadius: 12, padding: '16px 20px' }}>
              <div style={{ fontFamily: 'Syne', fontWeight: 700, fontSize: 13, color: 'var(--text)', marginBottom: 12 }}>
                News
              </div>
              {news.isLoading && <div style={{ color: 'var(--muted)', fontFamily: 'Syne', fontSize: 13 }}>Caricamento news…</div>}
              {!news.isLoading && (news.data ?? []).length === 0 && (
                <div style={{ color: 'var(--muted)', fontFamily: 'Syne', fontSize: 13 }}>Nessuna news disponibile.</div>
              )}
              {!news.isLoading && (news.data ?? []).length > 0 && (
                <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
                  {(news.data ?? []).slice(0, 8).map((n, i) => (
                    <a key={i} href={n.url} target="_blank" rel="noopener noreferrer" style={{ textDecoration: 'none', display: 'block', padding: '10px 12px', background: 'var(--s3)', borderRadius: 8, border: '1px solid var(--border)' }}>
                      <div style={{ fontFamily: 'Syne', fontSize: 12, color: 'var(--text)', marginBottom: 4, lineHeight: 1.4 }}>{n.title}</div>
                      <div style={{ display: 'flex', gap: 8 }}>
                        {n.publisher && <span style={{ fontFamily: 'Syne', fontSize: 10, color: 'var(--muted)' }}>{n.publisher}</span>}
                        {n.publishedAt && <span style={{ fontFamily: 'JetBrains Mono', fontSize: 10, color: 'var(--muted)' }}>{new Date(n.publishedAt).toLocaleDateString('it-IT')}</span>}
                      </div>
                    </a>
                  ))}
                </div>
              )}
            </div>
          </>
        )}
        {!ticker && !isLoading && <div style={{ textAlign: 'center', padding: 60, color: 'var(--muted)', fontFamily: 'Syne', fontSize: 15 }}>Cerca un titolo per iniziare l'analisi</div>}
      </div>
      <div style={{ flex: 1, minWidth: 320 }}>
        <ChatPanel tabKey={tabKey} context={context} quickActions={['Dimmi tutto su questo titolo', 'È un buon momento per comprare?', 'Cosa dice l\'analisi tecnica?', 'Confronta con l\'SP500']} placeholder="Chiedimi del titolo analizzato…" />
      </div>
    </div>
  )
}
