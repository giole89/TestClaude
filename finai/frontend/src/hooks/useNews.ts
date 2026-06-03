import { useQuery } from '@tanstack/react-query'
import axios from 'axios'
import { API_BASE } from '@/lib/constants'

export interface NewsItem {
  title: string
  url: string
  publisher: string | null
  publishedAt: number | null
  summary: string | null
  ticker: string
}

export function useNews(tickers: string[], count = 10) {
  const enabled = tickers.length > 0

  return useQuery<NewsItem[]>({
    queryKey: ['news', tickers.join(','), count],
    queryFn: () =>
      axios
        .get(`${API_BASE}/api/news`, { params: { tickers: tickers.join(','), count } })
        .then(r => r.data),
    enabled,
    staleTime: 15 * 60 * 1000, // 15min
    retry: 1,
  })
}
