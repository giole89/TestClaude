import { useQuery } from '@tanstack/react-query'
import axios from 'axios'
import { API_BASE, STOCK_UNIVERSE, ETF_UNIVERSE, INDICES } from '@/lib/constants'
import { QuoteData } from './useQuote'

async function fetchBatch(tickers: string[]): Promise<QuoteData[]> {
  const res = await axios.get(`${API_BASE}/api/batch`, {
    params: { tickers: tickers.join(',') },
  })
  return res.data
}

export function useIndices() {
  return useQuery<QuoteData[]>({
    queryKey: ['indices'],
    queryFn: () => axios.get(`${API_BASE}/api/indices`).then(r => r.data),
    staleTime: 60_000,
    refetchInterval: 60_000,
    retry: 2,
  })
}

export function useStockBatch() {
  return useQuery<QuoteData[]>({
    queryKey: ['stockBatch'],
    queryFn: () => fetchBatch(STOCK_UNIVERSE),
    staleTime: 120_000,
    refetchInterval: 300_000,
    retry: 2,
  })
}

export function useEtfBatch() {
  return useQuery<QuoteData[]>({
    queryKey: ['etfBatch'],
    queryFn: () => fetchBatch(ETF_UNIVERSE),
    staleTime: 120_000,
    refetchInterval: 300_000,
    retry: 2,
  })
}

export function useMarketData() {
  const stocks = useStockBatch()
  const etfs = useEtfBatch()

  const sorted = (data: QuoteData[], key: 'dayChangePct' | 'ytdChangePct') =>
    [...(data ?? [])].sort((a, b) => b[key] - a[key])

  const stocksByDay = sorted(stocks.data ?? [], 'dayChangePct')
  const etfsByDay = sorted(etfs.data ?? [], 'dayChangePct')

  const topStocks = stocksByDay.slice(0, 10)
  const worstStocks = [...stocksByDay].reverse().slice(0, 10)
  const topEtf = etfsByDay.slice(0, 10)
  const worstEtf = [...etfsByDay].reverse().slice(0, 10)

  const allQuotes = [...(stocks.data ?? []), ...(etfs.data ?? [])]
  const posCount = allQuotes.filter(q => q.dayChangePct > 0).length
  const sentimentPct = allQuotes.length > 0 ? (posCount / allQuotes.length) * 100 : 0

  return {
    isLoading: stocks.isLoading || etfs.isLoading,
    topStocks,
    worstStocks,
    topEtf,
    worstEtf,
    allQuotes,
    sentimentPct,
    posCount,
    totalCount: allQuotes.length,
  }
}
