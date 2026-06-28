import { useState } from 'react'
import { useFinance } from '@/hooks/useFinance'
import { formatNumber } from '@/lib/formatters'

const CATEGORY_OPTIONS = ['Casa e utenze', 'Trasporti', 'Salute', 'Abbonamenti', 'Assicurazioni', 'Altro']

export function FixedExpensesManager() {
  const { fixedExpenses, createFixedExpense, deleteFixedExpense, isCreatingFixedExpense } = useFinance()
  const [name, setName] = useState('')
  const [category, setCategory] = useState(CATEGORY_OPTIONS[0])
  const [amount, setAmount] = useState('')
  const [error, setError] = useState<string | null>(null)

  const total = fixedExpenses.filter(f => f.active).reduce((sum, f) => sum + f.amount, 0)

  const handleAdd = async () => {
    setError(null)
    const value = parseFloat(amount)
    if (!name.trim() || isNaN(value) || value <= 0) return
    try {
      await createFixedExpense({ name: name.trim(), category, amount: value })
      setName(''); setAmount('')
    } catch (e: any) {
      setError(e?.response?.data?.error || 'Impossibile salvare la spesa fissa')
    }
  }

  return (
    <div style={{ background: 'var(--s2)', border: '1px solid var(--border)', borderRadius: 12, padding: '16px 20px' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', marginBottom: 4 }}>
        <div style={{ fontFamily: 'Syne', fontWeight: 700, fontSize: 14, color: 'var(--text)' }}>
          Costi fissi mensili
        </div>
        <div style={{ fontFamily: 'JetBrains Mono', fontSize: 13, color: 'var(--text)' }}>
          Totale: {formatNumber(total, 2)} €
        </div>
      </div>
      <div style={{ fontFamily: 'Syne', fontSize: 12, color: 'var(--muted)', marginBottom: 12 }}>
        Affitto, mutuo, utenze, abbonamenti: spese ricorrenti che il budget userà come base fissa per il mese successivo.
      </div>

      <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', alignItems: 'center', marginBottom: 12 }}>
        <input
          value={name}
          onChange={e => setName(e.target.value)}
          placeholder="Nome (es. Affitto)"
          style={{
            flex: '1 1 160px', background: 'var(--s3)', border: '1px solid var(--border)',
            borderRadius: 8, padding: '10px 12px', color: 'var(--text)', fontFamily: 'Syne', fontSize: 13, outline: 'none',
          }}
        />
        <select
          value={category}
          onChange={e => setCategory(e.target.value)}
          style={{
            background: 'var(--s3)', border: '1px solid var(--border)', borderRadius: 8,
            padding: '10px 12px', color: 'var(--text)', fontFamily: 'Syne', fontSize: 13, outline: 'none',
          }}
        >
          {CATEGORY_OPTIONS.map(c => <option key={c} value={c}>{c}</option>)}
        </select>
        <input
          value={amount}
          onChange={e => setAmount(e.target.value)}
          placeholder="€ / mese"
          type="number"
          style={{
            width: 100, background: 'var(--s3)', border: '1px solid var(--border)',
            borderRadius: 8, padding: '10px 12px', color: 'var(--text)', fontFamily: 'JetBrains Mono', fontSize: 13, outline: 'none',
          }}
        />
        <button
          onClick={handleAdd}
          disabled={isCreatingFixedExpense || !name.trim() || !amount}
          style={{
            padding: '10px 18px', borderRadius: 8, background: 'var(--acc)', border: 'none',
            color: '#07080a', fontFamily: 'Syne', fontWeight: 700, fontSize: 13,
            cursor: isCreatingFixedExpense ? 'default' : 'pointer', opacity: isCreatingFixedExpense ? 0.6 : 1,
          }}
        >
          + Aggiungi
        </button>
      </div>

      {error && (
        <div style={{ color: 'var(--red)', fontFamily: 'Syne', fontSize: 12, marginBottom: 8 }}>{error}</div>
      )}

      {fixedExpenses.length === 0 ? (
        <div style={{ textAlign: 'center', padding: 16, color: 'var(--muted)', fontFamily: 'Syne', fontSize: 12 }}>
          Nessuna spesa fissa inserita.
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
          {fixedExpenses.map(f => (
            <div key={f.id} style={{
              display: 'flex', alignItems: 'center', gap: 10, padding: '8px 10px',
              background: 'var(--s3)', borderRadius: 8, fontFamily: 'JetBrains Mono', fontSize: 12,
            }}>
              <div style={{ flex: 1, color: 'var(--text)', fontFamily: 'Syne', fontWeight: 600 }}>{f.name}</div>
              <div style={{ color: 'var(--muted)' }}>{f.category}</div>
              <div style={{ color: 'var(--text)', minWidth: 80, textAlign: 'right' }}>{formatNumber(f.amount, 2)} €</div>
              <button
                onClick={() => deleteFixedExpense(f.id)}
                style={{
                  background: 'none', border: 'none', color: 'var(--red)', cursor: 'pointer',
                  fontFamily: 'Syne', fontSize: 11, padding: '4px 8px',
                }}
              >
                ✕
              </button>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
