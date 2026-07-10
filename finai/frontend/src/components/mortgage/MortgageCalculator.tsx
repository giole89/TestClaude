import { useEffect, useState } from 'react'
import { useMortgage, AmortizationYear, MortgageSimulation, PurchaseType, HomeSaleAdvice, DurationOption, TaxDeductionAdvice } from '@/hooks/useMortgage'
import { formatNumber } from '@/lib/formatters'

function affordabilityColor(label: string): string {
  if (label === 'Sostenibile') return 'var(--acc)'
  if (label === 'Al limite') return 'var(--acc3)'
  return 'var(--red)'
}

const PURCHASE_TYPE_OPTIONS: Array<{ value: PurchaseType; label: string }> = [
  { value: 'PRIMA_CASA_PRIVATO', label: 'Prima casa — acquisto da privato' },
  { value: 'PRIMA_CASA_COSTRUTTORE', label: 'Prima casa — acquisto da costruttore (con IVA)' },
  { value: 'SECONDA_CASA_PRIVATO', label: 'Seconda casa — acquisto da privato' },
  { value: 'SECONDA_CASA_COSTRUTTORE', label: 'Seconda casa — acquisto da costruttore (con IVA)' },
]

const inputStyle: React.CSSProperties = {
  background: 'var(--s3)', border: '1px solid var(--border)', borderRadius: 8,
  padding: '10px 12px', color: 'var(--text)', fontFamily: 'JetBrains Mono', fontSize: 13, outline: 'none',
}
const labelStyle: React.CSSProperties = { fontFamily: 'Syne', fontSize: 11, color: 'var(--muted2)' }

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

function CostRow({ label, amount, estimated }: { label: string; amount: number; estimated: boolean }) {
  return (
    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '6px 0', borderBottom: '1px solid var(--border)' }}>
      <span style={{ fontFamily: 'Syne', fontSize: 12, color: 'var(--text)' }}>
        {label} {estimated && <span style={{ color: 'var(--muted)', fontSize: 10 }}>(stimato)</span>}
      </span>
      <span style={{ fontFamily: 'JetBrains Mono', fontSize: 12, color: 'var(--text)' }}>{formatNumber(amount, 0)} €</span>
    </div>
  )
}

function CostBreakdownCard({ result }: { result: MortgageSimulation }) {
  return (
    <div style={{ marginTop: 14, background: 'var(--s3)', borderRadius: 10, padding: '12px 14px' }}>
      <div style={{ fontFamily: 'Syne', fontWeight: 700, fontSize: 12, color: 'var(--text)', marginBottom: 8 }}>
        Tutto ciò che non rientra nel mutuo
      </div>
      <CostRow label="Capitale proprio (prezzo − mutuo)" amount={result.downPayment} estimated={false} />
      <CostRow label="Notaio" amount={result.notaryCosts} estimated={result.notaryCostsEstimated} />
      <CostRow label="Istruttoria bancaria" amount={result.originationFees} estimated={result.originationFeesEstimated} />
      <CostRow label="Perizia immobile" amount={result.appraisalFees} estimated={result.appraisalFeesEstimated} />
      <CostRow
        label={result.agencyFeeMode === 'PERCENTAGE'
          ? `Agenzia immobiliare (${formatNumber(result.agencyFeesBase, 0)}€ + IVA ${formatNumber(result.agencyFeesIva, 0)}€)`
          : 'Agenzia immobiliare'}
        amount={result.agencyFees}
        estimated={result.agencyFeesEstimated}
      />
      <CostRow label="Imposta di registro / IVA" amount={result.registrationTax} estimated={result.registrationTaxEstimated} />
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', paddingTop: 8, marginTop: 4 }}>
        <span style={{ fontFamily: 'Syne', fontWeight: 700, fontSize: 13, color: 'var(--text)' }}>Totale da pagare oltre al mutuo</span>
        <span style={{ fontFamily: 'Instrument Serif', fontSize: 18, color: 'var(--acc)' }}>{formatNumber(result.totalOutOfPocketCost, 0)} €</span>
      </div>
      <div style={{ fontFamily: 'Syne', fontSize: 10, color: 'var(--muted)', marginTop: 8, lineHeight: 1.4 }}>
        {result.registrationTaxNote}
      </div>
    </div>
  )
}

function HomeSaleCard({ sale }: { sale: HomeSaleAdvice }) {
  return (
    <div style={{
      padding: '10px 12px', borderRadius: 8, marginBottom: 10,
      background: sale.netProceeds > 0 ? 'rgba(110,231,183,0.08)' : 'rgba(239,68,68,0.1)',
      border: `1px solid ${sale.netProceeds > 0 ? 'var(--acc)' : 'var(--red)'}`,
    }}>
      <div style={{ fontFamily: 'Syne', fontWeight: 700, fontSize: 12, color: 'var(--text)', marginBottom: 6 }}>
        🏡 Vendita immobile esistente
      </div>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(120px, 1fr))', gap: 8, marginBottom: 8 }}>
        <div>
          <div style={{ fontFamily: 'Syne', fontSize: 10, color: 'var(--muted)' }}>Plusvalenza</div>
          <div style={{ fontFamily: 'JetBrains Mono', fontSize: 13, color: 'var(--text)' }}>{formatNumber(sale.capitalGain, 0)} €</div>
        </div>
        <div>
          <div style={{ fontFamily: 'Syne', fontSize: 10, color: 'var(--muted)' }}>Imposta plusvalenza</div>
          <div style={{ fontFamily: 'JetBrains Mono', fontSize: 13, color: sale.capitalGainsTaxable ? 'var(--red)' : 'var(--text)' }}>
            {formatNumber(sale.capitalGainsTax, 0)} €
          </div>
        </div>
        <div>
          <div style={{ fontFamily: 'Syne', fontSize: 10, color: 'var(--muted)' }}>
            Spese agenzia (vendita){sale.saleAgencyFeeMode === 'PERCENTAGE' && ` (${formatNumber(sale.saleAgencyFeesBase, 0)}€ + IVA ${formatNumber(sale.saleAgencyFeesIva, 0)}€)`}
          </div>
          <div style={{ fontFamily: 'JetBrains Mono', fontSize: 13, color: 'var(--text)' }}>
            {formatNumber(sale.saleAgencyFees, 0)} € {sale.saleAgencyFeesEstimated && <span style={{ color: 'var(--muted)', fontSize: 9 }}>(stimato)</span>}
          </div>
        </div>
        <div>
          <div style={{ fontFamily: 'Syne', fontSize: 10, color: 'var(--muted)' }}>Mutuo residuo da estinguere</div>
          <div style={{ fontFamily: 'JetBrains Mono', fontSize: 13, color: 'var(--text)' }}>{formatNumber(sale.residualMortgageBalance, 0)} €</div>
        </div>
        <div>
          <div style={{ fontFamily: 'Syne', fontSize: 10, color: 'var(--muted)' }}>Capitale netto disponibile</div>
          <div style={{ fontFamily: 'JetBrains Mono', fontSize: 14, fontWeight: 700, color: sale.netProceeds > 0 ? 'var(--acc)' : 'var(--red)' }}>
            {formatNumber(sale.netProceeds, 0)} €
          </div>
        </div>
      </div>
      <div style={{ fontFamily: 'Syne', fontSize: 11, color: 'var(--muted2)', lineHeight: 1.4 }}>{sale.capitalGainsNote}</div>
      {sale.timingNote && (
        <div style={{ fontFamily: 'Syne', fontSize: 11, color: 'var(--acc3)', lineHeight: 1.4, marginTop: 6 }}>⏱ {sale.timingNote}</div>
      )}

      {sale.mustFullyFundPurchase && (
        <div style={{
          marginTop: 10, padding: '8px 10px', borderRadius: 8,
          background: sale.coversFullPurchase ? 'rgba(110,231,183,0.12)' : 'rgba(239,68,68,0.15)',
          border: `1px solid ${sale.coversFullPurchase ? 'var(--acc)' : 'var(--red)'}`,
        }}>
          <div style={{ fontFamily: 'Syne', fontWeight: 700, fontSize: 12, color: sale.coversFullPurchase ? 'var(--acc)' : 'var(--red)', marginBottom: 4 }}>
            {sale.coversFullPurchase ? '✅ Copre anticipo + spese' : '⚠ Non copre anticipo + spese'}
            {' — '}{sale.fundingGapOrSurplus >= 0 ? 'surplus di' : 'mancano'} {formatNumber(Math.abs(sale.fundingGapOrSurplus), 0)} €
          </div>
          {sale.fullFundingNote && (
            <div style={{ fontFamily: 'Syne', fontSize: 11, color: 'var(--text)', lineHeight: 1.5 }}>{sale.fullFundingNote}</div>
          )}
        </div>
      )}
    </div>
  )
}

function LiquiditySourcesCard({ result }: { result: MortgageSimulation }) {
  return (
    <div style={{ marginTop: 14, background: 'var(--s3)', borderRadius: 10, padding: '12px 14px' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', flexWrap: 'wrap', gap: 12, marginBottom: 10 }}>
        <div>
          <div style={{ fontFamily: 'Syne', fontSize: 11, color: 'var(--muted)', marginBottom: 4 }}>
            Liquidità disponibile {result.liquidSavingsSource === 'PROFILE' && '(dal questionario Finanza Personale)'}
          </div>
          <div style={{ fontFamily: 'JetBrains Mono', fontSize: 16, color: 'var(--text)' }}>
            {formatNumber(result.availableLiquidSavings, 0)} €
          </div>
        </div>
        <div>
          <div style={{ fontFamily: 'Syne', fontSize: 11, color: 'var(--muted)', marginBottom: 4 }}>Capitale totale disponibile</div>
          <div style={{ fontFamily: 'JetBrains Mono', fontSize: 16, color: 'var(--text)' }}>
            {formatNumber(result.totalAvailableCapital, 0)} €
          </div>
        </div>
        <div>
          <div style={{ fontFamily: 'Syne', fontSize: 11, color: 'var(--muted)', marginBottom: 4 }}>Fabbisogno residuo</div>
          <div style={{ fontFamily: 'JetBrains Mono', fontSize: 16, color: result.shortfall > 0 ? 'var(--red)' : 'var(--acc)' }}>
            {formatNumber(result.shortfall, 0)} €
          </div>
        </div>
      </div>

      {result.homeSale && <HomeSaleCard sale={result.homeSale} />}

      {result.pensionFund && (
        <div style={{
          padding: '10px 12px', borderRadius: 8, marginBottom: 10,
          background: result.pensionFund.eligibleForHomePurchase ? 'rgba(110,231,183,0.08)' : 'rgba(245,158,11,0.1)',
          border: `1px solid ${result.pensionFund.eligibleForHomePurchase ? 'var(--acc)' : 'var(--acc3)'}`,
          fontFamily: 'Syne', fontSize: 12, color: 'var(--text)', lineHeight: 1.5,
        }}>
          <span style={{ fontWeight: 700 }}>
            🏦 Fondo pensione ({result.pensionFund.yearsEnrolled} {result.pensionFund.yearsEnrolled === 1 ? 'anno' : 'anni'} di iscrizione):{' '}
          </span>
          {result.pensionFund.note}
        </div>
      )}

      <div style={{ fontFamily: 'Syne', fontWeight: 700, fontSize: 11, color: 'var(--muted2)', marginBottom: 6 }}>
        Da dove attingere
      </div>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
        {result.budgetAdvice.map((a, i) => (
          <div key={i} style={{ padding: '8px 10px', background: 'var(--s2)', borderRadius: 8 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', gap: 8 }}>
              <span style={{ fontFamily: 'Syne', fontWeight: 700, fontSize: 12, color: 'var(--text)' }}>{a.source}</span>
              {a.amount != null && (
                <span style={{ fontFamily: 'JetBrains Mono', fontSize: 12, color: 'var(--acc)' }}>{formatNumber(a.amount, 0)} €</span>
              )}
            </div>
            <div style={{ fontFamily: 'Syne', fontSize: 11, color: 'var(--muted2)', marginTop: 2, lineHeight: 1.4 }}>{a.message}</div>
          </div>
        ))}
      </div>
    </div>
  )
}

function bindingConstraintLabel(constraint: string): string {
  return constraint === 'REDDITO' ? 'reddito e altri debiti' : 'limite LTV (80% del valore immobile)'
}

function MaxLoanAdviceCard({ advice }: { advice: MortgageSimulation['maxLoanAdvice'] }) {
  const requestedOverMax = advice.requestedLoanAmount > advice.recommendedMaxLoan
  return (
    <div style={{ background: 'var(--s2)', border: '1px solid var(--acc)', borderRadius: 12, padding: '16px 20px', marginTop: 16 }}>
      <div style={{ fontFamily: 'Syne', fontWeight: 700, fontSize: 14, color: 'var(--text)', marginBottom: 4 }}>
        Quanto mutuo potresti richiedere
      </div>
      <div style={{ fontFamily: 'Syne', fontSize: 12, color: 'var(--muted)', marginBottom: 14 }}>
        Calcolato al contrario dal tuo reddito, dagli altri debiti già in essere e dal vincolo di LTV che le banche applicano.
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(140px, 1fr))', gap: 10, marginBottom: 14 }}>
        <div style={{ background: 'var(--s3)', border: '1px solid var(--border)', borderRadius: 10, padding: '10px 12px', textAlign: 'center' }}>
          <div style={{ fontFamily: 'Syne', fontSize: 10, color: 'var(--muted)', marginBottom: 4 }}>Max prudente (30%)</div>
          <div style={{ fontFamily: 'JetBrains Mono', fontSize: 15, color: 'var(--text)' }}>{formatNumber(advice.maxLoanComfortable, 0)} €</div>
        </div>
        <div style={{ background: 'var(--s3)', border: '1px solid var(--border)', borderRadius: 10, padding: '10px 12px', textAlign: 'center' }}>
          <div style={{ fontFamily: 'Syne', fontSize: 10, color: 'var(--muted)', marginBottom: 4 }}>Max soglia limite (35%)</div>
          <div style={{ fontFamily: 'JetBrains Mono', fontSize: 15, color: 'var(--text)' }}>{formatNumber(advice.maxLoanAtLimit, 0)} €</div>
        </div>
        <div style={{ background: 'var(--s3)', border: '1px solid var(--border)', borderRadius: 10, padding: '10px 12px', textAlign: 'center' }}>
          <div style={{ fontFamily: 'Syne', fontSize: 10, color: 'var(--muted)', marginBottom: 4 }}>Max per LTV (80%)</div>
          <div style={{ fontFamily: 'JetBrains Mono', fontSize: 15, color: 'var(--text)' }}>{formatNumber(advice.maxLoanByLtv, 0)} €</div>
        </div>
        <div style={{ background: 'rgba(110,231,183,0.08)', border: '1px solid var(--acc)', borderRadius: 10, padding: '10px 12px', textAlign: 'center' }}>
          <div style={{ fontFamily: 'Syne', fontSize: 10, color: 'var(--muted)', marginBottom: 4 }}>Massimo consigliato</div>
          <div style={{ fontFamily: 'Instrument Serif', fontSize: 18, color: 'var(--acc)' }}>{formatNumber(advice.recommendedMaxLoan, 0)} €</div>
        </div>
      </div>

      <div style={{ fontFamily: 'Syne', fontSize: 11, color: 'var(--muted2)', marginBottom: 10 }}>
        Vincolo più stringente: <strong style={{ color: 'var(--text)' }}>{bindingConstraintLabel(advice.bindingConstraint)}</strong>.
        Con questo mutuo, il capitale proprio sarebbe circa il {formatNumber(advice.equityRatioAtRecommendedPct, 0)}% del valore dell'immobile.
      </div>

      <div style={{
        padding: '10px 12px', borderRadius: 8, marginBottom: 10,
        background: requestedOverMax ? 'rgba(239,68,68,0.1)' : 'rgba(110,231,183,0.08)',
        border: `1px solid ${requestedOverMax ? 'var(--red)' : 'var(--acc)'}`,
        fontFamily: 'Syne', fontSize: 12, color: requestedOverMax ? 'var(--red)' : 'var(--text)', lineHeight: 1.5,
      }}>
        {advice.requestedLoanNote}
      </div>

      <div style={{ padding: '10px 12px', borderRadius: 8, background: 'var(--s3)', fontFamily: 'Syne', fontSize: 12, color: 'var(--muted2)', lineHeight: 1.5 }}>
        <span style={{ fontWeight: 700, color: 'var(--text)' }}>Mutuo minimo necessario dato il tuo capitale: </span>
        {formatNumber(advice.minLoanNeededGivenCapital, 0)} €.{' '}
        {advice.minLoanNeededGivenCapital === 0
          ? 'Il capitale disponibile copre già interamente prezzo e spese accessorie.'
          : advice.minLoanNeededGivenCapital < advice.requestedLoanAmount
          ? 'Potresti ridurre l\'importo richiesto usando più capitale proprio, risparmiando sugli interessi totali.'
          : 'Il capitale disponibile non basta a mantenere il mutuo entro il massimo consigliato: valuta più risparmio o un immobile di valore inferiore.'}
      </div>
    </div>
  )
}

function DurationComparisonTable({ options }: { options: DurationOption[] }) {
  return (
    <div style={{ background: 'var(--s2)', border: '1px solid var(--border)', borderRadius: 12, padding: '16px 20px', marginTop: 16 }}>
      <div style={{ fontFamily: 'Syne', fontWeight: 700, fontSize: 14, color: 'var(--text)', marginBottom: 4 }}>
        Rata più bassa o meno interessi? Confronto tra durate
      </div>
      <div style={{ fontFamily: 'Syne', fontSize: 12, color: 'var(--muted)', marginBottom: 14 }}>
        Stesso importo di mutuo simulato con durate diverse: una durata più lunga abbassa la rata ma aumenta il costo totale degli interessi.
      </div>
      <div style={{ background: 'var(--s3)', border: '1px solid var(--border)', borderRadius: 10, overflow: 'hidden' }}>
        <div style={{
          display: 'grid', gridTemplateColumns: '60px 1fr 1fr 1fr 1fr', gap: 8, padding: '8px 12px',
          fontFamily: 'Syne', fontWeight: 700, fontSize: 10, color: 'var(--muted)', borderBottom: '1px solid var(--border)',
        }}>
          <span>Anni</span>
          <span style={{ textAlign: 'right' }}>Rata</span>
          <span style={{ textAlign: 'right' }}>Interessi totali</span>
          <span style={{ textAlign: 'right' }}>Rata/reddito</span>
          <span style={{ textAlign: 'right' }}>Sostenibilità</span>
        </div>
        {options.map(o => (
          <div key={o.years} style={{
            display: 'grid', gridTemplateColumns: '60px 1fr 1fr 1fr 1fr', gap: 8, padding: '8px 12px',
            fontFamily: 'JetBrains Mono', fontSize: 12, color: 'var(--text)', borderBottom: '1px solid var(--border)',
            background: o.isSelected ? 'rgba(110,231,183,0.08)' : 'transparent',
          }}>
            <span style={{ fontWeight: o.isSelected ? 700 : 400 }}>{o.years} {o.isSelected && '←'}</span>
            <span style={{ textAlign: 'right' }}>{formatNumber(o.monthlyPayment, 0)} €</span>
            <span style={{ textAlign: 'right', color: 'var(--muted2)' }}>{formatNumber(o.totalInterest, 0)} €</span>
            <span style={{ textAlign: 'right' }}>{formatNumber(o.combinedPaymentToIncomeRatioPct, 1)}%</span>
            <span style={{ textAlign: 'right', fontFamily: 'Syne', fontWeight: 700, fontSize: 11, color: affordabilityColor(o.affordabilityLabel) }}>
              {o.affordabilityLabel}
            </span>
          </div>
        ))}
      </div>
    </div>
  )
}

function FullOverviewCard({ result }: { result: MortgageSimulation }) {
  const totalOperationCost = result.totalOutOfPocketCost + result.totalPaid
  const capitalGapOrSurplus = result.totalAvailableCapital - result.totalOutOfPocketCost
  const advice = result.maxLoanAdvice
  const requestedOverMax = advice.requestedLoanAmount > advice.recommendedMaxLoan

  const warnings = [
    result.ltvWarning,
    result.affordabilityWarning,
    result.stressTestWarning,
    requestedOverMax ? advice.requestedLoanNote : null,
    result.pensionFund && !result.pensionFund.eligibleForHomePurchase ? result.pensionFund.note : null,
    result.homeSale?.fullFundingNote ?? null,
    result.homeSale?.timingNote ?? null,
  ].filter((w): w is string => !!w)

  return (
    <div style={{ background: 'var(--s2)', border: '2px solid var(--acc)', borderRadius: 12, padding: '18px 20px', marginTop: 16 }}>
      <div style={{ fontFamily: 'Syne', fontWeight: 700, fontSize: 15, color: 'var(--text)', marginBottom: 4 }}>
        📋 Prospetto completo
      </div>
      <div style={{ fontFamily: 'Syne', fontSize: 12, color: 'var(--muted)', marginBottom: 14 }}>
        Tutti i numeri chiave dell'operazione in un unico colpo d'occhio.
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(160px, 1fr))', gap: 10, marginBottom: 14 }}>
        <div style={{ background: 'var(--s3)', borderRadius: 10, padding: '10px 12px' }}>
          <div style={{ fontFamily: 'Syne', fontSize: 10, color: 'var(--muted)', marginBottom: 4 }}>Rata mensile</div>
          <div style={{ fontFamily: 'JetBrains Mono', fontSize: 15, color: 'var(--text)' }}>{formatNumber(result.monthlyPayment, 0)} €</div>
          <div style={{ fontFamily: 'Syne', fontWeight: 700, fontSize: 10, color: affordabilityColor(result.affordabilityLabel), marginTop: 2 }}>
            {result.affordabilityLabel} ({formatNumber(result.combinedPaymentToIncomeRatioPct, 1)}%)
          </div>
        </div>
        <div style={{ background: 'var(--s3)', borderRadius: 10, padding: '10px 12px' }}>
          <div style={{ fontFamily: 'Syne', fontSize: 10, color: 'var(--muted)', marginBottom: 4 }}>Costo totale operazione</div>
          <div style={{ fontFamily: 'JetBrains Mono', fontSize: 15, color: 'var(--text)' }}>{formatNumber(totalOperationCost, 0)} €</div>
          <div style={{ fontFamily: 'Syne', fontSize: 9, color: 'var(--muted)', marginTop: 2 }}>prezzo + interessi + spese accessorie</div>
        </div>
        <div style={{ background: 'var(--s3)', borderRadius: 10, padding: '10px 12px' }}>
          <div style={{ fontFamily: 'Syne', fontSize: 10, color: 'var(--muted)', marginBottom: 4 }}>Da pagare oltre al mutuo</div>
          <div style={{ fontFamily: 'JetBrains Mono', fontSize: 15, color: 'var(--text)' }}>{formatNumber(result.totalOutOfPocketCost, 0)} €</div>
          <div style={{ fontFamily: 'Syne', fontSize: 9, color: 'var(--muted)', marginTop: 2 }}>capitale proprio + spese accessorie</div>
        </div>
        <div style={{ background: 'var(--s3)', borderRadius: 10, padding: '10px 12px' }}>
          <div style={{ fontFamily: 'Syne', fontSize: 10, color: 'var(--muted)', marginBottom: 4 }}>Capitale disponibile</div>
          <div style={{ fontFamily: 'JetBrains Mono', fontSize: 15, color: 'var(--text)' }}>{formatNumber(result.totalAvailableCapital, 0)} €</div>
          <div style={{ fontFamily: 'Syne', fontSize: 9, color: 'var(--muted)', marginTop: 2 }}>liquidità + vendita netta</div>
        </div>
        <div style={{
          background: capitalGapOrSurplus >= 0 ? 'rgba(110,231,183,0.1)' : 'rgba(239,68,68,0.1)',
          border: `1px solid ${capitalGapOrSurplus >= 0 ? 'var(--acc)' : 'var(--red)'}`, borderRadius: 10, padding: '10px 12px',
        }}>
          <div style={{ fontFamily: 'Syne', fontSize: 10, color: 'var(--muted)', marginBottom: 4 }}>
            {capitalGapOrSurplus >= 0 ? 'Surplus di capitale' : 'Fabbisogno residuo'}
          </div>
          <div style={{ fontFamily: 'Instrument Serif', fontSize: 16, color: capitalGapOrSurplus >= 0 ? 'var(--acc)' : 'var(--red)' }}>
            {formatNumber(Math.abs(capitalGapOrSurplus), 0)} €
          </div>
        </div>
        <div style={{ background: 'var(--s3)', borderRadius: 10, padding: '10px 12px' }}>
          <div style={{ fontFamily: 'Syne', fontSize: 10, color: 'var(--muted)', marginBottom: 4 }}>Mutuo richiesto vs consigliato</div>
          <div style={{ fontFamily: 'JetBrains Mono', fontSize: 13, color: requestedOverMax ? 'var(--red)' : 'var(--text)' }}>
            {formatNumber(advice.requestedLoanAmount, 0)} € / {formatNumber(advice.recommendedMaxLoan, 0)} €
          </div>
        </div>
        {result.taxDeductions.eligible && (
          <div style={{ background: 'rgba(110,231,183,0.08)', border: '1px solid var(--acc)', borderRadius: 10, padding: '10px 12px' }}>
            <div style={{ fontFamily: 'Syne', fontSize: 10, color: 'var(--muted)', marginBottom: 4 }}>Detrazione fiscale stimata (1° anno)</div>
            <div style={{ fontFamily: 'JetBrains Mono', fontSize: 15, color: 'var(--acc)' }}>
              {formatNumber(result.taxDeductions.estimatedAnnualInterestDeduction + result.taxDeductions.estimatedAgencyFeeDeduction, 0)} €
            </div>
            <div style={{ fontFamily: 'Syne', fontSize: 9, color: 'var(--muted)', marginTop: 2 }}>interessi mutuo (annua) + agenzia (una tantum)</div>
          </div>
        )}
      </div>

      {result.homeSale?.mustFullyFundPurchase && (
        <div style={{
          padding: '10px 12px', borderRadius: 8, marginBottom: 10,
          background: result.homeSale.coversFullPurchase ? 'rgba(110,231,183,0.1)' : 'rgba(239,68,68,0.15)',
          border: `1px solid ${result.homeSale.coversFullPurchase ? 'var(--acc)' : 'var(--red)'}`,
          fontFamily: 'Syne', fontWeight: 700, fontSize: 12,
          color: result.homeSale.coversFullPurchase ? 'var(--acc)' : 'var(--red)',
        }}>
          {result.homeSale.coversFullPurchase
            ? '✅ La vendita, unica fonte dichiarata, copre interamente anticipo e spese.'
            : '⚠ La vendita, unica fonte dichiarata, NON copre da sola anticipo e spese: vedi le alternative nella sezione "Casa da vendere".'}
        </div>
      )}

      {warnings.length > 0 && (
        <div>
          <div style={{ fontFamily: 'Syne', fontWeight: 700, fontSize: 11, color: 'var(--muted2)', marginBottom: 6 }}>
            Punti di attenzione, in ordine
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
            {warnings.map((w, i) => (
              <div key={i} style={{
                padding: '8px 10px', borderRadius: 8, background: 'var(--s3)',
                fontFamily: 'Syne', fontSize: 11, color: 'var(--muted2)', lineHeight: 1.5,
              }}>
                {i + 1}. {w}
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}

function TaxDeductionCard({ tax }: { tax: TaxDeductionAdvice }) {
  return (
    <div style={{
      marginTop: 14, padding: '12px 14px', borderRadius: 10,
      background: tax.eligible ? 'rgba(110,231,183,0.08)' : 'var(--s3)',
      border: `1px solid ${tax.eligible ? 'var(--acc)' : 'var(--border)'}`,
    }}>
      <div style={{ fontFamily: 'Syne', fontWeight: 700, fontSize: 12, color: 'var(--text)', marginBottom: 8 }}>
        🧾 Detrazioni fiscali (dichiarazione dei redditi)
      </div>
      {tax.eligible && (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: 10, marginBottom: 8 }}>
          <div style={{ background: 'var(--s2)', borderRadius: 8, padding: '8px 10px' }}>
            <div style={{ fontFamily: 'Syne', fontSize: 10, color: 'var(--muted)' }}>Detrazione interessi mutuo (19%, primo anno)</div>
            <div style={{ fontFamily: 'JetBrains Mono', fontSize: 14, color: 'var(--acc)' }}>{formatNumber(tax.estimatedAnnualInterestDeduction, 0)} €/anno</div>
            <div style={{ fontFamily: 'Syne', fontSize: 9, color: 'var(--muted)', marginTop: 2 }}>
              su {formatNumber(tax.estimatedFirstYearInterest, 0)} € di interessi (max {formatNumber(tax.maxDeductibleInterestPerYear, 0)} €/anno) — diminuisce negli anni
            </div>
          </div>
          <div style={{ background: 'var(--s2)', borderRadius: 8, padding: '8px 10px' }}>
            <div style={{ fontFamily: 'Syne', fontSize: 10, color: 'var(--muted)' }}>Detrazione spese di agenzia (19%, una tantum)</div>
            <div style={{ fontFamily: 'JetBrains Mono', fontSize: 14, color: 'var(--acc)' }}>{formatNumber(tax.estimatedAgencyFeeDeduction, 0)} €</div>
            <div style={{ fontFamily: 'Syne', fontSize: 9, color: 'var(--muted)', marginTop: 2 }}>
              max {formatNumber(tax.maxDeductibleAgencyFee, 0)} € di spesa detraibile, solo nell'anno di acquisto
            </div>
          </div>
        </div>
      )}
      <div style={{ fontFamily: 'Syne', fontSize: 11, color: 'var(--muted2)', lineHeight: 1.5 }}>{tax.note}</div>
    </div>
  )
}

function ResultCard({ result }: { result: MortgageSimulation }) {
  return (
    <div style={{ background: 'var(--s2)', border: '1px solid var(--acc)', borderRadius: 12, padding: '16px 20px', marginTop: 16 }}>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 12, marginBottom: 14 }}>
        <div style={{ background: 'var(--s3)', border: '1px solid var(--border)', borderRadius: 10, padding: '12px 14px', textAlign: 'center' }}>
          <div style={{ fontFamily: 'Syne', fontSize: 11, color: 'var(--muted)', marginBottom: 6 }}>Rata mensile</div>
          <div style={{ fontFamily: 'Instrument Serif', fontSize: 22, color: 'var(--acc)' }}>{formatNumber(result.monthlyPayment, 2)} €</div>
        </div>
        <div style={{ background: 'var(--s3)', border: '1px solid var(--border)', borderRadius: 10, padding: '12px 14px', textAlign: 'center' }}>
          <div style={{ fontFamily: 'Syne', fontSize: 11, color: 'var(--muted)', marginBottom: 6 }}>Totale interessi</div>
          <div style={{ fontFamily: 'Instrument Serif', fontSize: 22, color: 'var(--text)' }}>{formatNumber(result.totalInterest, 0)} €</div>
        </div>
        <div style={{ background: 'var(--s3)', border: '1px solid var(--border)', borderRadius: 10, padding: '12px 14px', textAlign: 'center' }}>
          <div style={{ fontFamily: 'Syne', fontSize: 11, color: 'var(--muted)', marginBottom: 6 }}>Costo totale mutuo</div>
          <div style={{ fontFamily: 'Instrument Serif', fontSize: 22, color: 'var(--text)' }}>{formatNumber(result.totalPaid, 0)} €</div>
        </div>
      </div>

      <div style={{ display: 'flex', gap: 12, marginBottom: 14, flexWrap: 'wrap' }}>
        <div style={{ flex: '1 1 220px', background: 'var(--s3)', borderRadius: 10, padding: '10px 14px' }}>
          <div style={{ fontFamily: 'Syne', fontSize: 11, color: 'var(--muted)', marginBottom: 4 }}>Loan-to-Value (LTV)</div>
          <div style={{ fontFamily: 'JetBrains Mono', fontSize: 16, color: result.ltvWarning ? 'var(--red)' : 'var(--text)' }}>
            {formatNumber(result.loanToValuePct, 1)}%
          </div>
        </div>
        <div style={{ flex: '1 1 220px', background: 'var(--s3)', borderRadius: 10, padding: '10px 14px' }}>
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
      </div>

      {result.incomeEstimated && (
        <div style={{ fontFamily: 'Syne', fontSize: 11, color: 'var(--muted)', marginBottom: 10 }}>
          Reddito netto mensile stimato dal budget: {formatNumber(result.monthlyNetIncome, 0)} €. Per un calcolo più preciso, inseriscilo manualmente qui sopra.
        </div>
      )}

      {result.ltvWarning && (
        <div style={{
          padding: '10px 12px', borderRadius: 8, background: 'rgba(239,68,68,0.1)',
          border: '1px solid var(--red)', fontFamily: 'Syne', fontSize: 12, color: 'var(--red)', lineHeight: 1.5, marginBottom: 10,
        }}>
          <span style={{ fontWeight: 700 }}>⚠ LTV elevato: </span>{result.ltvWarning}
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

      <div style={{
        padding: '10px 12px', borderRadius: 8, background: 'var(--s3)',
        fontFamily: 'Syne', fontSize: 12, color: 'var(--muted2)', lineHeight: 1.5,
      }}>
        <span style={{ fontWeight: 700, color: 'var(--text)' }}>Stress test tassi: </span>
        se il tasso salisse al {formatNumber(result.stressTestRatePct, 1)}% (+2 punti, scenario rilevante per un mutuo a tasso variabile),
        la rata salirebbe a {formatNumber(result.stressTestMonthlyPayment, 2)} € e il rapporto rata/reddito
        al {formatNumber(result.stressTestCombinedRatioPct, 1)}%.
        {result.stressTestWarning && (
          <div style={{ color: 'var(--red)', marginTop: 6, fontWeight: 600 }}>{result.stressTestWarning}</div>
        )}
      </div>

      <CostBreakdownCard result={result} />
      <TaxDeductionCard tax={result.taxDeductions} />
      <LiquiditySourcesCard result={result} />
      <AmortizationTable schedule={result.schedule} />
    </div>
  )
}

export function MortgageCalculator() {
  const { incomeEstimate, simulateMortgage, mortgageResult, isSimulatingMortgage, mortgageError } = useMortgage()

  const [propertyValue, setPropertyValue] = useState('')
  const [loanAmount, setLoanAmount] = useState('')
  const [interestRatePct, setInterestRatePct] = useState('')
  const [years, setYears] = useState('25')
  const [monthlyNetIncome, setMonthlyNetIncome] = useState('')
  const [purchaseType, setPurchaseType] = useState<PurchaseType>('PRIMA_CASA_PRIVATO')

  const [notaryCosts, setNotaryCosts] = useState('')
  const [originationFees, setOriginationFees] = useState('')
  const [appraisalFees, setAppraisalFees] = useState('')
  const [agencyFeeMode, setAgencyFeeMode] = useState<'PERCENTAGE' | 'AMOUNT'>('PERCENTAGE')
  const [agencyFeePct, setAgencyFeePct] = useState('')
  const [agencyFeeAmount, setAgencyFeeAmount] = useState('')
  const [registrationTax, setRegistrationTax] = useState('')

  const [liquidSavings, setLiquidSavings] = useState('')
  const [pensionFundYears, setPensionFundYears] = useState('')
  const [pensionFundBalance, setPensionFundBalance] = useState('')

  const [hasHomeToSell, setHasHomeToSell] = useState(false)
  const [saleValue, setSaleValue] = useState('')
  const [purchasePrice, setPurchasePrice] = useState('')
  const [yearsOwned, setYearsOwned] = useState('')
  const [mainResidence, setMainResidence] = useState(true)
  const [residualMortgageBalance, setResidualMortgageBalance] = useState('')
  const [saleAgencyFeeMode, setSaleAgencyFeeMode] = useState<'PERCENTAGE' | 'AMOUNT'>('PERCENTAGE')
  const [saleAgencyFeePct, setSaleAgencyFeePct] = useState('')
  const [saleAgencyFeeAmount, setSaleAgencyFeeAmount] = useState('')
  const [monthsUntilSale, setMonthsUntilSale] = useState('')
  const [mustFullyFundPurchase, setMustFullyFundPurchase] = useState(false)

  useEffect(() => {
    if (incomeEstimate?.estimatedMonthlyIncome != null && monthlyNetIncome === '') {
      setMonthlyNetIncome(String(incomeEstimate.estimatedMonthlyIncome))
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [incomeEstimate?.estimatedMonthlyIncome])

  const propertyNum = parseFloat(propertyValue)
  const loanNum = parseFloat(loanAmount)
  const rateNum = parseFloat(interestRatePct)
  const yearsNum = parseInt(years, 10)
  const valid = !isNaN(propertyNum) && propertyNum > 0 && !isNaN(loanNum) && loanNum > 0
    && !isNaN(rateNum) && rateNum > 0 && !isNaN(yearsNum) && yearsNum > 0

  const optionalNumber = (value: string): number | null => {
    if (value.trim() === '') return null
    const parsed = parseFloat(value)
    return isNaN(parsed) ? null : parsed
  }
  const optionalInt = (value: string): number | null => {
    if (value.trim() === '') return null
    const parsed = parseInt(value, 10)
    return isNaN(parsed) ? null : parsed
  }

  const saleValueNum = parseFloat(saleValue)
  const purchasePriceNum = parseFloat(purchasePrice)
  const yearsOwnedNum = parseInt(yearsOwned, 10)
  const homeSaleValid = hasHomeToSell && !isNaN(saleValueNum) && saleValueNum > 0
    && !isNaN(purchasePriceNum) && purchasePriceNum > 0 && !isNaN(yearsOwnedNum) && yearsOwnedNum >= 0

  const handleSimulate = async () => {
    if (!valid) return
    await simulateMortgage({
      propertyValue: propertyNum,
      loanAmount: loanNum,
      interestRatePct: rateNum,
      years: yearsNum,
      monthlyNetIncome: optionalNumber(monthlyNetIncome),
      purchaseType,
      notaryCosts: optionalNumber(notaryCosts),
      originationFees: optionalNumber(originationFees),
      appraisalFees: optionalNumber(appraisalFees),
      agencyFeePct: agencyFeeMode === 'PERCENTAGE' ? optionalNumber(agencyFeePct) : null,
      agencyFeeAmount: agencyFeeMode === 'AMOUNT' ? optionalNumber(agencyFeeAmount) : null,
      registrationTax: optionalNumber(registrationTax),
      liquidSavings: optionalNumber(liquidSavings),
      pensionFundYears: optionalInt(pensionFundYears),
      pensionFundBalance: optionalNumber(pensionFundBalance),
      homeSale: homeSaleValid ? {
        saleValue: saleValueNum,
        purchasePrice: purchasePriceNum,
        yearsOwned: yearsOwnedNum,
        mainResidence,
        residualMortgageBalance: optionalNumber(residualMortgageBalance),
        saleAgencyFeePct: saleAgencyFeeMode === 'PERCENTAGE' ? optionalNumber(saleAgencyFeePct) : null,
        saleAgencyFeeAmount: saleAgencyFeeMode === 'AMOUNT' ? optionalNumber(saleAgencyFeeAmount) : null,
        monthsUntilSale: optionalInt(monthsUntilSale),
        mustFullyFundPurchase,
      } : null,
    })
  }

  return (
    <div style={{ background: 'var(--s2)', border: '1px solid var(--border)', borderRadius: 12, padding: '16px 20px' }}>
      <div style={{ fontFamily: 'Syne', fontWeight: 700, fontSize: 14, color: 'var(--text)', marginBottom: 4 }}>
        Calcolatore mutuo
      </div>
      <div style={{ fontFamily: 'Syne', fontSize: 12, color: 'var(--muted)', marginBottom: 14 }}>
        Inserisci i dati dell'immobile e del mutuo richiesto per calcolare la rata, la sostenibilità e tutto ciò che non rientra nel mutuo.
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(160px, 1fr))', gap: 10, marginBottom: 12 }}>
        <label style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
          <span style={labelStyle}>Importo immobile (€)</span>
          <input value={propertyValue} onChange={e => setPropertyValue(e.target.value)} type="number" placeholder="es. 250000" style={inputStyle} />
        </label>
        <label style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
          <span style={labelStyle}>Importo richiesto mutuo (€)</span>
          <input value={loanAmount} onChange={e => setLoanAmount(e.target.value)} type="number" placeholder="es. 200000" style={inputStyle} />
        </label>
        <label style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
          <span style={labelStyle}>Tasso di interesse annuo (TAN %)</span>
          <input value={interestRatePct} onChange={e => setInterestRatePct(e.target.value)} type="number" step="0.01" placeholder="es. 3.5" style={inputStyle} />
        </label>
        <label style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
          <span style={labelStyle}>Anni a disposizione</span>
          <input value={years} onChange={e => setYears(e.target.value)} type="number" placeholder="es. 25" style={inputStyle} />
        </label>
        <label style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
          <span style={labelStyle}>Reddito netto mensile (€, opzionale)</span>
          <input value={monthlyNetIncome} onChange={e => setMonthlyNetIncome(e.target.value)} type="number" placeholder="stimato dal budget se vuoto" style={inputStyle} />
        </label>
        <label style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
          <span style={labelStyle}>Tipo di acquisto</span>
          <select value={purchaseType} onChange={e => setPurchaseType(e.target.value as PurchaseType)} style={inputStyle}>
            {PURCHASE_TYPE_OPTIONS.map(o => <option key={o.value} value={o.value}>{o.label}</option>)}
          </select>
        </label>
      </div>

      <div style={{ fontFamily: 'Syne', fontWeight: 700, fontSize: 12, color: 'var(--muted2)', margin: '4px 0 8px' }}>
        Spese non coperte dal mutuo <span style={{ fontWeight: 400, color: 'var(--muted)' }}>(opzionali — se vuote, FINAI le stima)</span>
      </div>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(160px, 1fr))', gap: 10, marginBottom: 12 }}>
        <label style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
          <span style={labelStyle}>Notaio (€)</span>
          <input value={notaryCosts} onChange={e => setNotaryCosts(e.target.value)} type="number" placeholder="stima ~2% immobile" style={inputStyle} />
        </label>
        <label style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
          <span style={labelStyle}>Istruttoria bancaria (€)</span>
          <input value={originationFees} onChange={e => setOriginationFees(e.target.value)} type="number" placeholder="stima ~0.5% mutuo" style={inputStyle} />
        </label>
        <label style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
          <span style={labelStyle}>Perizia immobile (€)</span>
          <input value={appraisalFees} onChange={e => setAppraisalFees(e.target.value)} type="number" placeholder="stima ~300€" style={inputStyle} />
        </label>
        <label style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
          <span style={labelStyle}>
            Agenzia immobiliare
            <button
              type="button"
              onClick={() => setAgencyFeeMode(m => m === 'PERCENTAGE' ? 'AMOUNT' : 'PERCENTAGE')}
              style={{
                marginLeft: 6, padding: '1px 8px', borderRadius: 6, border: '1px solid var(--border)',
                background: 'var(--s2)', color: 'var(--acc)', fontFamily: 'Syne', fontSize: 10, fontWeight: 700, cursor: 'pointer',
              }}
            >
              {agencyFeeMode === 'PERCENTAGE' ? '% → passa a €' : '€ → passa a %'}
            </button>
          </span>
          {agencyFeeMode === 'PERCENTAGE' ? (
            <input value={agencyFeePct} onChange={e => setAgencyFeePct(e.target.value)} type="number" step="0.1" placeholder="stima 3% (+ IVA auto)" style={inputStyle} />
          ) : (
            <input value={agencyFeeAmount} onChange={e => setAgencyFeeAmount(e.target.value)} type="number" placeholder="importo finale già con IVA" style={inputStyle} />
          )}
        </label>
        <label style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
          <span style={labelStyle}>Imposta di registro / IVA (€)</span>
          <input value={registrationTax} onChange={e => setRegistrationTax(e.target.value)} type="number" placeholder="stima da tipo acquisto" style={inputStyle} />
        </label>
      </div>

      <div style={{ fontFamily: 'Syne', fontWeight: 700, fontSize: 12, color: 'var(--muted2)', margin: '4px 0 8px' }}>
        A cosa attingere <span style={{ fontWeight: 400, color: 'var(--muted)' }}>(opzionale)</span>
      </div>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(160px, 1fr))', gap: 10, marginBottom: 12 }}>
        <label style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
          <span style={labelStyle}>Liquidità disponibile (€)</span>
          <input value={liquidSavings} onChange={e => setLiquidSavings(e.target.value)} type="number" placeholder="dal questionario se vuoto" style={inputStyle} />
        </label>
        <label style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
          <span style={labelStyle}>Anni iscrizione fondo pensione</span>
          <input value={pensionFundYears} onChange={e => setPensionFundYears(e.target.value)} type="number" placeholder="es. 2" style={inputStyle} />
        </label>
        <label style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
          <span style={labelStyle}>Montante fondo pensione (€)</span>
          <input value={pensionFundBalance} onChange={e => setPensionFundBalance(e.target.value)} type="number" placeholder="opzionale" style={inputStyle} />
        </label>
      </div>

      <label style={{ display: 'flex', alignItems: 'center', gap: 8, margin: '4px 0 8px', cursor: 'pointer' }}>
        <input type="checkbox" checked={hasHomeToSell} onChange={e => setHasHomeToSell(e.target.checked)} />
        <span style={{ fontFamily: 'Syne', fontWeight: 700, fontSize: 12, color: 'var(--muted2)' }}>
          Ho una casa da vendere per finanziare questo acquisto
        </span>
      </label>

      {hasHomeToSell && (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(160px, 1fr))', gap: 10, marginBottom: 12 }}>
          <label style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
            <span style={labelStyle}>Valore di vendita stimato (€)</span>
            <input value={saleValue} onChange={e => setSaleValue(e.target.value)} type="number" placeholder="es. 250000" style={inputStyle} />
          </label>
          <label style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
            <span style={labelStyle}>Prezzo di acquisto originario (€)</span>
            <input value={purchasePrice} onChange={e => setPurchasePrice(e.target.value)} type="number" placeholder="es. 180000" style={inputStyle} />
          </label>
          <label style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
            <span style={labelStyle}>Anni di possesso</span>
            <input value={yearsOwned} onChange={e => setYearsOwned(e.target.value)} type="number" placeholder="es. 6" style={inputStyle} />
          </label>
          <label style={{ display: 'flex', alignItems: 'center', gap: 8, paddingTop: 18 }}>
            <input type="checkbox" checked={mainResidence} onChange={e => setMainResidence(e.target.checked)} />
            <span style={labelStyle}>È stata la tua abitazione principale</span>
          </label>
          <label style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
            <span style={labelStyle}>Mutuo/finanziamento residuo (€)</span>
            <input value={residualMortgageBalance} onChange={e => setResidualMortgageBalance(e.target.value)} type="number" placeholder="0 se nessuno" style={inputStyle} />
          </label>
          <label style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
            <span style={labelStyle}>
              Spese agenzia per la vendita
              <button
                type="button"
                onClick={() => setSaleAgencyFeeMode(m => m === 'PERCENTAGE' ? 'AMOUNT' : 'PERCENTAGE')}
                style={{
                  marginLeft: 6, padding: '1px 8px', borderRadius: 6, border: '1px solid var(--border)',
                  background: 'var(--s2)', color: 'var(--acc)', fontFamily: 'Syne', fontSize: 10, fontWeight: 700, cursor: 'pointer',
                }}
              >
                {saleAgencyFeeMode === 'PERCENTAGE' ? '% → passa a €' : '€ → passa a %'}
              </button>
            </span>
            {saleAgencyFeeMode === 'PERCENTAGE' ? (
              <input value={saleAgencyFeePct} onChange={e => setSaleAgencyFeePct(e.target.value)} type="number" step="0.1" placeholder="stima 3% (+ IVA auto)" style={inputStyle} />
            ) : (
              <input value={saleAgencyFeeAmount} onChange={e => setSaleAgencyFeeAmount(e.target.value)} type="number" placeholder="importo finale già con IVA" style={inputStyle} />
            )}
          </label>
          <label style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
            <span style={labelStyle}>Tra quanti mesi prevedi di venderla</span>
            <input value={monthsUntilSale} onChange={e => setMonthsUntilSale(e.target.value)} type="number" placeholder="opzionale" style={inputStyle} />
          </label>
          <label style={{
            display: 'flex', alignItems: 'flex-start', gap: 8, gridColumn: '1 / -1',
            padding: '8px 10px', borderRadius: 8, background: 'rgba(239,68,68,0.06)', border: '1px solid var(--border)', cursor: 'pointer',
          }}>
            <input type="checkbox" checked={mustFullyFundPurchase} onChange={e => setMustFullyFundPurchase(e.target.checked)} style={{ marginTop: 2 }} />
            <span style={{ fontFamily: 'Syne', fontSize: 12, color: 'var(--text)', lineHeight: 1.4 }}>
              Questi soldi devono coprire <strong>tutto</strong> — capitale proprio (anticipo) + spese accessorie — perché non ho altra liquidità a disposizione
            </span>
          </label>
        </div>
      )}

      {mortgageError && (
        <div style={{ color: 'var(--red)', fontFamily: 'Syne', fontSize: 12, marginBottom: 10 }}>
          {(mortgageError as any)?.response?.data?.error || 'Impossibile calcolare la simulazione'}
        </div>
      )}

      <button
        onClick={handleSimulate}
        disabled={!valid || isSimulatingMortgage}
        style={{
          padding: '10px 20px', borderRadius: 8, background: 'var(--acc)', border: 'none',
          color: '#07080a', fontFamily: 'Syne', fontWeight: 700, fontSize: 13,
          cursor: valid ? 'pointer' : 'default', opacity: valid ? 1 : 0.5,
        }}
      >
        {isSimulatingMortgage ? 'Calcolo…' : 'Calcola rata mutuo'}
      </button>

      {mortgageResult && (
        <>
          <FullOverviewCard result={mortgageResult} />
          <ResultCard result={mortgageResult} />
          <MaxLoanAdviceCard advice={mortgageResult.maxLoanAdvice} />
          <DurationComparisonTable options={mortgageResult.durationComparison} />
        </>
      )}
    </div>
  )
}
