import { useQuery } from '@tanstack/react-query'
import axios from 'axios'
import { API_BASE } from '@/lib/constants'

export interface BenchmarkData {
  portfolioReturn: number
  benchmarkReturn: number
  alpha: number
  period: string
  portfolioLabel: string
  benchmarkLabel: string
}

export function useBenchmark(period: string) {
  return useQuery<BenchmarkData>({
    queryKey: ['benchmark', period],
    queryFn: () =>
      axios
        .get(`${API_BASE}/api/portfolio/benchmark`, { params: { period } })
        .then(r => r.data),
    staleTime: 60 * 60 * 1000, // 1h
    retry: 1,
  })
}
