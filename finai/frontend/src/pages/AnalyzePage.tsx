import { useState, useEffect } from 'react'
import { useFullQuote } from '@/hooks/useQuote'
import { StockHero } from '@/components/analyze/StockHero'
import { PriceChart } from '@/components/analyze/PriceChart'
import { SignalBadge } from '@/components/analyze/SignalBadge'
import { PredictionCard } from '@/components/analyze/PredictionCard'
import { ChatPanel } from '@/components/chat/ChatPanel'
import { SearchInput } from '@/components/common/SearchInput'
import { chatKey, useChatStore } from '@/store/useChatStore'
import { WATCHLIST_QUICK } from '@/lib/constants'

export function AnalyzePage() {
  const [ticker, setTicker] = useState<string | null>(() =>
    sessionStorage.getItem('finai_analyze_ticker') || null
  )
  const { data, isLoading, error } = useFullQuote(ticker)
  const { resetHistory } = useChatStore()

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
