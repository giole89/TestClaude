import { useState } from 'react'
import { useFinance } from '@/hooks/useFinance'
import { StatementUpload } from '@/components/finance/StatementUpload'
import { FixedExpensesManager } from '@/components/finance/FixedExpensesManager'
import { BudgetSummary } from '@/components/finance/BudgetSummary'
import { ExpensesPieChart } from '@/components/finance/ExpensesPieChart'
import { QuestionnaireWizard } from '@/components/finance/QuestionnaireWizard'
import { formatNumber, formatDate, colorForChange } from '@/lib/formatters'

type TypeFilter = 'ALL' | 'INCOME' | 'VARIABLE_EXPENSE'

function TransactionsHistory() {
  const { transactions, deleteTransaction, deleteTransactions, deleteAllTransactions, isDeletingTransactions, isDeletingAllTransactions } = useFinance()
  const [show, setShow] = useState(true)
  const [selected, setSelected] = useState<Set<string>>(new Set())
  const [typeFilter, setTypeFilter] = useState<TypeFilter>('ALL')

  const filtered = typeFilter === 'ALL' ? transactions : transactions.filter(t => t.type === typeFilter)

  const toggle = (id: string) => {
    setSelected(prev => {
      const next = new Set(prev)
      next.has(id) ? next.delete(id) : next.add(id)
      return next
    })
  }

  const allSelected = filtered.length > 0 && filtered.every(t => selected.has(t.id))

  const toggleAll = () => {
    setSelected(allSelected ? new Set() : new Set(filtered.map(t => t.id)))
  }

  const handleDeleteSelected = async () => {
    if (selected.size === 0) return
    if (!window.confirm(`Eliminare ${selected.size} movimenti selezionati?`)) return
    await deleteTransactions(Array.from(selected))
    setSelected(new Set())
  }

  const handleDeleteAll = async () => {
    if (transactions.length === 0) return
    if (!window.confirm(`Eliminare tutti i ${transactions.length} movimenti importati? L'operazione non è reversibile.`)) return
    await deleteAllTransactions()
    setSelected(new Set())
  }

  return (
    <div>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 8 }}>
        <button
          onClick={() => setShow(s => !s)}
          style={{ background: 'none', border: 'none', color: 'var(--muted2)', fontFamily: 'Syne', fontSize: 12, cursor: 'pointer', padding: 0 }}
        >
          {show ? '▾' : '▸'} Movimenti importati ({transactions.length})
        </button>
        {show && (
          <div style={{ display: 'flex', gap: 8 }}>
            {(['ALL', 'INCOME', 'VARIABLE_EXPENSE'] as TypeFilter[]).map(f => (
              <button
                key={f}
                onClick={() => setTypeFilter(f)}
                style={{
                  background: typeFilter === f ? 'var(--s3)' : 'none',
                  border: '1px solid var(--border)', color: typeFilter === f ? 'var(--text)' : 'var(--muted2)',
                  borderRadius: 6, padding: '3px 10px', fontFamily: 'Syne', fontSize: 11, cursor: 'pointer',
                }}
              >
                {f === 'ALL' ? 'Tutti' : f === 'INCOME' ? 'Entrate' : 'Uscite'}
              </button>
            ))}
            {selected.size > 0 && (
              <button
                onClick={handleDeleteSelected}
                disabled={isDeletingTransactions}
                style={{ background: 'none', border: '1px solid var(--red)', color: 'var(--red)', borderRadius: 6, padding: '3px 10px', fontFamily: 'Syne', fontSize: 11, cursor: 'pointer' }}
              >
                Elimina selezionati ({selected.size})
              </button>
            )}
            <button
              onClick={handleDeleteAll}
              disabled={isDeletingAllTransactions}
              style={{ background: 'none', border: '1px solid var(--border)', color: 'var(--muted)', borderRadius: 6, padding: '3px 10px', fontFamily: 'Syne', fontSize: 11, cursor: 'pointer' }}
            >
              Elimina tutti
            </button>
          </div>
        )}
      </div>
      {show && (
        <div style={{ background: 'var(--s2)', border: '1px solid var(--border)', borderRadius: 12, overflow: 'hidden', marginTop: 8, maxHeight: 360, overflowY: 'auto' }}>
          {filtered.length === 0 ? (
            <div style={{ padding: 16, textAlign: 'center', color: 'var(--muted)', fontFamily: 'Syne', fontSize: 12 }}>
              {transactions.length === 0 ? 'Nessun movimento importato.' : 'Nessun movimento per questo filtro.'}
            </div>
          ) : (
            <>
              <div style={{
                display: 'grid', gridTemplateColumns: '24px 90px 1fr 120px 90px 30px', gap: 8, alignItems: 'center',
                padding: '6px 16px', borderBottom: '1px solid var(--border)', fontFamily: 'Syne', fontSize: 10, color: 'var(--muted2)',
              }}>
                <input type="checkbox" checked={allSelected} onChange={toggleAll} />
                <div>Tutti</div>
                <div />
                <div />
                <div />
                <div />
              </div>
              {filtered.map(t => (
                <div key={t.id} style={{
                  display: 'grid', gridTemplateColumns: '24px 90px 1fr 120px 90px 30px', gap: 8, alignItems: 'center',
                  padding: '8px 16px', borderBottom: '1px solid var(--border)', fontFamily: 'JetBrains Mono', fontSize: 11,
                }}>
                  <input type="checkbox" checked={selected.has(t.id)} onChange={() => toggle(t.id)} />
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
              ))}
            </>
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
      <ExpensesPieChart />
      <FixedExpensesManager />
      <BudgetSummary />
      <QuestionnaireWizard />
    </div>
  )
}
