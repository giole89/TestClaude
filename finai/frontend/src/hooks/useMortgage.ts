import { useQuery, useMutation } from '@tanstack/react-query'
import axios from 'axios'
import { API_BASE } from '@/lib/constants'

export interface AmortizationYear {
  year: number
  principalPaid: number
  interestPaid: number
  remainingBalance: number
}

export type PurchaseType = 'PRIMA_CASA_PRIVATO' | 'PRIMA_CASA_COSTRUTTORE' | 'SECONDA_CASA_PRIVATO' | 'SECONDA_CASA_COSTRUTTORE'

export interface HomeSaleInput {
  saleValue: number
  purchasePrice: number
  yearsOwned: number
  mainResidence?: boolean | null
  residualMortgageBalance?: number | null
  saleAgencyFees?: number | null
  monthsUntilSale?: number | null
}

export interface MortgageInput {
  propertyValue: number
  loanAmount: number
  interestRatePct: number
  years: number
  monthlyNetIncome?: number | null
  purchaseType?: PurchaseType | null
  notaryCosts?: number | null
  originationFees?: number | null
  appraisalFees?: number | null
  agencyFeePct?: number | null
  agencyFeeAmount?: number | null
  registrationTax?: number | null
  liquidSavings?: number | null
  pensionFundYears?: number | null
  pensionFundBalance?: number | null
  homeSale?: HomeSaleInput | null
}

export interface PensionFundAdvice {
  yearsEnrolled: number
  eligibleForHomePurchase: boolean
  yearsUntilEligible: number
  maxAnticipationPct: number | null
  estimatedMaxAnticipation: number | null
  note: string
}

export interface BudgetAdvice {
  source: string
  message: string
  amount: number | null
}

export interface HomeSaleAdvice {
  capitalGain: number
  capitalGainsTaxable: boolean
  capitalGainsTax: number
  capitalGainsNote: string
  saleAgencyFees: number
  saleAgencyFeesEstimated: boolean
  residualMortgageBalance: number
  netProceeds: number
  monthsUntilSale: number | null
  timingNote: string | null
  summary: string
}

export interface MortgageSimulation {
  monthlyPayment: number
  totalPaid: number
  totalInterest: number
  loanToValuePct: number
  ltvWarning: string | null
  monthlyNetIncome: number
  incomeEstimated: boolean
  otherActiveDebtPayments: number
  paymentToIncomeRatioPct: number
  combinedPaymentToIncomeRatioPct: number
  affordabilityLabel: 'Sostenibile' | 'Al limite' | 'Rischioso'
  affordabilityWarning: string | null
  stressTestRatePct: number
  stressTestMonthlyPayment: number
  stressTestCombinedRatioPct: number
  stressTestWarning: string | null
  downPayment: number
  notaryCosts: number
  notaryCostsEstimated: boolean
  originationFees: number
  originationFeesEstimated: boolean
  appraisalFees: number
  appraisalFeesEstimated: boolean
  agencyFees: number
  agencyFeesEstimated: boolean
  agencyFeesBase: number
  agencyFeesIva: number
  agencyFeeMode: 'PERCENTAGE' | 'AMOUNT'
  registrationTax: number
  registrationTaxEstimated: boolean
  registrationTaxNote: string
  totalAncillaryCosts: number
  totalOutOfPocketCost: number
  availableLiquidSavings: number
  liquidSavingsSource: 'DECLARED' | 'PROFILE' | 'NONE'
  homeSale: HomeSaleAdvice | null
  totalAvailableCapital: number
  shortfall: number
  pensionFund: PensionFundAdvice | null
  budgetAdvice: BudgetAdvice[]
  schedule: AmortizationYear[]
}

export interface LoanInput {
  loanAmount: number
  interestRatePct: number
  months: number
  monthlyNetIncome?: number | null
}

export interface LoanSimulation {
  monthlyPayment: number
  totalPaid: number
  totalInterest: number
  monthlyNetIncome: number
  incomeEstimated: boolean
  otherActiveDebtPayments: number
  paymentToIncomeRatioPct: number
  combinedPaymentToIncomeRatioPct: number
  affordabilityLabel: 'Sostenibile' | 'Al limite' | 'Rischioso'
  affordabilityWarning: string | null
  schedule: AmortizationYear[]
}

export interface IncomeEstimate {
  estimatedMonthlyIncome: number | null
  hasEnoughData: boolean
}

const INCOME_ESTIMATE_KEY = ['mortgageIncomeEstimate']

export function useMortgage() {
  const incomeEstimate = useQuery<IncomeEstimate>({
    queryKey: INCOME_ESTIMATE_KEY,
    queryFn: () => axios.get(`${API_BASE}/api/finance/mortgage/income-estimate`).then(r => r.data),
    staleTime: 60_000,
  })

  const simulateMortgage = useMutation({
    mutationFn: (req: MortgageInput) =>
      axios.post(`${API_BASE}/api/finance/mortgage/simulate`, req).then(r => r.data as MortgageSimulation),
  })

  const simulateLoan = useMutation({
    mutationFn: (req: LoanInput) =>
      axios.post(`${API_BASE}/api/finance/loan/simulate`, req).then(r => r.data as LoanSimulation),
  })

  return {
    incomeEstimate: incomeEstimate.data,

    simulateMortgage: simulateMortgage.mutateAsync,
    mortgageResult: simulateMortgage.data,
    isSimulatingMortgage: simulateMortgage.isPending,
    mortgageError: simulateMortgage.error,
    resetMortgageResult: simulateMortgage.reset,

    simulateLoan: simulateLoan.mutateAsync,
    loanResult: simulateLoan.data,
    isSimulatingLoan: simulateLoan.isPending,
    loanError: simulateLoan.error,
    resetLoanResult: simulateLoan.reset,
  }
}
