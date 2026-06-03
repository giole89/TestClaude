import { useQuery } from '@tanstack/react-query'
import axios from 'axios'
import { API_BASE, INDICES } from '@/lib/constants'
import { QuoteData } from '@/hooks/useQuote'
import { formatNumber, formatPct, colorForChange } from '@/lib/formatters'

// Ticker macro aggiuntivi
const MACRO_TICKERS = [
  ...INDICES.map(i => i.ticker),
  '^TNX',   // US 10Y Yield
  '^TYX',   // US 30Y Yield
  'BTC-USD',
  'ETH-USD',
]

const MACRO_LABELS: Record<string, string> = {
  '^GSPC': 'S&P 500',
  '^NDX': 'Nasdaq 100',
  '^DJI': 'Dow Jones',
  '^STOXX50E': 'Euro Stoxx 50',
  'FTSEMIB.MI': 'FTSE MIB',
  '^VIX': 'VIX',
  'EURUSD=X': 'EUR/USD',
  'GC=F': 'Oro',
  'CL=F': 'WTI Oil',
  '^TNX': 'US 10Y Yield',
  '^TYX': 'US 30Y Yield',
  'BTC-USD': 'Bitcoin',
  'ETH-USD': 'Ethereum',
}

const SECTIONS: Array<{
  title: string
  tickers: string[]
}> = [
  { title: 'Indici', tickers: ['^GSPC', '^NDX', '^DJI', '^STOXX50E', 'FTSEMIB.MI'] },
  { title: 'Valute & Commodity', tickers: ['EURUSD=X', 'GC=F', 'CL=F', '^VIX'] },
  { title: 'Crypto', tickers: ['BTC-USD', 'ETH-USD'] },
  { title: 'Tassi USA', tickers: ['^TNX', '^TYX'] },
]

function MacroCard({ q, label }: { q: QuoteData; label: string }) {
  const isPositive = q.dayChangePct > 0
  const color = colorForChange(q.dayChangePct)

  return (
    <div style={{
      background: 'var(--s3)', border: '1px solid var(--border)', borderRadius: 10,
      padding: '14px 16px', display: 'flex', flexDirection: 'column', gap: 6,
    }}>
      <div style={{ fontFamily: 'Syne', fontSize: 11, color: 'var(--muted)', fontWeight: 700 }}>{label}</div>
      <div style={{ fontFamily: 'Instrument Serif', fontSize: 24, color: 'var(--text)' }}>
        {q.price != null ? formatNumber(q.price, q.price < 10 ? 4 : 2) : '—'}
      </div>
      <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
        <span style={{ fontFamily: 'JetBrains Mono', fontSize: 12, color, fontWeight: 700 }}>
          {formatPct(q.dayChangePct)}
        </span>
        {q.ytdChangePct != null && (
          <span style={{ fontFamily: 'JetBrains Mono', fontSize: 11, color: 'var(--muted)' }}>
            YTD {formatPct(q.ytdChangePct)}
          </span>
        )}
      </div>
    </div>
  )
}

export function MacroPage() {
  const { data: quotes, isLoading } = useQuery<QuoteData[]>({
    queryKey: ['macroBatch'],
    queryFn: () =>
      axios.get(`${API_BASE}/api/batch`, { params: { tickers: MACRO_TICKERS.join(',') } }).then(r => r.data),
    staleTime: 60_000,
    refetchInterval: 60_000,
    retry: 2,
  })

  const quoteMap = Object.fromEntries((quotes ?? []).map(q => [q.ticker, q]))

  // Sentiment globale basato su % indici in positivo
  const indexTickers = ['^GSPC', '^NDX', '^DJI', '^STOXX50E', 'FTSEMIB.MI']
  const indexQuotes = indexTickers.map(t => quoteMap[t]).filter(Boolean)
  const posCount = indexQuotes.filter(q => q.dayChangePct > 0).length
  const sentimentPct = indexQuotes.length > 0 ? (posCount / indexQuotes.length) * 100 : 0
  const sentimentLabel = sentimentPct >= 60 ? 'Risk On 🟢' : sentimentPct >= 40 ? 'Neutro 🟡' : 'Risk Off 🔴'
  const sentimentColor = sentimentPct >= 60 ? 'var(--acc)' : sentimentPct >= 40 ? '#f59e0b' : 'var(--red)'

  return (
    <div style={{ padding: 24, overflowY: 'auto', height: 'calc(100vh - 112px)' }}>
      {/* Header + Sentiment */}
      <div style={{
        display: 'flex', justifyContent: 'space-between', alignItems: 'center',
        marginBottom: 20,
      }}>
        <div style={{ fontFamily: 'Syne', fontWeight: 700, fontSize: 18, color: 'var(--text)' }}>
          Macro Dashboard
        </div>
        {!isLoading && indexQuotes.length > 0 && (
          <div style={{
            background: 'var(--s2)', border: '1px solid var(--border)', borderRadius: 10,
            padding: '10px 20px', display: 'flex', flexDirection: 'column', alignItems: 'center',
          }}>
            <div style={{ fontFamily: 'Syne', fontSize: 11, color: 'var(--muted)' }}>Sentiment globale</div>
            <div style={{ fontFamily: 'Instrument Serif', fontSize: 20, color: sentimentColor }}>{sentimentLabel}</div>
            <div style={{ fontFamily: 'JetBrains Mono', fontSize: 11, color: 'var(--muted)' }}>
              {posCount}/{indexQuotes.length} indici positivi
            </div>
          </div>
        )}
      </div>

      {isLoading && (
        <div style={{ textAlign: 'center', padding: 40, color: 'var(--muted)', fontFamily: 'Syne' }}>
          Caricamento dati macro…
        </div>
      )}

      {!isLoading && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 24 }}>
          {SECTIONS.map(section => {
            const sectionQuotes = section.tickers
              .map(t => ({ ticker: t, quote: quoteMap[t] }))
              .filter(({ quote }) => quote != null)

            return (
              <div key={section.title}>
                <div style={{ fontFamily: 'Syne', fontWeight: 700, fontSize: 13, color: 'var(--text)', marginBottom: 12 }}>
                  {section.title}
                </div>
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(180px, 1fr))', gap: 10 }}>
                  {sectionQuotes.map(({ ticker, quote }) => (
                    <MacroCard
                      key={ticker}
                      q={quote}
                      label={MACRO_LABELS[ticker] || ticker}
                    />
                  ))}
                  {sectionQuotes.length === 0 && (
                    <div style={{ color: 'var(--muted)', fontFamily: 'Syne', fontSize: 12, padding: 10 }}>
                      Dati non disponibili
                    </div>
                  )}
                </div>
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}
