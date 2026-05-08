import { useQuery } from '@tanstack/react-query'
import axios from 'axios'
import { API_BASE } from '@/lib/constants'

export interface QuoteData {
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

export interface FullQuoteData extends QuoteData {
  rsi: number
  sma20: number
  sma50: number
  sma200: number
  volatility: number
  momentum30: number
  bullScore: number
  history: Array<{ date: string; open: number; high: number; low: number; close: number; volume: number }>
}

export function useQuote(ticker: string | null) {
  return useQuery<QuoteData>({
    queryKey: ['quote', ticker],
    queryFn: () => axios.get(`${API_BASE}/api/quote/${ticker}`).then(r => r.data),
    enabled: !!ticker,
    staleTime: 60_000,
    retry: 2,
    refetchInterval: 60_000,
  })
}

export function useFullQuote(ticker: string | null) {
  return useQuery<FullQuoteData>({
    queryKey: ['fullQuote', ticker],
    queryFn: () => axios.get(`${API_BASE}/api/quote/${ticker}/full`).then(r => r.data),
    enabled: !!ticker,
    staleTime: 60_000,
    retry: 2,
  })
}
