import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import axios from 'axios'
import { API_BASE } from '@/lib/constants'

export interface PortfolioItem {
  id: string
  ticker: string
  name: string
  qty: number
  loadPrice: number
  currentPrice?: number
  currency: string
  createdAt: number
}

const KEY = ['portfolio']

export function usePortfolio() {
  const qc = useQueryClient()

  const query = useQuery<PortfolioItem[]>({
    queryKey: KEY,
    queryFn: () => axios.get(`${API_BASE}/api/portfolio`).then(r => r.data),
    staleTime: 30_000,
    retry: 2,
  })

  const add = useMutation({
    mutationFn: (item: Omit<PortfolioItem, 'createdAt'>) =>
      axios.post(`${API_BASE}/api/portfolio`, item).then(r => r.data),
    onSuccess: () => qc.invalidateQueries({ queryKey: KEY }),
  })

  const remove = useMutation({
    mutationFn: (id: string) => axios.delete(`${API_BASE}/api/portfolio/${id}`),
    onSuccess: () => qc.invalidateQueries({ queryKey: KEY }),
  })

  const refresh = useMutation({
    mutationFn: () => axios.post(`${API_BASE}/api/portfolio/refresh`).then(r => r.data as PortfolioItem[]),
    onSuccess: (data) => qc.setQueryData(KEY, data),
  })

  return {
    items: query.data ?? [],
    isLoading: query.isLoading,
    isRefreshing: refresh.isPending,
    add: add.mutateAsync,
    remove: remove.mutateAsync,
    refresh: refresh.mutateAsync,
  }
}
