import { useFinance, CategoryAmount } from '@/hooks/useFinance'
import { formatNumber } from '@/lib/formatters'

function CategoryBarList({ title, categories, barColor }: { title: string; categories: CategoryAmount[]; barColor: string }) {
  if (categories.length === 0) return null
  const max = Math.max(1, ...categories.map(c => c.amount))
  return (
    <div>
      <div style={{ fontFamily: 'Syne', fontWeight: 700, fontSize: 12, color: 'var(--muted2)', marginBottom: 8 }}>
        {title}
      </div>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
        {categories.map(c => (
          <div key={c.category} style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            <div style={{ width: 110, fontFamily: 'Syne', fontSize: 11, color: 'var(--muted2)' }}>{c.category}</div>
            <div style={{ flex: 1, background: 'var(--s3)', borderRadius: 6, height: 14, position: 'relative', overflow: 'hidden' }}>
              <div style={{
                position: 'absolute', left: 0, top: 0, height: '100%',
                width: `${(c.amount / max) * 100}%`, background: barColor, borderRadius: 6,
              }} />
            </div>
            <div style={{ width: 80, textAlign: 'right', fontFamily: 'JetBrains Mono', fontSize: 11, color: 'var(--text)' }}>
              {formatNumber(c.amount, 2)} €
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}

export function BudgetSummary() {
  const { budget, isLoadingBudget } = useFinance()

  if (isLoadingBudget || !budget) {
    return (
      <div style={{ textAlign: 'center', padding: 24, color: 'var(--muted)', fontFamily: 'Syne' }}>
        Calcolo budget…
      </div>
    )
  }

  return (
    <div style={{ background: 'var(--s2)', border: '1px solid var(--border)', borderRadius: 12, padding: '16px 20px' }}>
      <div style={{ fontFamily: 'Syne', fontWeight: 700, fontSize: 14, color: 'var(--text)', marginBottom: 4 }}>
        Budget previsionale — {budget.periodLabel}
      </div>
      <div style={{ fontFamily: 'Syne', fontSize: 12, color: 'var(--muted)', marginBottom: 14 }}>
        {budget.hasEnoughData
          ? `Stima basata sulla media degli ultimi ${budget.monthsOfHistory} ${budget.monthsOfHistory === 1 ? 'mese completo' : 'mesi completi'} (il mese in corso, non ancora concluso, non è incluso nella media: per la spesa reale fin qui vedi il grafico qui sotto).`
          : budget.basedOnCurrentMonthOnly
          ? 'Stima provvisoria basata solo sul mese in corso (non ancora concluso): potrebbe essere sottostimata rispetto a un mese intero. Importa anche l\'estratto conto di un mese precedente per una stima più affidabile.'
          : 'Servono dati di almeno un mese passato completo per stimare in modo affidabile entrate e spese variabili: importa l\'estratto conto del mese precedente.'}
      </div>

      {budget.deficit && (
        <div style={{
          background: 'rgba(239,68,68,0.1)', border: '1px solid var(--red)', borderRadius: 10,
          padding: '10px 14px', marginBottom: 14, display: 'flex', alignItems: 'center', gap: 10,
        }}>
          <span style={{ fontSize: 18 }} aria-hidden="true">⚠</span>
          <div style={{ fontFamily: 'Syne', fontSize: 12, color: 'var(--red)' }}>
            Il mese successivo è previsto <strong>in perdita di {formatNumber(Math.abs(budget.projectedSavings), 2)} €</strong>:
            le entrate stimate non bastano a coprire costi fissi e variabili previsti.
          </div>
        </div>
      )}

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 12, marginBottom: 16 }}>
        {[
          { label: 'Entrate stimate', value: budget.estimatedIncome, color: 'var(--text)' },
          { label: 'Costi fissi', value: budget.fixedCosts, color: 'var(--red)' },
          { label: 'Costi variabili stimati', value: budget.variableCostsEstimate, color: 'var(--acc3)' },
          {
            label: budget.deficit ? 'Saldo previsto (in perdita)' : 'Risparmio investibile',
            value: budget.deficit ? budget.projectedSavings : budget.investableAmount,
            color: budget.deficit ? 'var(--red)' : 'var(--acc)',
          },
        ].map(s => (
          <div key={s.label} style={{ background: 'var(--s3)', border: '1px solid var(--border)', borderRadius: 10, padding: '12px 14px', textAlign: 'center' }}>
            <div style={{ fontFamily: 'Syne', fontSize: 11, color: 'var(--muted)', marginBottom: 6 }}>{s.label}</div>
            <div style={{ fontFamily: 'Instrument Serif', fontSize: 20, color: s.color }}>
              {formatNumber(s.value, 2)} €
            </div>
          </div>
        ))}
      </div>

      <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
        <CategoryBarList title="Entrate medie per categoria" categories={budget.incomeByCategory} barColor="var(--acc)" />
        <CategoryBarList title="Spese variabili medie per categoria" categories={budget.variableByCategory} barColor="var(--acc3)" />
      </div>
    </div>
  )
}
