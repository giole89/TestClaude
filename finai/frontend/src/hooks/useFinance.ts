import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import axios from 'axios'
import { API_BASE } from '@/lib/constants'

export interface Transaction {
  id: string
  date: string
  description: string
  amount: number
  category: string
  type: 'INCOME' | 'VARIABLE_EXPENSE'
  sourceFile: string | null
}

export interface StatementUploadResult {
  imported: number
  skipped: number
  duplicates: number
  transactions: Transaction[]
}

export interface FixedExpense {
  id: string
  name: string
  category: string
  amount: number
  active: boolean
  createdAt: string
}

export interface FixedExpenseInput {
  name: string
  category: string
  amount: number
  active?: boolean
}

export interface CategoryAmount {
  category: string
  amount: number
}

export interface Budget {
  periodLabel: string
  estimatedIncome: number
  fixedCosts: number
  variableCostsEstimate: number
  variableByCategory: CategoryAmount[]
  incomeByCategory: CategoryAmount[]
  projectedSavings: number
  investableAmount: number
  monthsOfHistory: number
  hasEnoughData: boolean
}

export interface MonthlyExpenses {
  periodLabel: string
  total: number
  byCategory: CategoryAmount[]
}

export type InvestmentGoal = 'EMERGENCY' | 'MAJOR_PURCHASE' | 'RETIREMENT' | 'GROWTH' | 'OTHER'
export type InvestmentHorizon = 'UNDER_1Y' | 'Y1_3' | 'Y3_5' | 'Y5_10' | 'OVER_10Y'

export interface InvestorProfile {
  goal: InvestmentGoal | null
  goalNote: string | null
  horizon: InvestmentHorizon | null
  completed: boolean
}

export interface Allocation {
  equityPct: number
  bondPct: number
  liquidityPct: number
}

export interface MarketSnapshot {
  sp500ChangePct: number | null
  vixLevel: number | null
  sentiment: string
  note: string
}

export interface PortfolioLine {
  ticker: string
  name: string
  assetClass: string
  weightPct: number
  price: number | null
  dayChangePct: number | null
  currency: string | null
  rationale: string
}

export interface Recommendation {
  profileLabel: string
  allocation: Allocation
  summary: string
  suggestedInstruments: string[]
  goal: string
  horizon: string
  marketSnapshot: MarketSnapshot | null
  samplePortfolio: PortfolioLine[]
}

const TRANSACTIONS_KEY = ['financeTransactions']
const FIXED_EXPENSES_KEY = ['financeFixedExpenses']
const BUDGET_KEY = ['financeBudget']
const CURRENT_MONTH_EXPENSES_KEY = ['financeCurrentMonthExpenses']
const PROFILE_KEY = ['financeProfile']
const RECOMMENDATION_KEY = ['financeRecommendation']

export function useFinance() {
  const qc = useQueryClient()

  const transactions = useQuery<Transaction[]>({
    queryKey: TRANSACTIONS_KEY,
    queryFn: () => axios.get(`${API_BASE}/api/finance/transactions`).then(r => r.data),
    staleTime: 30_000,
  })

  const fixedExpenses = useQuery<FixedExpense[]>({
    queryKey: FIXED_EXPENSES_KEY,
    queryFn: () => axios.get(`${API_BASE}/api/finance/fixed-expenses`).then(r => r.data),
    staleTime: 30_000,
  })

  const budget = useQuery<Budget>({
    queryKey: BUDGET_KEY,
    queryFn: () => axios.get(`${API_BASE}/api/finance/budget/next-month`).then(r => r.data),
    staleTime: 30_000,
  })

  const currentMonthExpenses = useQuery<MonthlyExpenses>({
    queryKey: CURRENT_MONTH_EXPENSES_KEY,
    queryFn: () => axios.get(`${API_BASE}/api/finance/expenses/current-month`).then(r => r.data),
    staleTime: 30_000,
  })

  const profile = useQuery<InvestorProfile>({
    queryKey: PROFILE_KEY,
    queryFn: () => axios.get(`${API_BASE}/api/finance/questionnaire`).then(r => r.data),
    staleTime: 60_000,
  })

  const recommendation = useQuery<Recommendation>({
    queryKey: RECOMMENDATION_KEY,
    queryFn: () => axios.get(`${API_BASE}/api/finance/recommendation`).then(r => r.data),
    enabled: !!profile.data?.completed,
    retry: false,
    staleTime: 60_000,
  })

  const invalidateMoneyFlows = () => {
    qc.invalidateQueries({ queryKey: TRANSACTIONS_KEY })
    qc.invalidateQueries({ queryKey: BUDGET_KEY })
    qc.invalidateQueries({ queryKey: CURRENT_MONTH_EXPENSES_KEY })
  }

  const uploadStatement = useMutation({
    mutationFn: (file: File) => {
      const form = new FormData()
      form.append('file', file)
      return axios.post(`${API_BASE}/api/finance/statements/upload`, form, {
        headers: { 'Content-Type': 'multipart/form-data' },
      }).then(r => r.data as StatementUploadResult)
    },
    onSuccess: invalidateMoneyFlows,
  })

  const deleteTransaction = useMutation({
    mutationFn: (id: string) => axios.delete(`${API_BASE}/api/finance/transactions/${id}`),
    onSuccess: invalidateMoneyFlows,
  })

  const deleteTransactions = useMutation({
    mutationFn: (ids: string[]) => axios.delete(`${API_BASE}/api/finance/transactions`, { data: ids }),
    onSuccess: invalidateMoneyFlows,
  })

  const deleteAllTransactions = useMutation({
    mutationFn: () => axios.delete(`${API_BASE}/api/finance/transactions/all`),
    onSuccess: invalidateMoneyFlows,
  })

  const invalidateFixedExpenses = () => {
    qc.invalidateQueries({ queryKey: FIXED_EXPENSES_KEY })
    qc.invalidateQueries({ queryKey: BUDGET_KEY })
  }

  const createFixedExpense = useMutation({
    mutationFn: (req: FixedExpenseInput) => axios.post(`${API_BASE}/api/finance/fixed-expenses`, req).then(r => r.data),
    onSuccess: invalidateFixedExpenses,
  })

  const updateFixedExpense = useMutation({
    mutationFn: (args: { id: string; req: FixedExpenseInput }) =>
      axios.put(`${API_BASE}/api/finance/fixed-expenses/${args.id}`, args.req).then(r => r.data),
    onSuccess: invalidateFixedExpenses,
  })

  const deleteFixedExpense = useMutation({
    mutationFn: (id: string) => axios.delete(`${API_BASE}/api/finance/fixed-expenses/${id}`),
    onSuccess: invalidateFixedExpenses,
  })

  const submitQuestionnaire = useMutation({
    mutationFn: (req: { goal: InvestmentGoal; goalNote?: string; horizon: InvestmentHorizon }) =>
      axios.post(`${API_BASE}/api/finance/questionnaire`, req).then(r => r.data as Recommendation),
    onSuccess: (data) => {
      qc.setQueryData(RECOMMENDATION_KEY, data)
      qc.invalidateQueries({ queryKey: PROFILE_KEY })
    },
  })

  return {
    transactions: transactions.data ?? [],
    isLoadingTransactions: transactions.isLoading,
    fixedExpenses: fixedExpenses.data ?? [],
    isLoadingFixedExpenses: fixedExpenses.isLoading,
    budget: budget.data,
    isLoadingBudget: budget.isLoading,
    currentMonthExpenses: currentMonthExpenses.data,
    isLoadingCurrentMonthExpenses: currentMonthExpenses.isLoading,
    profile: profile.data,
    recommendation: recommendation.data,
    isLoadingRecommendation: recommendation.isFetching,

    uploadStatement: uploadStatement.mutateAsync,
    isUploading: uploadStatement.isPending,
    deleteTransaction: deleteTransaction.mutateAsync,
    deleteTransactions: deleteTransactions.mutateAsync,
    isDeletingTransactions: deleteTransactions.isPending,
    deleteAllTransactions: deleteAllTransactions.mutateAsync,
    isDeletingAllTransactions: deleteAllTransactions.isPending,

    createFixedExpense: createFixedExpense.mutateAsync,
    isCreatingFixedExpense: createFixedExpense.isPending,
    updateFixedExpense: updateFixedExpense.mutateAsync,
    deleteFixedExpense: deleteFixedExpense.mutateAsync,

    submitQuestionnaire: submitQuestionnaire.mutateAsync,
    isSubmittingQuestionnaire: submitQuestionnaire.isPending,
  }
}
