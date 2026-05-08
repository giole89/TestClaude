import { FullQuoteData } from '@/hooks/useQuote'

interface Props {
  data: FullQuoteData
}

function getSignal(data: FullQuoteData): { signal: 'BUY' | 'SELL' | 'HOLD'; reason: string; color: string } {
  const { rsi, sma20, sma50, sma200, price, momentum30, bullScore } = data

  if (bullScore >= 70 && rsi < 70 && momentum30 > 0) {
    return {
      signal: 'BUY',
      reason: `Bull score ${bullScore}/100. Prezzo sopra SMA50 e SMA200, momentum positivo, RSI non in ipercomprato.`,
      color: 'var(--acc)',
    }
  }
  if (bullScore <= 30 || (rsi > 75) || (price < sma50 && price < sma200 && momentum30 < -10)) {
    return {
      signal: 'SELL',
      reason: `Bull score ${bullScore}/100. Segnali tecnici deboli: RSI ${rsi}, prezzo sotto medie mobili chiave.`,
      color: 'var(--red)',
    }
  }
  return {
    signal: 'HOLD',
    reason: `Bull score ${bullScore}/100. Scenario misto: attendere conferma direzionale prima di agire.`,
    color: 'var(--acc3)',
  }
}

export function SignalBadge({ data }: Props) {
  const { signal, reason, color } = getSignal(data)

  return (
    <div style={{
      background: 'var(--s2)', border: `1px solid ${color}`,
      borderRadius: 12, padding: '14px 18px',
      display: 'flex', alignItems: 'center', gap: 16,
    }}>
      <div style={{
        padding: '6px 16px', borderRadius: 8,
        background: color, color: signal === 'HOLD' ? '#07080a' : '#07080a',
        fontFamily: 'Syne', fontWeight: 800, fontSize: 16,
      }}>
        {signal}
      </div>
      <p style={{ fontFamily: 'Syne', fontSize: 13, color: 'var(--muted2)', margin: 0, flex: 1 }}>
        {reason}
      </p>
    </div>
  )
}
