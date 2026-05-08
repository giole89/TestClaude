import { lazy, Suspense } from 'react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { Header } from '@/components/layout/Header'
import { NavTabs } from '@/components/layout/NavTabs'
import { PandaLoader } from '@/components/layout/PandaLoader'
import { useAppStore } from '@/store/useAppStore'

const MarketPage = lazy(() => import('@/pages/MarketPage').then(m => ({ default: m.MarketPage })))
const AnalyzePage = lazy(() => import('@/pages/AnalyzePage').then(m => ({ default: m.AnalyzePage })))
const ComparePage = lazy(() => import('@/pages/ComparePage').then(m => ({ default: m.ComparePage })))
const AlertsPage = lazy(() => import('@/pages/AlertsPage').then(m => ({ default: m.AlertsPage })))
const LongTermPage = lazy(() => import('@/pages/LongTermPage').then(m => ({ default: m.LongTermPage })))
const PortfolioPage = lazy(() => import('@/pages/PortfolioPage').then(m => ({ default: m.PortfolioPage })))
const SuggestedPage = lazy(() => import('@/pages/SuggestedPage').then(m => ({ default: m.SuggestedPage })))
const GuidePage = lazy(() => import('@/pages/GuidePage').then(m => ({ default: m.GuidePage })))

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      retry: 2,
    },
  },
})

function PageLoader() {
  return (
    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '60vh', color: 'var(--muted)', fontFamily: 'Syne' }}>
      Caricamento…
    </div>
  )
}

function TabRouter() {
  const { activeTab } = useAppStore()

  return (
    <Suspense fallback={<PageLoader />}>
      {activeTab === 'market' && <MarketPage />}
      {activeTab === 'analyze' && <AnalyzePage />}
      {activeTab === 'compare' && <ComparePage />}
      {activeTab === 'alerts' && <AlertsPage />}
      {activeTab === 'longterm' && <LongTermPage />}
      {activeTab === 'portfolio' && <PortfolioPage />}
      {activeTab === 'suggested' && <SuggestedPage />}
      {activeTab === 'guide' && <GuidePage />}
    </Suspense>
  )
}

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <div style={{ minHeight: '100vh', background: 'var(--bg)' }}>
        <Header />
        <NavTabs />
        <main>
          <TabRouter />
        </main>
        <PandaLoader />
      </div>
    </QueryClientProvider>
  )
}
