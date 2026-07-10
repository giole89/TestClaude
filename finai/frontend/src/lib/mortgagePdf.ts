import jsPDF from 'jspdf'
import autoTable from 'jspdf-autotable'
import { MortgageSimulation, PurchaseType } from '@/hooks/useMortgage'
import { formatNumber } from '@/lib/formatters'

const PURCHASE_TYPE_LABELS: Record<PurchaseType, string> = {
  PRIMA_CASA_PRIVATO: 'Prima casa — da privato',
  PRIMA_CASA_COSTRUTTORE: 'Prima casa — da costruttore',
  SECONDA_CASA_PRIVATO: 'Seconda casa — da privato',
  SECONDA_CASA_COSTRUTTORE: 'Seconda casa — da costruttore',
}

const ACCENT: [number, number, number] = [16, 163, 127]
const TEXT_DARK: [number, number, number] = [30, 30, 30]
const MUTED: [number, number, number] = [110, 110, 110]
const RED: [number, number, number] = [200, 50, 50]
const BORDER: [number, number, number] = [225, 225, 225]

const euro = (n: number) => `${formatNumber(n, 0)} €`

interface MortgagePdfMeta {
  propertyValue: number
  loanAmount: number
  interestRatePct: number
  years: number
  purchaseType: PurchaseType
}

export function downloadMortgagePdf(result: MortgageSimulation, meta: MortgagePdfMeta) {
  const doc = new jsPDF({ unit: 'mm', format: 'a4' })
  const pageWidth = doc.internal.pageSize.getWidth()
  const marginX = 14
  let y = 18

  // ── Header ──────────────────────────────────────────────
  doc.setFont('helvetica', 'bold')
  doc.setFontSize(18)
  doc.setTextColor(...TEXT_DARK)
  doc.text('Prospetto Mutuo', marginX, y)

  doc.setFont('helvetica', 'normal')
  doc.setFontSize(9)
  doc.setTextColor(...MUTED)
  const generatedOn = new Date().toLocaleDateString('it-IT', { day: '2-digit', month: 'long', year: 'numeric' })
  doc.text(`Generato il ${generatedOn} — FINAI`, pageWidth - marginX, y - 1, { align: 'right' })

  y += 5
  doc.setDrawColor(...ACCENT)
  doc.setLineWidth(0.8)
  doc.line(marginX, y, pageWidth - marginX, y)
  y += 6

  doc.setFontSize(10)
  doc.setTextColor(...TEXT_DARK)
  doc.text(
    `Immobile ${euro(meta.propertyValue)}  ·  Mutuo richiesto ${euro(meta.loanAmount)}  ·  TAN ${formatNumber(meta.interestRatePct, 2)}%  ·  ${meta.years} anni  ·  ${PURCHASE_TYPE_LABELS[meta.purchaseType]}`,
    marginX, y
  )
  y += 8

  const sectionTitle = (title: string) => {
    doc.setFont('helvetica', 'bold')
    doc.setFontSize(12)
    doc.setTextColor(...TEXT_DARK)
    doc.text(title, marginX, y)
    y += 5
  }

  const table = (rows: Array<[string, string]>, highlightLastRow = false) => {
    autoTable(doc, {
      startY: y,
      margin: { left: marginX, right: marginX },
      theme: 'plain',
      styles: { font: 'helvetica', fontSize: 10, textColor: TEXT_DARK, cellPadding: { top: 2, bottom: 2, left: 0, right: 0 } },
      columnStyles: { 0: { cellWidth: 110 }, 1: { cellWidth: 'auto', halign: 'right' } },
      body: rows,
      didParseCell: (data) => {
        if (data.section === 'body') {
          data.cell.styles.lineWidth = { top: 0, right: 0, bottom: 0.15, left: 0 } as any
          data.cell.styles.lineColor = BORDER as any
          if (highlightLastRow && data.row.index === rows.length - 1) {
            data.cell.styles.fontStyle = 'bold'
            data.cell.styles.textColor = ACCENT as any
          }
        }
      },
    })
    y = (doc as any).lastAutoTable.finalY + 8
  }

  const ensureSpace = (needed: number) => {
    const pageHeight = doc.internal.pageSize.getHeight()
    if (y + needed > pageHeight - 20) {
      doc.addPage()
      y = 18
    }
  }

  // ── 1. Sintesi ──────────────────────────────────────────
  ensureSpace(40)
  sectionTitle('1. Sintesi')
  table([
    ['Rata mensile', euro(result.monthlyPayment)],
    ['Sostenibilità (rata/reddito)', `${result.affordabilityLabel} (${formatNumber(result.combinedPaymentToIncomeRatioPct, 1)}%)`],
    ['Loan-to-Value (LTV)', `${formatNumber(result.loanToValuePct, 1)}%`],
    ['Interessi totali sul mutuo', euro(result.totalInterest)],
    ['Costo totale del mutuo (capitale + interessi)', euro(result.totalPaid)],
  ])

  // ── 2. Costo totale dell'operazione ─────────────────────
  ensureSpace(60)
  sectionTitle("2. Costo totale dell'operazione")
  table([
    ['Capitale proprio (anticipo)', euro(result.downPayment)],
    ['Notaio', euro(result.notaryCosts)],
    ['Istruttoria bancaria', euro(result.originationFees)],
    ['Perizia immobile', euro(result.appraisalFees)],
    ['Agenzia immobiliare', euro(result.agencyFees)],
    ['Imposta di registro / IVA', euro(result.registrationTax)],
    ['Totale da pagare oltre al mutuo', euro(result.totalOutOfPocketCost)],
    ['Costo totale operazione (prezzo + interessi + spese)', euro(result.totalOutOfPocketCost + result.totalPaid)],
  ], true)

  // ── 3. Capitale disponibile ──────────────────────────────
  ensureSpace(50)
  sectionTitle('3. Capitale disponibile')
  const capitalRows: Array<[string, string]> = [
    ['Liquidità disponibile', euro(result.availableLiquidSavings)],
  ]
  if (result.homeSale) {
    capitalRows.push(['Guadagno dalla vendita (plusvalenza)', euro(result.homeSale.capitalGain)])
    capitalRows.push(['Netto dalla vendita, tolte le spese', euro(result.homeSale.netProceeds)])
  }
  capitalRows.push(['Capitale totale disponibile', euro(result.totalAvailableCapital)])
  capitalRows.push([result.shortfall > 0 ? 'Fabbisogno residuo' : 'Surplus di capitale',
    euro(result.shortfall > 0 ? result.shortfall : result.totalAvailableCapital - result.totalOutOfPocketCost)])
  table(capitalRows, true)

  // ── 4. Quanto mutuo potresti richiedere ─────────────────
  ensureSpace(50)
  sectionTitle('4. Quanto mutuo potresti richiedere')
  table([
    ['Massimo prudente (30% del reddito)', euro(result.maxLoanAdvice.maxLoanComfortable)],
    ['Massimo a soglia limite (35% del reddito)', euro(result.maxLoanAdvice.maxLoanAtLimit)],
    ['Massimo per LTV (80% del valore immobile)', euro(result.maxLoanAdvice.maxLoanByLtv)],
    ['Mutuo massimo consigliato', euro(result.maxLoanAdvice.recommendedMaxLoan)],
    ['Mutuo richiesto in questa simulazione', euro(result.maxLoanAdvice.requestedLoanAmount)],
  ], true)

  // ── 5. Detrazioni fiscali ────────────────────────────────
  if (result.taxDeductions.eligible) {
    ensureSpace(30)
    sectionTitle('5. Detrazioni fiscali (dichiarazione dei redditi)')
    table([
      ['Detrazione interessi mutuo (19%, primo anno)', `${euro(result.taxDeductions.estimatedAnnualInterestDeduction)}/anno`],
      ['Detrazione spese di agenzia (19%, una tantum)', euro(result.taxDeductions.estimatedAgencyFeeDeduction)],
    ])
  }

  // ── 6. Punti di attenzione ───────────────────────────────
  const warnings = [
    result.ltvWarning,
    result.affordabilityWarning,
    result.stressTestWarning,
    result.maxLoanAdvice.requestedLoanAmount > result.maxLoanAdvice.recommendedMaxLoan ? result.maxLoanAdvice.requestedLoanNote : null,
    result.pensionFund && !result.pensionFund.eligibleForHomePurchase ? result.pensionFund.note : null,
    result.homeSale?.fullFundingNote ?? null,
    result.homeSale?.timingNote ?? null,
  ].filter((w): w is string => !!w)

  if (warnings.length > 0) {
    ensureSpace(20 + warnings.length * 10)
    sectionTitle(`${result.taxDeductions.eligible ? '6' : '5'}. Punti di attenzione`)
    doc.setFont('helvetica', 'normal')
    doc.setFontSize(9)
    warnings.forEach((w, i) => {
      const lines = doc.splitTextToSize(`${i + 1}. ${w}`, pageWidth - marginX * 2)
      ensureSpace(lines.length * 4.5 + 3)
      doc.setTextColor(...RED)
      doc.text(lines, marginX, y)
      y += lines.length * 4.5 + 3
    })
    y += 3
  }

  // ── Footer disclaimer on every page ─────────────────────
  const pageCount = doc.getNumberOfPages()
  for (let i = 1; i <= pageCount; i++) {
    doc.setPage(i)
    const pageHeight = doc.internal.pageSize.getHeight()
    doc.setFont('helvetica', 'italic')
    doc.setFontSize(7.5)
    doc.setTextColor(...MUTED)
    doc.text(
      'Stime indicative basate sui dati inseriti. Non costituiscono consulenza finanziaria, fiscale o legale: verifica sempre le cifre esatte con notaio, banca e commercialista.',
      marginX, pageHeight - 12, { maxWidth: pageWidth - marginX * 2 }
    )
    doc.text(`Pagina ${i} di ${pageCount}`, pageWidth - marginX, pageHeight - 12, { align: 'right' })
  }

  const dateSlug = new Date().toISOString().slice(0, 10)
  doc.save(`mutuo-prospetto-${dateSlug}.pdf`)
}
