import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import axios from 'axios'
import { API_BASE } from '@/lib/constants'

export interface WatchlistItem {
  id: string
  ticker: string
  name: string
  targetPrice: number | null
  note: string | null
  createdAt: number
}

export interface AddWatchlistRequest {
  id: string
  ticker: string
  name: string
  targetPrice?: number
  note?: string
}

const KEY = ['watchlist']

export function useWatchlist() {
  const qc = useQueryClient()

  const query = useQuery<WatchlistItem[]>({
    queryKey: KEY,
    queryFn: () => axios.get(`${API_BASE}/api/watchlist`).then(r => r.data),
    staleTime: 30_000,
    retry: 2,
  })

  const add = useMutation({
    mutationFn: (req: AddWatchlistRequest) =>
      axios.post(`${API_BASE}/api/watchlist`, req).then(r => r.data),
    onSuccess: () => qc.invalidateQueries({ queryKey: KEY }),
  })

  const remove = useMutation({
    mutationFn: (id: string) => axios.delete(`${API_BASE}/api/watchlist/${id}`),
    onSuccess: () => qc.invalidateQueries({ queryKey: KEY }),
  })

  return {
    items: query.data ?? [],
    isLoading: query.isLoading,
    add: add.mutateAsync,
    remove: remove.mutateAsync,
    isAdding: add.isPending,
  }
}
