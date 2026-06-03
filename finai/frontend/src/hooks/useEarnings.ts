import { useQuery } from '@tanstack/react-query'
import axios from 'axios'
import { API_BASE } from '@/lib/constants'

export interface EarningsItem {
  ticker: string
  companyName: string
  earningsStart: number | null
  earningsEnd: number | null
  earningsDate: string | null
  epsForward: number | null
  epsTrailing: number | null
  forwardPE: number | null
  quarter: string | null
}

export function useEarnings(tickers: string[]) {
  const enabled = tickers.length > 0

  return useQuery<EarningsItem[]>({
    queryKey: ['earnings', tickers.join(',')],
    queryFn: () =>
      axios
        .get(`${API_BASE}/api/earnings`, { params: { tickers: tickers.join(',') } })
        .then(r => r.data),
    enabled,
    staleTime: 6 * 60 * 60 * 1000, // 6h — le date earnings cambiano raramente
    retry: 1,
  })
}
