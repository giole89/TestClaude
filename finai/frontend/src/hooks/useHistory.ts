import { useQuery } from '@tanstack/react-query'
import axios from 'axios'
import { API_BASE } from '@/lib/constants'

export interface HistoryPoint {
  date: string
  open: number
  high: number
  low: number
  close: number
  volume: number
}

export function useHistory(ticker: string | null, range = '1y') {
  return useQuery<HistoryPoint[]>({
    queryKey: ['history', ticker, range],
    queryFn: () =>
      axios.get(`${API_BASE}/api/history/${ticker}`, { params: { range } }).then(r => r.data),
    enabled: !!ticker,
    staleTime: 14_400_000,
    retry: 2,
  })
}
