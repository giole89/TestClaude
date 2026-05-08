import yahooFinance from 'yahoo-finance2'

export interface QuoteResponse {
  ticker: string
  name: string
  price: number
  dayChange: number
  dayChangePct: number
  ytdChangePct: number
  high52w: number
  low52w: number
  volume: number
  marketCap?: number
  pe?: number
  currency: string
  exchange: string
  rangePosition: number
  timestamp: number
}

export interface HistoryPoint {
  date: string
  open: number
  high: number
  low: number
  close: number
  volume: number
}

function calcRangePosition(price: number, low52w: number, high52w: number): number {
  if (high52w <= low52w) return 50
  return Math.round(((price - low52w) / (high52w - low52w)) * 100)
}

export async function fetchQuote(ticker: string): Promise<QuoteResponse> {
  const result = await yahooFinance.quote(ticker, {}, { validateResult: false })

  const price = result.regularMarketPrice ?? 0
  const prevClose = result.regularMarketPreviousClose ?? price
  const dayChange = price - prevClose
  const dayChangePct = prevClose !== 0 ? (dayChange / prevClose) * 100 : 0

  const high52w = result.fiftyTwoWeekHigh ?? price
  const low52w = result.fiftyTwoWeekLow ?? price

  const ytdChangePct = result.fiftyTwoWeekChangePercent != null
    ? 0
    : 0

  return {
    ticker: result.symbol ?? ticker,
    name: result.longName ?? result.shortName ?? ticker,
    price,
    dayChange,
    dayChangePct,
    ytdChangePct: (result as Record<string, unknown>).ytdReturn != null
      ? ((result as Record<string, unknown>).ytdReturn as number) * 100
      : 0,
    high52w,
    low52w,
    volume: result.regularMarketVolume ?? 0,
    marketCap: result.marketCap,
    pe: result.trailingPE,
    currency: result.currency ?? 'USD',
    exchange: result.fullExchangeName ?? result.exchange ?? '',
    rangePosition: calcRangePosition(price, low52w, high52w),
    timestamp: Date.now(),
  }
}

export async function fetchBatch(tickers: string[]): Promise<QuoteResponse[]> {
  const results = await Promise.allSettled(tickers.map((t: string) => fetchQuote(t)))
  return results
    .filter((r): r is PromiseFulfilledResult<QuoteResponse> => r.status === 'fulfilled')
    .map((r: PromiseFulfilledResult<QuoteResponse>) => r.value)
}

export async function fetchHistory(ticker: string, range = '1y'): Promise<HistoryPoint[]> {
  const period1Map: Record<string, Date> = {
    '1m': new Date(Date.now() - 30 * 24 * 60 * 60 * 1000),
    '3m': new Date(Date.now() - 90 * 24 * 60 * 60 * 1000),
    '6m': new Date(Date.now() - 180 * 24 * 60 * 60 * 1000),
    '1y': new Date(Date.now() - 365 * 24 * 60 * 60 * 1000),
  }

  const period1 = period1Map[range] ?? period1Map['1y']

  const result = await yahooFinance.historical(ticker, {
    period1,
    interval: '1d',
  })

  return result
    .filter(r => r.close != null)
    .map(r => ({
      date: r.date.toISOString().split('T')[0],
      open: r.open ?? r.close,
      high: r.high ?? r.close,
      low: r.low ?? r.close,
      close: r.close,
      volume: r.volume ?? 0,
    }))
}

export async function fetchIndices(): Promise<QuoteResponse[]> {
  const INDICES = [
    '^GSPC', '^NDX', '^DJI', '^STOXX50E', 'FTSEMIB.MI',
    '^VIX', 'EURUSD=X', 'GC=F', 'CL=F'
  ]
  return fetchBatch(INDICES)
}
