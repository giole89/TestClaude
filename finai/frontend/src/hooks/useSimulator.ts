import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import axios from 'axios'
import { API_BASE } from '@/lib/constants'

export interface SimPosition {
  id: string
  ticker: string
  name: string
  qty: number
  avgPrice: number
  currentPrice: number | null
  currency: string
  value: number
  pnl: number
  pnlPct: number
  createdAt: string
}

export interface SimSummary {
  cashBalance: number
  startingBalance: number
  positionsValue: number
  totalValue: number
  totalPnl: number
  totalPnlPct: number
  positions: SimPosition[]
}

export interface SimTrade {
  id: string
  ticker: string
  name: string
  side: 'BUY' | 'SELL'
  qty: number
  price: number
  amount: number
  realizedPnl: number | null
  currency: string
  executedAt: string
}

const SUMMARY_KEY = ['simSummary']
const TRADES_KEY = ['simTrades']

export function useSimulator() {
  const qc = useQueryClient()

  const summary = useQuery<SimSummary>({
    queryKey: SUMMARY_KEY,
    queryFn: () => axios.get(`${API_BASE}/api/sim/summary`).then(r => r.data),
    staleTime: 30_000,
    retry: 2,
  })

  const trades = useQuery<SimTrade[]>({
    queryKey: TRADES_KEY,
    queryFn: () => axios.get(`${API_BASE}/api/sim/trades`).then(r => r.data),
    staleTime: 30_000,
    retry: 2,
  })

  const invalidateAll = () => {
    qc.invalidateQueries({ queryKey: SUMMARY_KEY })
    qc.invalidateQueries({ queryKey: TRADES_KEY })
  }

  const buy = useMutation({
    mutationFn: (req: { ticker: string; qty: number }) =>
      axios.post(`${API_BASE}/api/sim/buy`, req).then(r => r.data as SimSummary),
    onSuccess: (data) => { qc.setQueryData(SUMMARY_KEY, data); invalidateAll() },
  })

  const sell = useMutation({
    mutationFn: (req: { ticker: string; qty: number }) =>
      axios.post(`${API_BASE}/api/sim/sell`, req).then(r => r.data as SimSummary),
    onSuccess: (data) => { qc.setQueryData(SUMMARY_KEY, data); invalidateAll() },
  })

  const reset = useMutation({
    mutationFn: (startingBalance?: number) =>
      axios.post(`${API_BASE}/api/sim/reset`, startingBalance ? { startingBalance } : {}).then(r => r.data as SimSummary),
    onSuccess: (data) => { qc.setQueryData(SUMMARY_KEY, data); invalidateAll() },
  })

  return {
    summary: summary.data,
    isLoading: summary.isLoading,
    trades: trades.data ?? [],
    isLoadingTrades: trades.isLoading,
    buy: buy.mutateAsync,
    isBuying: buy.isPending,
    sell: sell.mutateAsync,
    isSelling: sell.isPending,
    reset: reset.mutateAsync,
    isResetting: reset.isPending,
  }
}
