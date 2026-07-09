import { useAppStore, TabId } from '@/store/useAppStore'

const TABS: Array<{ id: TabId; label: string; icon: string }> = [
  { id: 'market', label: 'Mercato', icon: '📈' },
  { id: 'analyze', label: 'Analisi', icon: '🔭' },
  { id: 'compare', label: 'Confronto', icon: '⚖️' },
  { id: 'alerts', label: 'Alert', icon: '🔔' },
  { id: 'longterm', label: 'Lungo Termine', icon: '🌱' },
  { id: 'portfolio', label: 'Portafoglio', icon: '💼' },
  { id: 'simulator', label: 'Simulazione', icon: '🧪' },
  { id: 'finance', label: 'Finanza Personale', icon: '💰' },
  { id: 'mortgage', label: 'Mutuo e Finanziamenti', icon: '🏠' },
  { id: 'suggested', label: 'Suggeriti', icon: '🎯' },
  { id: 'ipo', label: 'IPO', icon: '🏛️' },
  { id: 'screener', label: 'Screener', icon: '🔍' },
  { id: 'watchlist', label: 'Watchlist', icon: '⭐' },
  { id: 'macro', label: 'Macro', icon: '🌍' },
  { id: 'guide', label: 'Guida', icon: '📚' },
]

export function NavTabs() {
  const { activeTab, setActiveTab } = useAppStore()

  return (
    <nav style={{
      display: 'flex', gap: 2, padding: '8px 16px',
      background: 'var(--s1)', borderBottom: '1px solid var(--border)',
      overflowX: 'auto',
    }}>
      {TABS.map(tab => (
        <button
          key={tab.id}
          onClick={() => setActiveTab(tab.id)}
          style={{
            display: 'flex', alignItems: 'center', gap: 6,
            padding: '7px 14px', borderRadius: 8, border: 'none',
            cursor: 'pointer', whiteSpace: 'nowrap',
            fontFamily: 'Syne', fontWeight: 600, fontSize: 13,
            background: activeTab === tab.id ? 'var(--acc)' : 'transparent',
            color: activeTab === tab.id ? '#07080a' : 'var(--muted2)',
            transition: 'all 0.15s ease',
          }}
        >
          <span aria-hidden="true">{tab.icon}</span>
          <span>{tab.label}</span>
        </button>
      ))}
    </nav>
  )
}
