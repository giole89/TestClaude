import { useEffect, useState } from 'react'
import { useMortgage, AmortizationYear, LoanSimulation } from '@/hooks/useMortgage'
import { formatNumber } from '@/lib/formatters'

function affordabilityColor(label: string): string {
  if (label === 'Sostenibile') return 'var(--acc)'
  if (label === 'Al limite') return 'var(--acc3)'
  return 'var(--red)'
}

function AmortizationTable({ schedule }: { schedule: AmortizationYear[] }) {
  if (schedule.length === 0) return null
  return (
    <div style={{ marginTop: 14 }}>
      <div style={{ fontFamily: 'Syne', fontWeight: 700, fontSize: 11, color: 'var(--muted2)', marginBottom: 6 }}>
        Piano di ammortamento (riepilogo annuale)
      </div>
      <div style={{ background: 'var(--s3)', border: '1px solid var(--border)', borderRadius: 10, overflow: 'hidden', maxHeight: 260, overflowY: 'auto' }}>
        <div style={{
          display: 'grid', gridTemplateColumns: '50px 1fr 1fr 1fr', gap: 8, padding: '8px 12px',
          fontFamily: 'Syne', fontWeight: 700, fontSize: 10, color: 'var(--muted)', borderBottom: '1px solid var(--border)',
        }}>
          <span>Anno</span>
          <span style={{ textAlign: 'right' }}>Capitale</span>
          <span style={{ textAlign: 'right' }}>Interessi</span>
          <span style={{ textAlign: 'right' }}>Residuo</span>
        </div>
        {schedule.map(row => (
          <div key={row.year} style={{
            display: 'grid', gridTemplateColumns: '50px 1fr 1fr 1fr', gap: 8, padding: '6px 12px',
            fontFamily: 'JetBrains Mono', fontSize: 11, color: 'var(--text)', borderBottom: '1px solid var(--border)',
          }}>
            <span>{row.year}</span>
            <span style={{ textAlign: 'right' }}>{formatNumber(row.principalPaid, 0)} €</span>
            <span style={{ textAlign: 'right', color: 'var(--muted2)' }}>{formatNumber(row.interestPaid, 0)} €</span>
            <span style={{ textAlign: 'right' }}>{formatNumber(row.remainingBalance, 0)} €</span>
          </div>
        ))}
      </div>
    </div>
  )
}

function ResultCard({ result }: { result: LoanSimulation }) {
  return (
    <div style={{ background: 'var(--s2)', border: '1px solid var(--acc3)', borderRadius: 12, padding: '16px 20px', marginTop: 16 }}>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 12, marginBottom: 14 }}>
        <div style={{ background: 'var(--s3)', border: '1px solid var(--border)', borderRadius: 10, padding: '12px 14px', textAlign: 'center' }}>
          <div style={{ fontFamily: 'Syne', fontSize: 11, color: 'var(--muted)', marginBottom: 6 }}>Rata mensile</div>
          <div style={{ fontFamily: 'Instrument Serif', fontSize: 22, color: 'var(--acc3)' }}>{formatNumber(result.monthlyPayment, 2)} €</div>
        </div>
        <div style={{ background: 'var(--s3)', border: '1px solid var(--border)', borderRadius: 10, padding: '12px 14px', textAlign: 'center' }}>
          <div style={{ fontFamily: 'Syne', fontSize: 11, color: 'var(--muted)', marginBottom: 6 }}>Totale interessi</div>
          <div style={{ fontFamily: 'Instrument Serif', fontSize: 22, color: 'var(--text)' }}>{formatNumber(result.totalInterest, 0)} €</div>
        </div>
        <div style={{ background: 'var(--s3)', border: '1px solid var(--border)', borderRadius: 10, padding: '12px 14px', textAlign: 'center' }}>
          <div style={{ fontFamily: 'Syne', fontSize: 11, color: 'var(--muted)', marginBottom: 6 }}>Costo totale</div>
          <div style={{ fontFamily: 'Instrument Serif', fontSize: 22, color: 'var(--text)' }}>{formatNumber(result.totalPaid, 0)} €</div>
        </div>
      </div>

      <div style={{ background: 'var(--s3)', borderRadius: 10, padding: '10px 14px', marginBottom: 14 }}>
        <div style={{ fontFamily: 'Syne', fontSize: 11, color: 'var(--muted)', marginBottom: 4 }}>
          Rapporto rata/reddito {result.otherActiveDebtPayments > 0 ? '(con altri debiti in essere)' : ''}
        </div>
        <div style={{ display: 'flex', alignItems: 'baseline', gap: 8 }}>
          <span style={{ fontFamily: 'JetBrains Mono', fontSize: 16, color: affordabilityColor(result.affordabilityLabel) }}>
            {formatNumber(result.combinedPaymentToIncomeRatioPct, 1)}%
          </span>
          <span style={{ fontFamily: 'Syne', fontWeight: 700, fontSize: 11, color: affordabilityColor(result.affordabilityLabel) }}>
            {result.affordabilityLabel}
          </span>
        </div>
      </div>

      {result.incomeEstimated && (
        <div style={{ fontFamily: 'Syne', fontSize: 11, color: 'var(--muted)', marginBottom: 10 }}>
          Reddito netto mensile stimato dal budget: {formatNumber(result.monthlyNetIncome, 0)} €. Per un calcolo più preciso, inseriscilo manualmente qui sopra.
        </div>
      )}

      {result.affordabilityWarning && (
        <div style={{
          padding: '10px 12px', borderRadius: 8, background: 'rgba(239,68,68,0.1)',
          border: '1px solid var(--red)', fontFamily: 'Syne', fontSize: 12, color: 'var(--red)', lineHeight: 1.5, marginBottom: 10,
        }}>
          <span style={{ fontWeight: 700 }}>⚠ Sostenibilità: </span>{result.affordabilityWarning}
        </div>
      )}

      <AmortizationTable schedule={result.schedule} />
    </div>
  )
}

export function LoanCalculator() {
  const { incomeEstimate, simulateLoan, loanResult, isSimulatingLoan, loanError } = useMortgage()

  const [loanAmount, setLoanAmount] = useState('')
  const [interestRatePct, setInterestRatePct] = useState('')
  const [months, setMonths] = useState('60')
  const [monthlyNetIncome, setMonthlyNetIncome] = useState('')

  useEffect(() => {
    if (incomeEstimate?.estimatedMonthlyIncome != null && monthlyNetIncome === '') {
      setMonthlyNetIncome(String(incomeEstimate.estimatedMonthlyIncome))
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [incomeEstimate?.estimatedMonthlyIncome])

  const loanNum = parseFloat(loanAmount)
  const rateNum = parseFloat(interestRatePct)
  const monthsNum = parseInt(months, 10)
  const valid = !isNaN(loanNum) && loanNum > 0 && !isNaN(rateNum) && rateNum > 0 && !isNaN(monthsNum) && monthsNum > 0

  const handleSimulate = async () => {
    if (!valid) return
    const incomeNum = parseFloat(monthlyNetIncome)
    await simulateLoan({
      loanAmount: loanNum,
      interestRatePct: rateNum,
      months: monthsNum,
      monthlyNetIncome: monthlyNetIncome.trim() === '' || isNaN(incomeNum) ? null : incomeNum,
    })
  }

  return (
    <div style={{ background: 'var(--s2)', border: '1px solid var(--border)', borderRadius: 12, padding: '16px 20px' }}>
      <div style={{ fontFamily: 'Syne', fontWeight: 700, fontSize: 14, color: 'var(--text)', marginBottom: 4 }}>
        Calcolatore finanziamento / prestito personale
      </div>
      <div style={{ fontFamily: 'Syne', fontSize: 12, color: 'var(--muted)', marginBottom: 14 }}>
        Per prestiti personali, cessioni del quinto o altri finanziamenti: calcola la rata e verifica quanto pesa sul tuo reddito.
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(160px, 1fr))', gap: 10, marginBottom: 12 }}>
        <label style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
          <span style={{ fontFamily: 'Syne', fontSize: 11, color: 'var(--muted2)' }}>Importo finanziamento (€)</span>
          <input
            value={loanAmount} onChange={e => setLoanAmount(e.target.value)} type="number" placeholder="es. 15000"
            style={{ background: 'var(--s3)', border: '1px solid var(--border)', borderRadius: 8, padding: '10px 12px', color: 'var(--text)', fontFamily: 'JetBrains Mono', fontSize: 13, outline: 'none' }}
          />
        </label>
        <label style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
          <span style={{ fontFamily: 'Syne', fontSize: 11, color: 'var(--muted2)' }}>Tasso di interesse annuo (TAN %)</span>
          <input
            value={interestRatePct} onChange={e => setInterestRatePct(e.target.value)} type="number" step="0.01" placeholder="es. 6.5"
            style={{ background: 'var(--s3)', border: '1px solid var(--border)', borderRadius: 8, padding: '10px 12px', color: 'var(--text)', fontFamily: 'JetBrains Mono', fontSize: 13, outline: 'none' }}
          />
        </label>
        <label style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
          <span style={{ fontFamily: 'Syne', fontSize: 11, color: 'var(--muted2)' }}>Durata (mesi)</span>
          <input
            value={months} onChange={e => setMonths(e.target.value)} type="number" placeholder="es. 60"
            style={{ background: 'var(--s3)', border: '1px solid var(--border)', borderRadius: 8, padding: '10px 12px', color: 'var(--text)', fontFamily: 'JetBrains Mono', fontSize: 13, outline: 'none' }}
          />
        </label>
        <label style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
          <span style={{ fontFamily: 'Syne', fontSize: 11, color: 'var(--muted2)' }}>Reddito netto mensile (€, opzionale)</span>
          <input
            value={monthlyNetIncome} onChange={e => setMonthlyNetIncome(e.target.value)} type="number" placeholder="stimato dal budget se vuoto"
            style={{ background: 'var(--s3)', border: '1px solid var(--border)', borderRadius: 8, padding: '10px 12px', color: 'var(--text)', fontFamily: 'JetBrains Mono', fontSize: 13, outline: 'none' }}
          />
        </label>
      </div>

      {loanError && (
        <div style={{ color: 'var(--red)', fontFamily: 'Syne', fontSize: 12, marginBottom: 10 }}>
          {(loanError as any)?.response?.data?.error || 'Impossibile calcolare la simulazione'}
        </div>
      )}

      <button
        onClick={handleSimulate}
        disabled={!valid || isSimulatingLoan}
        style={{
          padding: '10px 20px', borderRadius: 8, background: 'var(--acc3)', border: 'none',
          color: '#07080a', fontFamily: 'Syne', fontWeight: 700, fontSize: 13,
          cursor: valid ? 'pointer' : 'default', opacity: valid ? 1 : 0.5,
        }}
      >
        {isSimulatingLoan ? 'Calcolo…' : 'Calcola rata finanziamento'}
      </button>

      {loanResult && <ResultCard result={loanResult} />}
    </div>
  )
}
