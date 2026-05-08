import yahooFinance from 'yahoo-finance2'
import axios from 'axios'
import { withRetry, withCircuitBreaker } from './retry'
import { cache } from './cache'

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

export interface SearchResult {
  ticker: string
  name: string
  exchange: string
  type: string
}

export interface RecentIPO {
  ticker: string
  companyName: string
  ipoDate: string
  ipoPrice?: number
  currentPrice?: number
  currency?: string
  exchange?: string
  performance?: number
  marketCap?: number
}

export interface UpcomingIPO {
  companyName: string
  proposedTicker?: string
  exchange?: string
  priceRange?: string
  sharesOffered?: string
  expectedDate?: string
  dollarValue?: string
}

function calcRangePosition(price: number, low52w: number, high52w: number): number {
  if (high52w <= low52w) return 50
  return Math.round(((price - low52w) / (high52w - low52w)) * 100)
}

async function fetchQuoteRaw(ticker: string): Promise<QuoteResponse> {
  const result = await yahooFinance.quote(ticker, {}, { validateResult: false })
  const price = result.regularMarketPrice ?? 0
  const prevClose = result.regularMarketPreviousClose ?? price
  const dayChange = price - prevClose
  const dayChangePct = prevClose !== 0 ? (dayChange / prevClose) * 100 : 0
  const high52w = result.fiftyTwoWeekHigh ?? price
  const low52w = result.fiftyTwoWeekLow ?? price
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

export async function fetchQuote(ticker: string): Promise<QuoteResponse> {
  const result = await withCircuitBreaker(
    () => withRetry(() => fetchQuoteRaw(ticker))
  )
  if (!result) throw new Error(`No data for ${ticker}`)
  return result
}

export async function fetchBatch(tickers: string[]): Promise<QuoteResponse[]> {
  const results = await Promise.allSettled(tickers.map(t => fetchQuote(t)))
  return results
    .filter((r): r is PromiseFulfilledResult<QuoteResponse> => r.status === 'fulfilled' && r.value != null)
    .map(r => r.value)
}

export async function fetchHistory(ticker: string, range = '1y'): Promise<HistoryPoint[]> {
  const period1Map: Record<string, Date> = {
    '1m': new Date(Date.now() - 30 * 24 * 60 * 60 * 1000),
    '3m': new Date(Date.now() - 90 * 24 * 60 * 60 * 1000),
    '6m': new Date(Date.now() - 180 * 24 * 60 * 60 * 1000),
    '1y': new Date(Date.now() - 365 * 24 * 60 * 60 * 1000),
    '2y': new Date(Date.now() - 730 * 24 * 60 * 60 * 1000),
  }
  const period1 = period1Map[range] ?? period1Map['1y']
  const result = await withRetry(() =>
    yahooFinance.historical(ticker, { period1, interval: '1d' })
  )
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
  const INDICES = ['^GSPC', '^NDX', '^DJI', '^STOXX50E', 'FTSEMIB.MI', '^VIX', 'EURUSD=X', 'GC=F', 'CL=F']
  return fetchBatch(INDICES)
}

export async function searchTickers(query: string): Promise<SearchResult[]> {
  try {
    const result = await withRetry(() =>
      yahooFinance.search(query, { quotesCount: 12, newsCount: 0 }, { validateResult: false })
    )
    return ((result.quotes ?? []) as Record<string, unknown>[])
      .filter(q => q.quoteType === 'EQUITY' || q.quoteType === 'ETF' || q.quoteType === 'MUTUALFUND')
      .map(q => ({
        ticker: q.symbol as string,
        name: ((q.shortname ?? q.longname ?? q.symbol) as string),
        exchange: (q.exchange ?? '') as string,
        type: (q.quoteType ?? 'EQUITY') as string,
      }))
  } catch {
    return []
  }
}

const NASDAQ_HEADERS = {
  'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
  'Accept': 'application/json, text/plain, */*',
  'Accept-Language': 'en-US,en;q=0.9',
  'Referer': 'https://www.nasdaq.com/market-activity/ipos',
  'Origin': 'https://www.nasdaq.com',
}

async function fetchNasdaqIPOMonth(yearMonth: string): Promise<{ upcoming: UpcomingIPO[]; recent: UpcomingIPO[] }> {
  try {
    const { data } = await axios.get(
      `https://api.nasdaq.com/api/ipo/calendar?date=${yearMonth}`,
      { headers: NASDAQ_HEADERS, timeout: 10_000 }
    )
    const d = data?.data ?? {}
    const parseRows = (rows: Record<string, string>[] = []): UpcomingIPO[] =>
      rows.map(r => ({
        companyName: r.companyName ?? r.issuerName ?? '',
        proposedTicker: r.proposedTickerSymbol ?? undefined,
        exchange: r.proposedExchange ?? r.exchange ?? undefined,
        priceRange: r.proposedSharePrice ?? r.priceRange ?? undefined,
        sharesOffered: r.sharesOffered ?? undefined,
        expectedDate: r.expectedPriceDate ?? r.pricedDate ?? r.filedDate ?? undefined,
        dollarValue: r.dollarValueOfSharesOffered ?? undefined,
      }))
    return {
      upcoming: parseRows(d.upcomingTable?.rows),
      recent: parseRows(d.pricedTable?.rows),
    }
  } catch {
    return { upcoming: [], recent: [] }
  }
}

export async function fetchUpcomingIPOs(): Promise<UpcomingIPO[]> {
  const now = new Date()
  const thisMonth = now.toISOString().slice(0, 7)
  const nextDate = new Date(now.getFullYear(), now.getMonth() + 1, 1)
  const nextMonth = nextDate.toISOString().slice(0, 7)
  const [curr, next] = await Promise.all([
    fetchNasdaqIPOMonth(thisMonth),
    fetchNasdaqIPOMonth(nextMonth),
  ])
  return [...next.upcoming, ...curr.upcoming].filter(i => i.companyName).slice(0, 30)
}

export async function fetchRecentIPOs(): Promise<RecentIPO[]> {
  const months: string[] = []
  const now = new Date()
  for (let i = 0; i < 6; i++) {
    const d = new Date(now.getFullYear(), now.getMonth() - i, 1)
    months.push(d.toISOString().slice(0, 7))
  }
  const results = await Promise.allSettled(months.map(m => fetchNasdaqIPOMonth(m)))
  const allRecent: UpcomingIPO[] = results
    .filter((r): r is PromiseFulfilledResult<{ upcoming: UpcomingIPO[]; recent: UpcomingIPO[] }> => r.status === 'fulfilled')
    .flatMap(r => r.value.recent)
    .filter(i => i.companyName)
    .slice(0, 40)

  const tickers = allRecent.map(i => i.proposedTicker).filter((t): t is string => Boolean(t))
  const quoteMap = new Map<string, QuoteResponse>()
  if (tickers.length > 0) {
    const quotes = await fetchBatch(tickers.slice(0, 25))
    quotes.forEach(q => quoteMap.set(q.ticker, q))
  }

  return allRecent.map(i => {
    const ticker = i.proposedTicker ?? ''
    const quote = quoteMap.get(ticker)
    const ipoPrice = parseFloat((i.priceRange ?? '').replace(/[^0-9.]/g, '')) || undefined
    const currentPrice = quote?.price
    const performance = ipoPrice && currentPrice ? ((currentPrice - ipoPrice) / ipoPrice) * 100 : undefined
    return {
      ticker,
      companyName: i.companyName,
      ipoDate: i.expectedDate ?? '',
      ipoPrice,
      currentPrice,
      currency: quote?.currency,
      exchange: i.exchange ?? quote?.exchange,
      performance,
      marketCap: quote?.marketCap,
    }
  })
}
