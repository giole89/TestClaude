import { MortgageCalculator } from '@/components/mortgage/MortgageCalculator'
import { LoanCalculator } from '@/components/mortgage/LoanCalculator'

export function MortgagePage() {
  return (
    <div style={{ padding: 24, overflowY: 'auto', height: 'calc(100vh - 112px)', display: 'flex', flexDirection: 'column', gap: 16 }}>
      <div>
        <div style={{ fontFamily: 'Syne', fontWeight: 700, fontSize: 18, color: 'var(--text)' }}>
          Mutuo e Finanziamenti
        </div>
        <div style={{ fontFamily: 'Syne', fontSize: 12, color: 'var(--muted)', marginTop: 2 }}>
          Calcola la rata di un mutuo o di un finanziamento e verifica quanto pesa sul tuo reddito, tenendo conto anche
          degli altri debiti già tracciati tra le spese fisse.
        </div>
      </div>

      <MortgageCalculator />
      <LoanCalculator />
    </div>
  )
}
