import { useState } from 'react'
import { PORTFOLIO_TEMPLATES, ProfileKey } from '@/lib/constants'
import { ChatPanel } from '@/components/chat/ChatPanel'
import { chatKey } from '@/store/useChatStore'
import { useChatStore } from '@/store/useChatStore'
import { useAppStore } from '@/store/useAppStore'

const PROFILES: ProfileKey[] = ['conservative', 'balanced', 'growth', 'aggressive']

export function SuggestedPage() {
  const [profile, setProfile] = useState<ProfileKey>('balanced')
  const { resetHistory } = useChatStore()
  const { setActiveTab } = useAppStore()

  const tmpl = PORTFOLIO_TEMPLATES[profile]
  const tabKey = chatKey('suggested', profile)

  const handleProfileChange = (p: ProfileKey) => {
    if (p !== profile) {
      resetHistory(chatKey('suggested', p))
    }
    setProfile(p)
  }

  const context = {
    tab: 'suggested',
    profile,
    portfolioTemplate: tmpl,
  }

  return (
    <div style={{ display: 'flex', gap: 16, padding: 24, height: 'calc(100vh - 112px)' }}>
      <div style={{ flex: 2, display: 'flex', flexDirection: 'column', gap: 16, overflowY: 'auto' }}>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 10 }}>
          {PROFILES.map(p => {
            const t = PORTFOLIO_TEMPLATES[p]
            return (
              <button
                key={p}
                onClick={() => handleProfileChange(p)}
                style={{
                  background: profile === p ? 'var(--acc)' : 'var(--s2)',
                  border: `1px solid ${profile === p ? 'var(--acc)' : 'var(--border)'}`,
                  borderRadius: 12, padding: '14px', cursor: 'pointer', textAlign: 'left',
                  color: profile === p ? '#07080a' : 'var(--text)',
                  transition: 'all 0.2s',
                }}
              >
                <div style={{ fontFamily: 'Syne', fontWeight: 800, fontSize: 14, marginBottom: 4 }}>
                  {t.label}
                </div>
                <div style={{ fontFamily: 'Syne', fontSize: 11, opacity: 0.8, marginBottom: 8 }}>
                  {t.desc}
                </div>
                <div style={{ fontFamily: 'JetBrains Mono', fontSize: 12, fontWeight: 700 }}>
                  {t.expected}
                </div>
              </button>
            )
          })}
        </div>

        <div style={{
          background: 'var(--s2)', border: '1px solid var(--border)',
          borderRadius: 12, padding: '20px',
        }}>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 12, marginBottom: 20 }}>
            {[
              { label: 'Rendimento atteso', value: tmpl.expected },
              { label: 'Max Drawdown', value: tmpl.maxDD },
              { label: 'Volatilità', value: tmpl.volatility },
              { label: 'Orizzonte temporale', value: tmpl.horizon },
            ].map(m => (
              <div key={m.label} style={{ background: 'var(--s3)', borderRadius: 8, padding: '10px 12px' }}>
                <div style={{ fontFamily: 'Syne', fontSize: 11, color: 'var(--muted)', marginBottom: 4 }}>{m.label}</div>
                <div style={{ fontFamily: 'JetBrains Mono', fontSize: 14, fontWeight: 700, color: 'var(--text)' }}>{m.value}</div>
              </div>
            ))}
          </div>

          <div style={{ fontFamily: 'Syne', fontWeight: 700, fontSize: 14, color: 'var(--text)', marginBottom: 12 }}>
            Allocazione
          </div>
          {tmpl.allocation.map(a => (
            <div key={a.ticker} style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 10 }}>
              <button
                onClick={() => { sessionStorage.setItem('finai_analyze_ticker', a.ticker); setActiveTab('analyze') }}
                style={{
                  background: 'none', border: 'none', cursor: 'pointer',
                  fontFamily: 'JetBrains Mono', fontWeight: 700, fontSize: 13,
                  color: 'var(--acc)', padding: 0, minWidth: 80, textAlign: 'left',
                }}
              >
                {a.ticker}
              </button>
              <div style={{ flex: 1 }}>
                <div style={{ fontFamily: 'Syne', fontSize: 13, color: 'var(--text)', marginBottom: 2 }}>
                  {a.name}
                </div>
                <div style={{ fontFamily: 'Syne', fontSize: 11, color: 'var(--muted)' }}>
                  {a.why}
                </div>
              </div>
              <div style={{ textAlign: 'right', minWidth: 50 }}>
                <div style={{ fontFamily: 'JetBrains Mono', fontWeight: 700, fontSize: 14, color: 'var(--acc)', marginBottom: 4 }}>
                  {a.pct}%
                </div>
                <div style={{ height: 4, width: a.pct * 1.5, background: 'var(--acc)', borderRadius: 2, marginLeft: 'auto' }} />
              </div>
            </div>
          ))}
        </div>
      </div>

      <div style={{ flex: 1, minWidth: 320 }}>
        <ChatPanel
          tabKey={tabKey}
          context={context}
          quickActions={[
            'Costruisci un portafoglio personalizzato per me',
            'Perché hai scelto questi ETF?',
            'Come implemento questo portafoglio?',
            'Quali sono i rischi di questo profilo?',
          ]}
          placeholder="Chiedimi del portafoglio suggerito…"
        />
      </div>
    </div>
  )
}
