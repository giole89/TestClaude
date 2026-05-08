import { useAppStore } from '@/store/useAppStore'

export function Header() {
  const { theme, toggleTheme } = useAppStore()

  return (
    <header style={{
      height: 56, display: 'flex', alignItems: 'center',
      padding: '0 24px', background: 'var(--s1)',
      borderBottom: '1px solid var(--border)',
      position: 'sticky', top: 0, zIndex: 100,
    }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 10, flex: 1 }}>
        <svg viewBox="0 0 100 100" width={32} height={32}>
          <ellipse cx="22" cy="22" rx="14" ry="14" fill="#1a1a1a"/>
          <ellipse cx="78" cy="22" rx="14" ry="14" fill="#1a1a1a"/>
          <ellipse cx="50" cy="54" rx="38" ry="36" fill="#f0f0f0"/>
          <ellipse cx="35" cy="46" rx="12" ry="11" fill="#1a1a1a"/>
          <ellipse cx="65" cy="46" rx="12" ry="11" fill="#1a1a1a"/>
          <text x="35" y="51" textAnchor="middle" fontSize="13" fontWeight="900" fill="#e8f542">€</text>
          <text x="65" y="51" textAnchor="middle" fontSize="13" fontWeight="900" fill="#e8f542">€</text>
          <ellipse cx="50" cy="65" rx="9" ry="6" fill="#1a1a1a"/>
          <path d="M42 73 Q50 80 58 73" stroke="#1a1a1a" strokeWidth="2.5" fill="none" strokeLinecap="round"/>
          <ellipse cx="28" cy="68" rx="7" ry="5" fill="#f9a8d4" opacity={0.6}/>
          <ellipse cx="72" cy="68" rx="7" ry="5" fill="#f9a8d4" opacity={0.6}/>
        </svg>
        <span style={{
          fontFamily: 'Syne', fontWeight: 800, fontSize: 20,
          color: 'var(--acc)', letterSpacing: '0.02em',
        }}>
          FINAI
        </span>
        <span style={{ color: 'var(--muted)', fontSize: 12, marginLeft: 4 }}>
          Advisor Finanziario Personale
        </span>
      </div>

      <button
        onClick={toggleTheme}
        style={{
          background: 'var(--s2)', border: '1px solid var(--border)',
          borderRadius: 8, padding: '6px 12px', cursor: 'pointer',
          color: 'var(--text)', fontSize: 16,
        }}
        title="Cambia tema"
      >
        {theme === 'dark' ? '☀️' : '🌙'}
      </button>
    </header>
  )
}
