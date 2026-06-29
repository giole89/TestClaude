import { useState, useMemo } from 'react'
import {
  ComposedChart, Line, Bar, XAxis, YAxis, CartesianGrid, Tooltip,
  ResponsiveContainer,
} from 'recharts'
import { HistoryPoint } from '@/hooks/useHistory'
import { calcSMA } from '@/lib/indicators'
import { formatNumber, formatDateShort } from '@/lib/formatters'

type Range = '1m' | '3m' | '6m' | '1y'
type ChartType = 'line' | 'candle'

interface Props {
  history: HistoryPoint[]
  ticker: string
}

const RANGE_DAYS: Record<Range, number> = { '1m': 21, '3m': 63, '6m': 126, '1y': 252 }

function CandleShape(props: any) {
  const { x, y, width, height, payload } = props
  const { open, close, high, low } = payload
  const isUp = close >= open
  const color = isUp ? 'var(--acc)' : 'var(--red)'
  const range = high - low || 1
  const topRatio = (high - Math.max(open, close)) / range
  const bottomRatio = (high - Math.min(open, close)) / range
  const bodyY = y + height * topRatio
  const bodyHeight = Math.max(height * (bottomRatio - topRatio), 1)
  const cx = x + width / 2
  return (
    <g>
      <line x1={cx} x2={cx} y1={y} y2={y + height} stroke={color} strokeWidth={1} />
      <rect x={x + width * 0.15} y={bodyY} width={width * 0.7} height={bodyHeight} fill={color} />
    </g>
  )
}

function CandleTooltip({ active, payload, label }: any) {
  if (!active || !payload || !payload.length) return null
  const d = payload[0].payload
  return (
    <div style={{
      background: 'var(--s3)', border: '1px solid var(--border)', borderRadius: 8,
      padding: 8, fontFamily: 'JetBrains Mono', fontSize: 12,
    }}>
      <div style={{ color: 'var(--muted)', marginBottom: 4 }}>{formatDateShort(label)}</div>
      <div>O: {formatNumber(d.open, 2)}</div>
      <div>H: {formatNumber(d.high, 2)}</div>
      <div>L: {formatNumber(d.low, 2)}</div>
      <div>C: {formatNumber(d.close, 2)}</div>
    </div>
  )
}

export function PriceChart({ history, ticker }: Props) {
  const [range, setRange] = useState<Range>('1y')
  const [chartType, setChartType] = useState<ChartType>('line')

  const sliced = useMemo(() => {
    const days = RANGE_DAYS[range]
    return history.slice(-days)
  }, [history, range])

  const closes = useMemo(() => sliced.map(h => h.close), [sliced])
  const highs = useMemo(() => sliced.map(h => h.high), [sliced])
  const lows = useMemo(() => sliced.map(h => h.low), [sliced])

  const chartData = useMemo(() => {
    const sma20arr = closes.map((_, i) => calcSMA(closes.slice(0, i + 1), Math.min(20, i + 1)))
    const sma50arr = closes.map((_, i) => calcSMA(closes.slice(0, i + 1), Math.min(50, i + 1)))
    return sliced.map((h, i) => ({
      date: h.date,
      open: h.open,
      high: h.high,
      low: h.low,
      close: h.close,
      range: [h.low, h.high],
      sma20: i >= 19 ? sma20arr[i] : null,
      sma50: i >= 49 ? sma50arr[i] : null,
    }))
  }, [sliced, closes])

  const minPrice = chartType === 'candle' ? Math.min(...lows) * 0.995 : Math.min(...closes) * 0.995
  const maxPrice = chartType === 'candle' ? Math.max(...highs) * 1.005 : Math.max(...closes) * 1.005

  return (
    <div style={{
      background: 'var(--s2)', border: '1px solid var(--border)',
      borderRadius: 12, padding: '16px',
    }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 12 }}>
        <span style={{ fontFamily: 'Syne', fontWeight: 700, fontSize: 14, color: 'var(--text)' }}>
          Grafico Prezzi — {ticker}
        </span>
        <div style={{ display: 'flex', gap: 8 }}>
          <div style={{ display: 'flex', gap: 4 }}>
            {(['line', 'candle'] as ChartType[]).map(t => (
              <button
                key={t}
                onClick={() => setChartType(t)}
                style={{
                  padding: '3px 10px', borderRadius: 6, border: 'none',
                  cursor: 'pointer', fontFamily: 'JetBrains Mono', fontSize: 11,
                  background: chartType === t ? 'var(--acc)' : 'var(--s3)',
                  color: chartType === t ? '#07080a' : 'var(--muted2)',
                }}
              >
                {t === 'line' ? 'Linea' : 'Candele'}
              </button>
            ))}
          </div>
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
      </div>

      <div style={{ display: 'flex', gap: 12, marginBottom: 8 }}>
        {chartType === 'line' ? (
          <span style={{ fontFamily: 'Syne', fontSize: 11, color: 'var(--text)' }}>
            <span aria-hidden="true" style={{ display: 'inline-block', width: 16, height: 2, background: 'var(--acc)', marginRight: 4, verticalAlign: 'middle' }} />
            Prezzo
          </span>
        ) : (
          <span style={{ fontFamily: 'Syne', fontSize: 11, color: 'var(--text)' }}>
            <span aria-hidden="true" style={{ display: 'inline-block', width: 10, height: 10, background: 'var(--acc)', marginRight: 4, verticalAlign: 'middle' }} />
            Candele O/H/L/C
          </span>
        )}
        <span style={{ fontFamily: 'Syne', fontSize: 11, color: '#e8c542' }}>
          <span aria-hidden="true" style={{ display: 'inline-block', width: 16, height: 2, background: '#e8c542', marginRight: 4, verticalAlign: 'middle', borderTop: '2px dashed #e8c542' }} />
          SMA 20
        </span>
        <span style={{ fontFamily: 'Syne', fontSize: 11, color: 'var(--acc2)' }}>
          <span aria-hidden="true" style={{ display: 'inline-block', width: 16, height: 2, background: 'var(--acc2)', marginRight: 4, verticalAlign: 'middle', borderTop: '2px dashed var(--acc2)' }} />
          SMA 50
        </span>
      </div>

      <div role="img" aria-label={`Grafico prezzi ${chartType === 'candle' ? 'a candele' : 'a linea'} di ${ticker} con medie mobili SMA 20 e SMA 50, periodo ${range}`}>
        <ResponsiveContainer width="100%" height={280}>
        <ComposedChart data={chartData} margin={{ top: 4, right: 4, left: -20, bottom: 0 }}>
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
          {chartType === 'candle' ? (
            <Tooltip content={<CandleTooltip />} />
          ) : (
            <Tooltip
              contentStyle={{
                background: 'var(--s3)', border: '1px solid var(--border)',
                borderRadius: 8, fontFamily: 'JetBrains Mono', fontSize: 12,
              }}
              labelFormatter={formatDateShort}
              formatter={(val: number) => [formatNumber(val, 2), '']}
            />
          )}
          {chartType === 'line' && (
            <Line type="monotone" dataKey="close" stroke="var(--acc)" dot={false} strokeWidth={1.5} name="Prezzo" />
          )}
          {chartType === 'candle' && (
            <Bar dataKey="range" shape={<CandleShape />} isAnimationActive={false} />
          )}
          <Line type="monotone" dataKey="sma20" stroke="#e8c542" dot={false} strokeWidth={1} strokeDasharray="4 2" name="SMA 20" connectNulls />
          <Line type="monotone" dataKey="sma50" stroke="var(--acc2)" dot={false} strokeWidth={1} strokeDasharray="4 2" name="SMA 50" connectNulls />
        </ComposedChart>
        </ResponsiveContainer>
      </div>
    </div>
  )
}
