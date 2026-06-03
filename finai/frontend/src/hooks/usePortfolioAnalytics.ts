import { useQuery } from '@tanstack/react-query'
import axios from 'axios'
import { API_BASE } from '@/lib/constants'

export interface PositionStats {
  ticker: string
  name: string
  qty: number
  loadPrice: number
  currentPrice: number
  marketValue: number
  cost: number
  gainAmount: number
  gainPct: number
  weight: number
  contributionPct: number
  currency: string
}

export interface PortfolioAnalytics {
  totalValue: number
  totalCost: number
  totalGainAmount: number
  totalGainPct: number
  positionCount: number
  bestTicker: string | null
  bestGainPct: number | null
  worstTicker: string | null
  worstGainPct: number | null
  topWeightTicker: string | null
  topWeightPct: number | null
  concentrationHhi: number
  currencyCount: number
  positions: PositionStats[]
}

export function usePortfolioAnalytics(enabled = true) {
  return useQuery<PortfolioAnalytics>({
    queryKey: ['portfolio-analytics'],
    queryFn: () =>
      axios.get(`${API_BASE}/api/portfolio/analytics`).then(r => r.data),
    enabled,
    staleTime: 60_000,
    retry: 2,
  })
}
