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

export function calcBullScore(params: {
  price: number
  sma20: number
  sma50: number
  sma200: number
  rsi: number
  momentum30: number
  volatility: number
  rangePosition: number
}): number {
  let score = 0
  const { price, sma20, sma50, sma200, rsi, momentum30, volatility, rangePosition } = params

  if (price > sma200) score += 25
  if (price > sma50) score += 20
  if (price > sma20) score += 15
  if (rsi > 50 && rsi < 70) score += 15
  if (momentum30 > 0) score += 10
  if (volatility < 30) score += 10
  if (rangePosition > 50) score += 5

  return Math.min(100, score)
}
