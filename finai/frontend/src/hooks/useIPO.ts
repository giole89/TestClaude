import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import axios from 'axios'
import { API_BASE } from '@/lib/constants'

export interface UpcomingIPO {
  companyName: string
  proposedTicker?: string
  exchange?: string
  priceRange?: string
  sharesOffered?: string
  expectedDate?: string
  dollarValue?: string
}

export interface RecentIPO {
  ticker: string
  companyName: string
  ipoDate: string
  ipoPrice?: number
  currentPrice?: number
  currency?: string
  exchange?: string
  performance?: number
  marketCap?: number
}

export interface WatchlistItem {
  id: string
  ticker?: string
  companyName: string
  expectedDate?: string
  exchange?: string
  sector?: string
  lockupDays: number
  ipoPrice?: number
  notes?: string
  createdAt: number
}

export function useUpcomingIPOs() {
  return useQuery<UpcomingIPO[]>({
    queryKey: ['ipo', 'upcoming'],
    queryFn: () => axios.get(`${API_BASE}/api/ipo/upcoming`).then(r => r.data),
    staleTime: 3_600_000,
    retry: 2,
  })
}

export function useRecentIPOs() {
  return useQuery<RecentIPO[]>({
    queryKey: ['ipo', 'recent'],
    queryFn: () => axios.get(`${API_BASE}/api/ipo/recent`).then(r => r.data),
    staleTime: 3_600_000,
    retry: 2,
  })
}

export function useIPOWatchlist() {
  const qc = useQueryClient()
  const KEY = ['ipo', 'watchlist']

  const query = useQuery<WatchlistItem[]>({
    queryKey: KEY,
    queryFn: () => axios.get(`${API_BASE}/api/ipo/watchlist`).then(r => r.data),
    staleTime: 60_000,
  })

  const add = useMutation({
    mutationFn: (item: Omit<WatchlistItem, 'createdAt'>) =>
      axios.post(`${API_BASE}/api/ipo/watchlist`, item).then(r => r.data),
    onSuccess: () => qc.invalidateQueries({ queryKey: KEY }),
  })

  const remove = useMutation({
    mutationFn: (id: string) => axios.delete(`${API_BASE}/api/ipo/watchlist/${id}`),
    onSuccess: () => qc.invalidateQueries({ queryKey: KEY }),
  })

  const update = useMutation({
    mutationFn: ({ id, ...fields }: Partial<WatchlistItem> & { id: string }) =>
      axios.patch(`${API_BASE}/api/ipo/watchlist/${id}`, fields),
    onSuccess: () => qc.invalidateQueries({ queryKey: KEY }),
  })

  return {
    items: query.data ?? [],
    isLoading: query.isLoading,
    add: add.mutateAsync,
    remove: remove.mutateAsync,
    update: update.mutateAsync,
  }
}
