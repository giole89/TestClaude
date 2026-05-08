import { useState, useMemo } from 'react'
import {
  LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip,
  ResponsiveContainer, ReferenceLine,
} from 'recharts'
import { HistoryPoint } from '@/hooks/useHistory'
import { calcSMA } from '@/lib/indicators'
import { formatNumber, formatDateShort } from '@/lib/formatters'

type Range = '1m' | '3m' | '6m' | '1y'

interface Props {
  history: HistoryPoint[]
  ticker: string
}

const RANGE_DAYS: Record<Range, number> = { '1m': 21, '3m': 63, '6m': 126, '1y': 252 }

export function PriceChart({ history, ticker }: Props) {
  const [range, setRange] = useState<Range>('1y')

  const sliced = useMemo(() => {
    const days = RANGE_DAYS[range]
    return history.slice(-days)
  }, [history, range])

  const closes = useMemo(() => sliced.map(h => h.close), [sliced])

  const chartData = useMemo(() => {
    const sma20arr = closes.map((_, i) => calcSMA(closes.slice(0, i + 1), Math.min(20, i + 1)))
    const sma50arr = closes.map((_, i) => calcSMA(closes.slice(0, i + 1), Math.min(50, i + 1)))
    return sliced.map((h, i) => ({
      date: h.date,
      close: h.close,
      sma20: i >= 19 ? sma20arr[i] : null,
      sma50: i >= 49 ? sma50arr[i] : null,
    }))
  }, [sliced, closes])

  const minPrice = Math.min(...closes) * 0.995
  const maxPrice = Math.max(...closes) * 1.005

  return (
    <div style={{
      background: 'var(--s2)', border: '1px solid var(--border)',
      borderRadius: 12, padding: '16px',
    }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 12 }}>
        <span style={{ fontFamily: 'Syne', fontWeight: 700, fontSize: 14, color: 'var(--text)' }}>
          Grafico Prezzi — {ticker}
        </span>
        <div style={{ display: 'flex', gap: 4 }}>
          {(['1m', '3m', '6m', '1y'] as Range[]).map(r => (
            <button
              key={r}
              onClick={() => setRange(r)}
              style={{
                padding: '3px 10px', borderRadius: 6, border: 'none',
                cursor: 'pointer', fontFamily: 'JetBrains Mono', fontSize: 11,
                background: range === r ? 'var(--acc)' : 'var(--s3)',
                color: range === r ? '#07080a' : 'var(--muted2)',
              }}
            >
              {r.toUpperCase()}
            </button>
          ))}
        </div>
      </div>

      <div style={{ display: 'flex', gap: 12, marginBottom: 8 }}>
        <span style={{ fontFamily: 'Syne', fontSize: 11, color: 'var(--text)' }}>
          <span style={{ display: 'inline-block', width: 16, height: 2, background: 'var(--acc)', marginRight: 4, verticalAlign: 'middle' }} />
          Prezzo
        </span>
        <span style={{ fontFamily: 'Syne', fontSize: 11, color: '#e8c542' }}>
          <span style={{ display: 'inline-block', width: 16, height: 2, background: '#e8c542', marginRight: 4, verticalAlign: 'middle', borderTop: '2px dashed #e8c542' }} />
          SMA 20
        </span>
        <span style={{ fontFamily: 'Syne', fontSize: 11, color: 'var(--acc2)' }}>
          <span style={{ display: 'inline-block', width: 16, height: 2, background: 'var(--acc2)', marginRight: 4, verticalAlign: 'middle', borderTop: '2px dashed var(--acc2)' }} />
          SMA 50
        </span>
      </div>

      <ResponsiveContainer width="100%" height={280}>
        <LineChart data={chartData} margin={{ top: 4, right: 4, left: -20, bottom: 0 }}>
          <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" />
          <XAxis
            dataKey="date"
            tickFormatter={formatDateShort}
            tick={{ fill: 'var(--muted)', fontSize: 10, fontFamily: 'JetBrains Mono' }}
            tickLine={false}
            interval="preserveStartEnd"
          />
          <YAxis
            domain={[minPrice, maxPrice]}
            tickFormatter={v => formatNumber(v, 0)}
            tick={{ fill: 'var(--muted)', fontSize: 10, fontFamily: 'JetBrains Mono' }}
            tickLine={false}
            axisLine={false}
          />
          <Tooltip
            contentStyle={{
              background: 'var(--s3)', border: '1px solid var(--border)',
              borderRadius: 8, fontFamily: 'JetBrains Mono', fontSize: 12,
            }}
            labelFormatter={formatDateShort}
            formatter={(val: number) => [formatNumber(val, 2), '']}
          />
          <Line type="monotone" dataKey="close" stroke="var(--acc)" dot={false} strokeWidth={1.5} name="Prezzo" />
          <Line type="monotone" dataKey="sma20" stroke="#e8c542" dot={false} strokeWidth={1} strokeDasharray="4 2" name="SMA 20" connectNulls />
          <Line type="monotone" dataKey="sma50" stroke="var(--acc2)" dot={false} strokeWidth={1} strokeDasharray="4 2" name="SMA 50" connectNulls />
        </LineChart>
      </ResponsiveContainer>
    </div>
  )
}
