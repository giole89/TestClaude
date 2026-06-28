import { useState } from 'react'
import { useFinance } from '@/hooks/useFinance'
import { StatementUpload } from '@/components/finance/StatementUpload'
import { FixedExpensesManager } from '@/components/finance/FixedExpensesManager'
import { BudgetSummary } from '@/components/finance/BudgetSummary'
import { QuestionnaireWizard } from '@/components/finance/QuestionnaireWizard'
import { formatNumber, formatDate, colorForChange } from '@/lib/formatters'

function TransactionsHistory() {
  const { transactions, deleteTransaction } = useFinance()
  const [show, setShow] = useState(false)

  return (
    <div>
      <button
        onClick={() => setShow(s => !s)}
        style={{ background: 'none', border: 'none', color: 'var(--muted2)', fontFamily: 'Syne', fontSize: 12, cursor: 'pointer', padding: 0 }}
      >
        {show ? '▾' : '▸'} Movimenti importati ({transactions.length})
      </button>
      {show && (
        <div style={{ background: 'var(--s2)', border: '1px solid var(--border)', borderRadius: 12, overflow: 'hidden', marginTop: 8, maxHeight: 360, overflowY: 'auto' }}>
          {transactions.length === 0 ? (
            <div style={{ padding: 16, textAlign: 'center', color: 'var(--muted)', fontFamily: 'Syne', fontSize: 12 }}>
              Nessun movimento importato.
            </div>
          ) : (
            transactions.map(t => (
              <div key={t.id} style={{
                display: 'grid', gridTemplateColumns: '90px 1fr 120px 90px 30px', gap: 8, alignItems: 'center',
                padding: '8px 16px', borderBottom: '1px solid var(--border)', fontFamily: 'JetBrains Mono', fontSize: 11,
              }}>
                <div style={{ color: 'var(--muted2)' }}>{formatDate(t.date)}</div>
                <div style={{ color: 'var(--text)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{t.description}</div>
                <div style={{ color: 'var(--muted)' }}>{t.category}</div>
                <div style={{ textAlign: 'right', color: colorForChange(t.amount) }}>{formatNumber(t.amount, 2)} €</div>
                <button
                  onClick={() => deleteTransaction(t.id)}
                  style={{ background: 'none', border: 'none', color: 'var(--red)', cursor: 'pointer', fontSize: 11 }}
                >
                  ✕
                </button>
              </div>
            ))
          )}
        </div>
      )}
    </div>
  )
}

export function PersonalFinancePage() {
  return (
    <div style={{ padding: 24, overflowY: 'auto', height: 'calc(100vh - 112px)', display: 'flex', flexDirection: 'column', gap: 16 }}>
      <div>
        <div style={{ fontFamily: 'Syne', fontWeight: 700, fontSize: 18, color: 'var(--text)' }}>
          Finanza Personale
        </div>
        <div style={{ fontFamily: 'Syne', fontSize: 12, color: 'var(--muted)', marginTop: 2 }}>
          Importa l'estratto conto, gestisci i costi fissi e ottieni il budget del mese successivo con la quota di risparmio investibile
        </div>
      </div>

      <StatementUpload />
      <TransactionsHistory />
      <FixedExpensesManager />
      <BudgetSummary />
      <QuestionnaireWizard />
    </div>
  )
}
