export function calcRSI(closes: number[], period = 14): number {
  if (closes.length < period + 1) return 50
  let gains = 0, losses = 0
  for (let i = closes.length - period; i < closes.length; i++) {
    const diff = closes[i] - closes[i - 1]
    if (diff >= 0) gains += diff
    else losses -= diff
  }
  const avgGain = gains / period
  const avgLoss = losses / period
  if (avgLoss === 0) return 100
  const rs = avgGain / avgLoss
  return Math.round(100 - 100 / (1 + rs))
}

export function calcSMA(closes: number[], period: number): number {
  if (closes.length < period) return closes[closes.length - 1] ?? 0
  const slice = closes.slice(-period)
  return slice.reduce((a, b) => a + b, 0) / period
}

export function calcEMA(closes: number[], period: number): number[] {
  const k = 2 / (period + 1)
  const ema: number[] = [closes[0]]
  for (let i = 1; i < closes.length; i++) {
    ema.push(closes[i] * k + ema[i - 1] * (1 - k))
  }
  return ema
}

export function calcMACD(closes: number[]): { macd: number; signal: number; histogram: number } {
  const ema12 = calcEMA(closes, 12)
  const ema26 = calcEMA(closes, 26)
  const macdLine = ema12.map((v, i) => v - ema26[i])
  const signalLine = calcEMA(macdLine, 9)
  const last = macdLine.length - 1
  return {
    macd: macdLine[last],
    signal: signalLine[last],
    histogram: macdLine[last] - signalLine[last],
  }
}

export function calcBollinger(closes: number[], period = 20): { upper: number; middle: number; lower: number } {
  const middle = calcSMA(closes, period)
  const slice = closes.slice(-period)
  const variance = slice.reduce((acc, v) => acc + (v - middle) ** 2, 0) / period
  const std = Math.sqrt(variance)
  return { upper: middle + 2 * std, middle, lower: middle - 2 * std }
}

export function calcVolatility(closes: number[]): number {
  if (closes.length < 2) return 0
  const returns: number[] = []
  for (let i = 1; i < closes.length; i++) {
    returns.push(Math.log(closes[i] / closes[i - 1]))
  }
  const mean = returns.reduce((a, b) => a + b, 0) / returns.length
  const variance = returns.reduce((a, b) => a + (b - mean) ** 2, 0) / returns.length
  return Math.round(Math.sqrt(variance) * Math.sqrt(252) * 100)
}

export function calcMomentum(closes: number[], days = 30): number {
  if (closes.length < days + 1) return 0
  const old = closes[closes.length - 1 - days]
  const current = closes[closes.length - 1]
  return Math.round(((current - old) / old) * 100 * 10) / 10
}

export interface TickerData {
  price: number
  sma20: number
  sma50: number
  sma200: number
  rsi: number
  momentum30: number
  volatility: number
  rangePosition: number
}

export function calcBullScore(data: TickerData): number {
  let score = 0
  const { price, sma20, sma50, sma200, rsi, momentum30, volatility, rangePosition } = data

  if (price > sma200) score += 25
  if (price > sma50) score += 20
  if (price > sma20) score += 15
  if (rsi > 50 && rsi < 70) score += 15
  if (momentum30 > 0) score += 10
  if (volatility < 30) score += 10
  if (rangePosition > 50) score += 5

  return Math.min(100, score)
}

export function predictScenarios(
  price: number,
  volatility: number,
  momentum: number
): { bull: number; base: number; bear: number } {
  const vol30 = (volatility / 100) * Math.sqrt(30 / 252)
  const trend = momentum / 100 / 12

  return {
    bull: Math.round(price * (1 + trend + vol30) * 100) / 100,
    base: Math.round(price * (1 + trend * 0.5) * 100) / 100,
    bear: Math.round(price * (1 - vol30 * 0.8) * 100) / 100,
  }
}

export function calcLongTermScore(params: {
  price: number
  sma200: number
  volatility: number
  rangePosition: number
  momentum30: number
  rsi: number
  low52w: number
  bullScore: number
}): { score: number; criteria: Array<{ label: string; passed: boolean; value: string; points: number }> } {
  const { price, sma200, volatility, rangePosition, momentum30, rsi, low52w, bullScore } = params
  const criteria = [
    {
      label: 'Prezzo sopra SMA200',
      passed: price > sma200,
      value: `${price > sma200 ? '✅' : '❌'} Prezzo ${price > sma200 ? '>' : '<'} SMA200 (${sma200.toFixed(2)})`,
      points: price > sma200 ? 20 : 0,
      max: 20,
    },
    {
      label: 'Volatilità accettabile',
      passed: volatility < 40,
      value: `${volatility < 40 ? '✅' : '❌'} Vol. ${volatility}% annua (soglia 40%)`,
      points: volatility < 20 ? 15 : volatility < 30 ? 10 : volatility < 40 ? 5 : 0,
      max: 15,
    },
    {
      label: 'Distanza sana dai massimi',
      passed: rangePosition < 90,
      value: `${rangePosition < 90 ? '✅' : '⚠️'} Posizione nel range 52W: ${rangePosition}%`,
      points: rangePosition < 70 ? 15 : rangePosition < 90 ? 8 : 0,
      max: 15,
    },
    {
      label: 'Momentum positivo',
      passed: momentum30 > 0,
      value: `${momentum30 > 0 ? '✅' : '❌'} Momentum 30gg: ${momentum30 > 0 ? '+' : ''}${momentum30}%`,
      points: momentum30 > 0 ? 10 : 0,
      max: 10,
    },
    {
      label: 'RSI non ipercomprato',
      passed: rsi < 70,
      value: `${rsi < 70 ? '✅' : '⚠️'} RSI: ${rsi} (soglia ipercomprato: 70)`,
      points: rsi < 70 ? 10 : rsi < 80 ? 5 : 0,
      max: 10,
    },
    {
      label: 'Recupero dai minimi',
      passed: price > low52w * 1.10,
      value: `${price > low52w * 1.10 ? '✅' : '❌'} Recupero >10% dai minimi 52W`,
      points: price > low52w * 1.10 ? 10 : 0,
      max: 10,
    },
    {
      label: 'Bull Score complessivo',
      passed: bullScore >= 50,
      value: `${bullScore >= 50 ? '✅' : '❌'} Bull Score: ${bullScore}/100`,
      points: Math.round((bullScore / 100) * 20),
      max: 20,
    },
  ]

  const score = criteria.reduce((acc, c) => acc + c.points, 0)
  return { score, criteria }
}
