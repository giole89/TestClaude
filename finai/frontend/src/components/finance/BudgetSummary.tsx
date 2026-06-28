import { useFinance } from '@/hooks/useFinance'
import { formatNumber } from '@/lib/formatters'

export function BudgetSummary() {
  const { budget, isLoadingBudget } = useFinance()

  if (isLoadingBudget || !budget) {
    return (
      <div style={{ textAlign: 'center', padding: 24, color: 'var(--muted)', fontFamily: 'Syne' }}>
        Calcolo budget…
      </div>
    )
  }

  const maxCategory = Math.max(1, ...budget.variableByCategory.map(c => c.amount))

  return (
    <div style={{ background: 'var(--s2)', border: '1px solid var(--border)', borderRadius: 12, padding: '16px 20px' }}>
      <div style={{ fontFamily: 'Syne', fontWeight: 700, fontSize: 14, color: 'var(--text)', marginBottom: 4 }}>
        Budget previsionale — {budget.periodLabel}
      </div>
      <div style={{ fontFamily: 'Syne', fontSize: 12, color: 'var(--muted)', marginBottom: 14 }}>
        {budget.hasEnoughData
          ? `Stima basata sulla media degli ultimi ${budget.monthsOfHistory} mesi importati.`
          : 'Importa almeno un estratto conto per stime più precise su entrate e spese variabili.'}
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 12, marginBottom: 16 }}>
        {[
          { label: 'Entrate stimate', value: budget.estimatedIncome, color: 'var(--text)' },
          { label: 'Costi fissi', value: budget.fixedCosts, color: 'var(--red)' },
          { label: 'Costi variabili stimati', value: budget.variableCostsEstimate, color: 'var(--acc3)' },
          { label: 'Risparmio investibile', value: budget.investableAmount, color: 'var(--acc)' },
        ].map(s => (
          <div key={s.label} style={{ background: 'var(--s3)', border: '1px solid var(--border)', borderRadius: 10, padding: '12px 14px', textAlign: 'center' }}>
            <div style={{ fontFamily: 'Syne', fontSize: 11, color: 'var(--muted)', marginBottom: 6 }}>{s.label}</div>
            <div style={{ fontFamily: 'Instrument Serif', fontSize: 20, color: s.color }}>
              {formatNumber(s.value, 2)} €
            </div>
          </div>
        ))}
      </div>

      {budget.variableByCategory.length > 0 && (
        <div>
          <div style={{ fontFamily: 'Syne', fontWeight: 700, fontSize: 12, color: 'var(--muted2)', marginBottom: 8 }}>
            Spese variabili medie per categoria
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
            {budget.variableByCategory.map(c => (
              <div key={c.category} style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                <div style={{ width: 110, fontFamily: 'Syne', fontSize: 11, color: 'var(--muted2)' }}>{c.category}</div>
                <div style={{ flex: 1, background: 'var(--s3)', borderRadius: 6, height: 14, position: 'relative', overflow: 'hidden' }}>
                  <div style={{
                    position: 'absolute', left: 0, top: 0, height: '100%',
                    width: `${(c.amount / maxCategory) * 100}%`, background: 'var(--acc3)', borderRadius: 6,
                  }} />
                </div>
                <div style={{ width: 80, textAlign: 'right', fontFamily: 'JetBrains Mono', fontSize: 11, color: 'var(--text)' }}>
                  {formatNumber(c.amount, 2)} €
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}
