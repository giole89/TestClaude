import { useQuery } from '@tanstack/react-query'
import axios from 'axios'
import { API_BASE } from '@/lib/constants'

export interface DividendItem {
  ticker: string
  companyName: string
  annualDividend: number
  dividendYield: number
  exDividendDate: string | null
  dividendDate: string | null
  payFrequency: number | null
}

export function useDividends(tickers: string[]) {
  const enabled = tickers.length > 0

  return useQuery<DividendItem[]>({
    queryKey: ['dividends', tickers.join(',')],
    queryFn: () =>
      axios
        .get(`${API_BASE}/api/dividends`, { params: { tickers: tickers.join(',') } })
        .then(r => r.data),
    enabled,
    staleTime: 24 * 60 * 60 * 1000, // 24h
    retry: 1,
  })
}
