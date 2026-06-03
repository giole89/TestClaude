import { useQuery } from '@tanstack/react-query'
import axios from 'axios'
import { API_BASE } from '@/lib/constants'

export interface ScreenerItem {
  ticker: string
  name: string
  price: number | null
  pe: number | null
  dividendYield: number | null
  ytdChangePct: number | null
  high52w: number | null
  low52w: number | null
  marketCap: number | null
  exchange: string
  rangePosition: number | null
  currency: string
}

export interface ScreenerFilters {
  minPE?: number
  maxPE?: number
  minYield?: number
  minYtd?: number
  market?: string
  limit?: number
}

export function useScreener(filters: ScreenerFilters, enabled = true) {
  return useQuery<ScreenerItem[]>({
    queryKey: ['screener', filters],
    queryFn: () =>
      axios
        .get(`${API_BASE}/api/screener`, { params: filters })
        .then(r => r.data),
    enabled,
    staleTime: 5 * 60 * 1000, // 5min
    retry: 1,
  })
}
