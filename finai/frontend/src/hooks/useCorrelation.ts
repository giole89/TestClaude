import { useQuery } from '@tanstack/react-query'
import axios from 'axios'
import { API_BASE } from '@/lib/constants'

export interface CorrelationData {
  labels: string[]
  matrix: number[][]
  interpretations: string[]
}

export function useCorrelation() {
  return useQuery<CorrelationData>({
    queryKey: ['correlation'],
    queryFn: () =>
      axios
        .get(`${API_BASE}/api/portfolio/correlation`)
        .then(r => r.data),
    staleTime: 4 * 60 * 60 * 1000, // 4h
    retry: 1,
  })
}
