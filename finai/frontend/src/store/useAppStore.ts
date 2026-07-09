import { create } from 'zustand'
import { persist } from 'zustand/middleware'

export type TabId = 'market' | 'analyze' | 'compare' | 'alerts' | 'longterm' | 'portfolio' | 'suggested' | 'ipo' | 'guide' | 'screener' | 'watchlist' | 'macro' | 'simulator' | 'finance' | 'mortgage'

interface AppState {
  activeTab: TabId
  theme: 'dark' | 'light'
  pendingOps: number
  loaderMessage: string
  setActiveTab: (tab: TabId) => void
  toggleTheme: () => void
  startLoading: (msg?: string) => void
  stopLoading: () => void
}

export const useAppStore = create<AppState>()(
  persist(
    (set, get) => ({
      activeTab: 'market',
      theme: 'dark',
      pendingOps: 0,
      loaderMessage: 'Caricamento in corso…',
      setActiveTab: (tab) => set({ activeTab: tab }),
      toggleTheme: () => {
        const next = get().theme === 'dark' ? 'light' : 'dark'
        document.documentElement.setAttribute('data-theme', next)
        set({ theme: next })
      },
      startLoading: (msg = 'Caricamento in corso…') =>
        set((s) => ({ pendingOps: s.pendingOps + 1, loaderMessage: msg })),
      stopLoading: () =>
        set((s) => ({ pendingOps: Math.max(0, s.pendingOps - 1) })),
    }),
    {
      name: 'finai-app',
      partialize: (s) => ({ theme: s.theme }),
      onRehydrateStorage: () => (state) => {
        if (state) document.documentElement.setAttribute('data-theme', state.theme)
      },
    }
  )
)
