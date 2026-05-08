import { useState } from 'react'
import { QuoteData } from '@/hooks/useQuote'
import { MarketRow } from './MarketRow'

interface Column {
  title: string
  emoji: string
  data: QuoteData[]
}

interface Props {
  topStocks: QuoteData[]
  worstStocks: QuoteData[]
  topEtf: QuoteData[]
  worstEtf: QuoteData[]
  onAnalyze: (ticker: string) => void
}

export function MarketGrid({ topStocks, worstStocks, topEtf, worstEtf, onAnalyze }: Props) {
  const [mode, setMode] = useState<'today' | 'ytd'>('today')

  const columns: Column[] = [
    { title: 'Azioni Top', emoji: '🔥', data: topStocks },
    { title: 'Azioni Worst', emoji: '❄️', data: worstStocks },
    { title: 'ETF Top', emoji: '📦', data: topEtf },
    { title: 'ETF Worst', emoji: '⚠️', data: worstEtf },
  ]

  return (
    <div>
      <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 16 }}>
        <span style={{ fontFamily: 'Syne', fontWeight: 700, fontSize: 14, color: 'var(--text)' }}>
          Andamento
        </span>
        {(['today', 'ytd'] as const).map(m => (
          <button
            key={m}
            onClick={() => setMode(m)}
            style={{
              padding: '4px 12px', borderRadius: 6, border: 'none',
              cursor: 'pointer', fontFamily: 'Syne', fontWeight: 600, fontSize: 12,
              background: mode === m ? 'var(--acc)' : 'var(--s3)',
              color: mode === m ? '#07080a' : 'var(--muted2)',
            }}
          >
            {m === 'today' ? 'Oggi' : 'Anno (YTD)'}
          </button>
        ))}
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 12 }}>
        {columns.map(col => (
          <div key={col.title} style={{
            background: 'var(--s2)', border: '1px solid var(--border)',
            borderRadius: 12, overflow: 'hidden',
          }}>
            <div style={{
              padding: '10px 12px', borderBottom: '1px solid var(--border)',
              fontFamily: 'Syne', fontWeight: 700, fontSize: 13, color: 'var(--text)',
            }}>
              {col.emoji} {col.title}
            </div>
            <div>
              {col.data.map((q, i) => (
                <MarketRow key={q.ticker} rank={i + 1} quote={q} mode={mode} onAnalyze={onAnalyze} />
              ))}
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}
