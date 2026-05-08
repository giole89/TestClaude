import { motion } from 'framer-motion'

interface Props {
  pct: number
  posCount: number
  totalCount: number
}

function getSentiment(pct: number): { label: string; emoji: string; color: string } {
  if (pct < 30) return { label: 'Paura', emoji: '😱', color: 'var(--red)' }
  if (pct < 45) return { label: 'Ribassista', emoji: '🔴', color: '#f87171' }
  if (pct < 55) return { label: 'Neutro', emoji: '😐', color: 'var(--muted2)' }
  if (pct < 70) return { label: 'Rialzista', emoji: '🟢', color: 'var(--acc)' }
  return { label: 'Euforico', emoji: '😄', color: '#86efac' }
}

export function SentimentMeter({ pct, posCount, totalCount }: Props) {
  const s = getSentiment(pct)

  return (
    <div style={{
      background: 'var(--s2)', border: '1px solid var(--border)',
      borderRadius: 12, padding: '16px 20px',
    }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 12 }}>
        <div>
          <span style={{ fontSize: 24 }}>{s.emoji}</span>
          <span style={{
            marginLeft: 10, fontFamily: 'Syne', fontWeight: 700, fontSize: 16,
            color: s.color,
          }}>
            {s.label}
          </span>
        </div>
        <span style={{ fontFamily: 'JetBrains Mono', fontSize: 24, fontWeight: 700, color: s.color }}>
          {Math.round(pct)}%
        </span>
      </div>

      <div style={{ height: 8, background: 'var(--s3)', borderRadius: 4, overflow: 'hidden', marginBottom: 8 }}>
        <motion.div
          initial={{ width: 0 }}
          animate={{ width: `${pct}%` }}
          transition={{ duration: 0.8, ease: 'easeOut' }}
          style={{ height: '100%', background: s.color, borderRadius: 4 }}
        />
      </div>

      <p style={{ fontFamily: 'Syne', fontSize: 12, color: 'var(--muted)', margin: 0 }}>
        {posCount}/{totalCount} titoli positivi oggi
      </p>
    </div>
  )
}
