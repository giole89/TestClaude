import { motion } from 'framer-motion'
import { FullQuoteData } from '@/hooks/useQuote'
import { predictScenarios } from '@/lib/indicators'
import { formatNumber, formatPct } from '@/lib/formatters'

interface Props {
  data: FullQuoteData
}

export function PredictionCard({ data }: Props) {
  const scenarios = predictScenarios(data.price, data.volatility, data.momentum30)
  const isBullish = data.momentum30 > 0 && data.bullScore >= 50
  const bullPct = Math.min(95, Math.max(5, data.bullScore))

  return (
    <div style={{
      background: 'var(--s2)', border: '1px solid var(--border)',
      borderRadius: 12, padding: '16px 20px',
    }}>
      <div style={{ marginBottom: 16 }}>
        <div style={{
          fontFamily: 'Syne', fontWeight: 800, fontSize: 18,
          color: isBullish ? 'var(--acc)' : 'var(--red)',
          marginBottom: 8,
        }}>
          {isBullish ? '📈 RIALZISTA' : '📉 RIBASSISTA'}
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <span style={{ fontFamily: 'Syne', fontSize: 13, color: 'var(--muted)' }}>
            Probabilità rialzo:
          </span>
          <div style={{ flex: 1, height: 8, background: 'var(--s3)', borderRadius: 4, overflow: 'hidden' }}>
            <motion.div
              initial={{ width: 0 }}
              animate={{ width: `${bullPct}%` }}
              transition={{ duration: 0.8, ease: 'easeOut' }}
              style={{
                height: '100%',
                background: bullPct > 60 ? 'var(--acc)' : bullPct < 40 ? 'var(--red)' : 'var(--acc3)',
                borderRadius: 4,
              }}
            />
          </div>
          <span style={{ fontFamily: 'JetBrains Mono', fontWeight: 700, fontSize: 14, color: 'var(--text)' }}>
            {bullPct}%
          </span>
        </div>
      </div>

      <div style={{ fontFamily: 'Syne', fontSize: 12, color: 'var(--muted)', marginBottom: 10 }}>
        Scenari a 30 giorni
      </div>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 8 }}>
        {[
          { label: 'Ottimista', price: scenarios.bull, color: 'var(--acc)' },
          { label: 'Base', price: scenarios.base, color: 'var(--acc2)' },
          { label: 'Pessimista', price: scenarios.bear, color: 'var(--red)' },
        ].map(s => {
          const pct = ((s.price - data.price) / data.price) * 100
          return (
            <div key={s.label} style={{
              background: 'var(--s3)', borderRadius: 8, padding: '10px 12px', textAlign: 'center',
            }}>
              <div style={{ fontFamily: 'Syne', fontSize: 11, color: 'var(--muted)', marginBottom: 4 }}>
                {s.label}
              </div>
              <div style={{ fontFamily: 'Instrument Serif', fontSize: 18, color: s.color }}>
                {formatNumber(s.price, 2)}
              </div>
              <div style={{ fontFamily: 'JetBrains Mono', fontSize: 11, color: s.color, marginTop: 2 }}>
                {formatPct(pct)}
              </div>
            </div>
          )
        })}
      </div>
    </div>
  )
}
