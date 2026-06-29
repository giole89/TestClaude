import { PieChart, Pie, Cell, Tooltip, Legend, ResponsiveContainer } from 'recharts'
import { useFinance } from '@/hooks/useFinance'
import { formatNumber } from '@/lib/formatters'

const COLORS = [
  'var(--acc3)', 'var(--acc)', 'var(--red)', '#f4a261', '#9d8df1',
  '#4fb6e8', '#e878c0', '#80c97a', '#e6c84b', '#bb8fce', '#5dade2',
]

function CategoryTooltip({ active, payload }: any) {
  if (!active || !payload || !payload.length) return null
  const d = payload[0].payload
  return (
    <div style={{
      background: 'var(--s3)', border: '1px solid var(--border)', borderRadius: 8,
      padding: '6px 10px', fontFamily: 'Syne', fontSize: 12, color: 'var(--text)',
    }}>
      {d.category}: {formatNumber(d.amount, 2)} €
    </div>
  )
}

export function ExpensesPieChart() {
  const { currentMonthExpenses, isLoadingCurrentMonthExpenses } = useFinance()

  if (isLoadingCurrentMonthExpenses || !currentMonthExpenses) {
    return null
  }

  return (
    <div style={{ background: 'var(--s2)', border: '1px solid var(--border)', borderRadius: 12, padding: '16px 20px' }}>
      <div style={{ fontFamily: 'Syne', fontWeight: 700, fontSize: 14, color: 'var(--text)', marginBottom: 4 }}>
        Spese del mese — {currentMonthExpenses.periodLabel}
      </div>
      <div style={{ fontFamily: 'Syne', fontSize: 12, color: 'var(--muted)', marginBottom: 14 }}>
        Spese variabili effettivamente sostenute nel mese in corso, divise per categoria. Totale: {formatNumber(currentMonthExpenses.total, 2)} €
      </div>

      {currentMonthExpenses.byCategory.length === 0 ? (
        <div style={{ textAlign: 'center', padding: 16, color: 'var(--muted)', fontFamily: 'Syne', fontSize: 12 }}>
          Nessuna spesa registrata per questo mese.
        </div>
      ) : (
        <div style={{ display: 'flex', gap: 20, flexWrap: 'wrap', alignItems: 'center' }}>
          <div style={{ minWidth: 280, flex: '1 1 320px' }}>
            <ResponsiveContainer width="100%" height={280}>
              <PieChart>
                <Pie
                  data={currentMonthExpenses.byCategory}
                  dataKey="amount"
                  nameKey="category"
                  cx="50%"
                  cy="50%"
                  outerRadius={100}
                  label={({ category, percent }) => `${category} ${(percent * 100).toFixed(0)}%`}
                  labelLine={false}
                >
                  {currentMonthExpenses.byCategory.map((_, idx) => (
                    <Cell key={idx} fill={COLORS[idx % COLORS.length]} />
                  ))}
                </Pie>
                <Tooltip content={<CategoryTooltip />} />
                <Legend wrapperStyle={{ fontFamily: 'Syne', fontSize: 11 }} />
              </PieChart>
            </ResponsiveContainer>
          </div>

          <div style={{ flex: '1 1 280px', minWidth: 240 }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', fontFamily: 'JetBrains Mono', fontSize: 11 }}>
              <thead>
                <tr style={{ borderBottom: '1px solid var(--border)' }}>
                  <th style={{ textAlign: 'left', padding: '4px 8px', color: 'var(--muted2)', fontFamily: 'Syne', fontSize: 10 }}></th>
                  <th style={{ textAlign: 'left', padding: '4px 8px', color: 'var(--muted2)', fontFamily: 'Syne', fontSize: 10 }}>Categoria</th>
                  <th style={{ textAlign: 'right', padding: '4px 8px', color: 'var(--muted2)', fontFamily: 'Syne', fontSize: 10 }}>Importo</th>
                  <th style={{ textAlign: 'right', padding: '4px 8px', color: 'var(--muted2)', fontFamily: 'Syne', fontSize: 10 }}>%</th>
                </tr>
              </thead>
              <tbody>
                {currentMonthExpenses.byCategory.map((c, idx) => (
                  <tr key={c.category} style={{ borderBottom: '1px solid var(--border)' }}>
                    <td style={{ padding: '4px 8px' }}>
                      <span style={{
                        display: 'inline-block', width: 8, height: 8, borderRadius: '50%',
                        background: COLORS[idx % COLORS.length],
                      }} />
                    </td>
                    <td style={{ padding: '4px 8px', color: 'var(--text)' }}>{c.category}</td>
                    <td style={{ padding: '4px 8px', textAlign: 'right', color: 'var(--text)' }}>{formatNumber(c.amount, 2)} €</td>
                    <td style={{ padding: '4px 8px', textAlign: 'right', color: 'var(--muted)' }}>
                      {currentMonthExpenses.total > 0 ? ((c.amount / currentMonthExpenses.total) * 100).toFixed(1) : '0.0'}%
                    </td>
                  </tr>
                ))}
              </tbody>
              <tfoot>
                <tr>
                  <td />
                  <td style={{ padding: '6px 8px', color: 'var(--text)', fontFamily: 'Syne', fontWeight: 700, fontSize: 11 }}>Totale</td>
                  <td style={{ padding: '6px 8px', textAlign: 'right', color: 'var(--text)', fontFamily: 'Syne', fontWeight: 700, fontSize: 11 }}>
                    {formatNumber(currentMonthExpenses.total, 2)} €
                  </td>
                  <td style={{ padding: '6px 8px', textAlign: 'right', color: 'var(--muted)', fontFamily: 'Syne', fontSize: 11 }}>100%</td>
                </tr>
              </tfoot>
            </table>
          </div>
        </div>
      )}
    </div>
  )
}
