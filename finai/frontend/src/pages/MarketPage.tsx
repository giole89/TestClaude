import { useRef } from 'react'
import { IndexBar } from '@/components/market/IndexBar'
import { SentimentMeter } from '@/components/market/SentimentMeter'
import { MarketGrid } from '@/components/market/MarketGrid'
import { useMarketData } from '@/hooks/useMarketBatch'
import { useAppStore } from '@/store/useAppStore'

export function MarketPage() {
  const { topStocks, worstStocks, topEtf, worstEtf, isLoading, sentimentPct, posCount, totalCount } = useMarketData()
  const { setActiveTab } = useAppStore()
  const analyzeTickerRef = useRef<string | null>(null)

  const handleAnalyze = (ticker: string) => {
    analyzeTickerRef.current = ticker
    sessionStorage.setItem('finai_analyze_ticker', ticker)
    setActiveTab('analyze')
  }

  return (
    <div>
      <IndexBar />
      <div style={{ padding: '20px 24px', display: 'flex', flexDirection: 'column', gap: 20 }}>
        <SentimentMeter pct={sentimentPct} posCount={posCount} totalCount={totalCount} />

        {isLoading ? (
          <div style={{ textAlign: 'center', padding: 40, color: 'var(--muted)' }}>
            <p style={{ fontFamily: 'Syne', fontSize: 14 }}>Caricamento dati di mercato…</p>
          </div>
        ) : (
          <MarketGrid
            topStocks={topStocks}
            worstStocks={worstStocks}
            topEtf={topEtf}
            worstEtf={worstEtf}
            onAnalyze={handleAnalyze}
          />
        )}
      </div>
    </div>
  )
}
