import { create } from 'zustand'
import { persist } from 'zustand/middleware'

export interface PortfolioItem {
  id: string
  ticker: string
  name: string
  qty: number
  loadPrice: number
  currentPrice?: number
  currency?: string
}

interface PortfolioState {
  items: PortfolioItem[]
  addItem: (item: Omit<PortfolioItem, 'id'>) => void
  removeItem: (id: string) => void
  updatePrice: (ticker: string, price: number, currency?: string) => void
  updateName: (ticker: string, name: string) => void
}

export const usePortfolioStore = create<PortfolioState>()(
  persist(
    (set) => ({
      items: [],
      addItem: (item) =>
        set((s) => ({
          items: [...s.items, { ...item, id: `${item.ticker}-${Date.now()}` }],
        })),
      removeItem: (id) =>
        set((s) => ({ items: s.items.filter((i) => i.id !== id) })),
      updatePrice: (ticker, price, currency) =>
        set((s) => ({
          items: s.items.map((i) =>
            i.ticker === ticker ? { ...i, currentPrice: price, currency: currency ?? i.currency } : i
          ),
        })),
      updateName: (ticker, name) =>
        set((s) => ({
          items: s.items.map((i) => (i.ticker === ticker ? { ...i, name } : i)),
        })),
    }),
    { name: 'finai-portfolio' }
  )
)
