import { useQuery, useMutation } from '@tanstack/react-query'
import axios from 'axios'
import { API_BASE } from '@/lib/constants'

export interface AmortizationYear {
  year: number
  principalPaid: number
  interestPaid: number
  remainingBalance: number
}

export interface MortgageInput {
  propertyValue: number
  loanAmount: number
  interestRatePct: number
  years: number
  monthlyNetIncome?: number | null
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
  estimatedAncillaryCosts: number
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
