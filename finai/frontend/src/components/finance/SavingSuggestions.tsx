import { useFinance, SavingSuggestion } from '@/hooks/useFinance'
import { formatNumber } from '@/lib/formatters'

const SEVERITY_COLOR: Record<SavingSuggestion['severity'], string> = {
  ALTA: 'var(--red)',
  MEDIA: 'var(--acc3)',
  BASSA: 'var(--muted2)',
}

const TYPE_LABEL: Record<SavingSuggestion['type'], string> = {
  HIGH_SHARE: 'Categoria sovrappesata',
  WANTS_OVER_BUDGET: 'Regola 50/30/20',
  RECURRING: 'Pagamento ricorrente',
  TREND_UP: 'Trend in aumento',
}

function SuggestionCard({ s }: { s: SavingSuggestion }) {
  const color = SEVERITY_COLOR[s.severity]
  return (
    <div style={{ background: 'var(--s3)', border: `1px solid ${color}`, borderRadius: 10, padding: '12px 14px' }}>
      <div style={{ display: 'flex', alignItems: 'baseline', justifyContent: 'space-between', gap: 8, marginBottom: 4 }}>
        <div style={{ fontFamily: 'Syne', fontWeight: 700, fontSize: 12, color: 'var(--text)' }}>{s.title}</div>
        <div style={{ fontFamily: 'JetBrains Mono', fontSize: 12, color, whiteSpace: 'nowrap' }}>
          ~{formatNumber(s.potentialMonthlySaving, 2)} €/mese
        </div>
      </div>
      <div style={{ fontFamily: 'Syne', fontSize: 11, color: 'var(--muted)', lineHeight: 1.5 }}>{s.message}</div>
      <div style={{ fontFamily: 'Syne', fontSize: 10, color, marginTop: 6 }}>
        {TYPE_LABEL[s.type]}{s.category ? ` · ${s.category}` : ''}
      </div>
    </div>
  )
}

export function SavingSuggestions() {
  const { spendingInsights, isLoadingSpendingInsights } = useFinance()

  if (isLoadingSpendingInsights || !spendingInsights) return null

  return (
    <div style={{ background: 'var(--s2)', border: '1px solid var(--border)', borderRadius: 12, padding: '16px 20px' }}>
      <div style={{ fontFamily: 'Syne', fontWeight: 700, fontSize: 14, color: 'var(--text)', marginBottom: 4 }}>
        Suggerimenti di risparmio
      </div>
      <div style={{ fontFamily: 'Syne', fontSize: 12, color: 'var(--muted)', marginBottom: 14 }}>
        {spendingInsights.suggestions.length === 0
          ? 'Analisi reale dei movimenti importati: al momento non emergono categorie sovrappesate, spese ricorrenti sospette o trend in forte aumento.'
          : `Analisi reale dei movimenti importati: seguendo tutti i suggerimenti potresti liberare circa ${formatNumber(spendingInsights.totalPotentialMonthlySaving, 2)} €/mese.`}
      </div>

      {spendingInsights.suggestions.length > 0 && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
          {spendingInsights.suggestions.map((s, i) => <SuggestionCard key={i} s={s} />)}
        </div>
      )}
    </div>
  )
}
