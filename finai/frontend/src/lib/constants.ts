export const STOCK_UNIVERSE = [
  'AAPL', 'MSFT', 'NVDA', 'AMZN', 'GOOGL', 'META', 'TSLA', 'BRK-B', 'JPM', 'V',
  'UNH', 'XOM', 'LLY', 'JNJ', 'WMT', 'MA', 'PG', 'HD', 'MRK', 'AVGO',
  'CVX', 'PEP', 'COST', 'ABBV', 'KO', 'ADBE', 'CSCO', 'ACN', 'MCD', 'CRM',
  'BAC', 'TMO', 'ABT', 'NFLX', 'ORCL', 'AMD', 'INTC', 'QCOM', 'TXN', 'PM',
  'ISP.MI', 'ENI.MI', 'ENEL.MI', 'TIT.MI', 'MB.MI', 'UCG.MI', 'STM.MI',
  'ASML.AS', 'NESN.SW', 'ROG.SW', 'NOVN.SW', 'SAP.DE', 'SIE.DE', 'ALV.DE',
  'MC.PA', 'OR.PA', 'TTE.PA', 'BNP.PA', 'SAN.PA', 'AIR.PA',
]

export const ETF_UNIVERSE = [
  'VWCE.DE', 'IWDA.AS', 'VUSA.AS', 'CSPX.AS', 'EQQQ.AS', 'WSML.AS',
  'SPY', 'QQQ', 'IVV', 'VOO', 'VTI', 'VIG', 'SCHD', 'XLK', 'XLF', 'XLE',
  'IUIT.AS', 'IUHC.AS', 'IUFS.AS', 'EXV1.DE',
  'AGGH.AS', 'IEAG.AS', 'IBTM.AS', 'VGEA.AS',
  'IQQH.DE', 'CLEAN.AS', '2B76.DE', 'WTAI.AS',
]

export const INDICES = [
  { ticker: '^GSPC', label: 'S&P 500' },
  { ticker: '^NDX', label: 'Nasdaq 100' },
  { ticker: '^DJI', label: 'Dow Jones' },
  { ticker: '^STOXX50E', label: 'Euro Stoxx 50' },
  { ticker: 'FTSEMIB.MI', label: 'FTSE MIB' },
  { ticker: '^VIX', label: 'VIX' },
  { ticker: 'EURUSD=X', label: 'EUR/USD' },
  { ticker: 'GC=F', label: 'Oro' },
  { ticker: 'CL=F', label: 'WTI Oil' },
]

export const WATCHLIST_QUICK = [
  'AAPL', 'MSFT', 'NVDA', 'GOOGL', 'TSLA', 'ISP.MI', 'ENEL.MI', 'IWDA.AS', 'VWCE.DE', 'SPY', 'QQQ',
]

export type ProfileKey = 'conservative' | 'balanced' | 'growth' | 'aggressive'

export interface AllocationItem {
  ticker: string
  name: string
  pct: number
  why: string
}

export interface PortfolioTemplate {
  label: string
  desc: string
  expected: string
  maxDD: string
  volatility: string
  horizon: string
  allocation: AllocationItem[]
}

export const PORTFOLIO_TEMPLATES: Record<ProfileKey, PortfolioTemplate> = {
  conservative: {
    label: '🛡️ Conservativo',
    desc: 'Protezione del capitale, rendimenti stabili',
    expected: '+3–5% annuo',
    maxDD: '-10%',
    volatility: 'Bassa',
    horizon: '2–5 anni',
    allocation: [
      { ticker: 'AGGH.AS', name: 'iShares Core Global Agg Bond ETF', pct: 40, why: 'Obbligazioni globali IG — stabilità e cedole' },
      { ticker: 'IEAG.AS', name: 'iShares Euro Aggregate Bond ETF', pct: 20, why: 'Obbligazioni euro — riduce rischio valutario' },
      { ticker: 'VWCE.DE', name: 'Vanguard FTSE All-World', pct: 20, why: 'Azionario globale — componente di crescita' },
      { ticker: 'IGLO.AS', name: 'iShares Global Govt Bond ETF', pct: 10, why: 'Titoli di stato — massima qualità creditizia' },
      { ticker: 'SGLD.AS', name: 'Invesco Physical Gold ETC', pct: 10, why: 'Oro fisico — protezione inflazione' },
    ],
  },
  balanced: {
    label: '⚖️ Bilanciato',
    desc: 'Mix crescita e stabilità, rischio moderato',
    expected: '+5–7% annuo',
    maxDD: '-20%',
    volatility: 'Media',
    horizon: '5–10 anni',
    allocation: [
      { ticker: 'VWCE.DE', name: 'Vanguard FTSE All-World', pct: 35, why: 'Core azionario globale' },
      { ticker: 'IWDA.AS', name: 'iShares MSCI World', pct: 20, why: 'Mercati sviluppati — qualità superiore' },
      { ticker: 'AGGH.AS', name: 'iShares Core Global Agg Bond', pct: 25, why: 'Obbligazioni — ammortizzatore volatilità' },
      { ticker: 'IUIT.AS', name: 'iShares S&P 500 IT Sector', pct: 10, why: 'Satellite tech — accesso al settore trainante' },
      { ticker: 'SGLD.AS', name: 'Invesco Physical Gold ETC', pct: 10, why: 'Oro — diversificazione non correlata' },
    ],
  },
  growth: {
    label: '🚀 Crescita',
    desc: 'Rendimento nel lungo periodo, rischio medio-alto',
    expected: '+7–10% annuo',
    maxDD: '-35%',
    volatility: 'Media-Alta',
    horizon: '10–15 anni',
    allocation: [
      { ticker: 'VWCE.DE', name: 'Vanguard FTSE All-World', pct: 40, why: 'Base azionaria globale — 3700+ aziende' },
      { ticker: 'QQQ', name: 'Invesco Nasdaq-100', pct: 20, why: 'Tech US — motore di crescita storico' },
      { ticker: 'EQQQ.AS', name: 'Invesco EQQQ Nasdaq-100 EU', pct: 15, why: 'Nasdaq EU-compliant, UCITS' },
      { ticker: 'IUIT.AS', name: 'iShares IT Sector ETF', pct: 15, why: 'Settore tech — diversificazione geografica' },
      { ticker: 'WSML.AS', name: 'iShares MSCI World Small Cap', pct: 10, why: 'Small cap — premio al rischio superiore' },
    ],
  },
  aggressive: {
    label: '⚡ Aggressivo',
    desc: 'Massimo potenziale, alta volatilità',
    expected: '+10–15% annuo',
    maxDD: '-50%+',
    volatility: 'Alta',
    horizon: '15+ anni',
    allocation: [
      { ticker: 'QQQ', name: 'Invesco Nasdaq-100', pct: 30, why: 'Core tech USA — aziende più innovative' },
      { ticker: 'NVDA', name: 'NVIDIA Corporation', pct: 15, why: 'Leader AI e GPU — rivoluzione AI' },
      { ticker: 'MSFT', name: 'Microsoft Corporation', pct: 15, why: 'Cloud + AI — fondamentali eccezionali' },
      { ticker: 'WSML.AS', name: 'iShares MSCI World Small Cap', pct: 20, why: 'Small cap — massimo potenziale LT' },
      { ticker: 'EQQQ.AS', name: 'Invesco EQQQ Nasdaq-100', pct: 20, why: 'Nasdaq EU — amplifica esposizione tech' },
    ],
  },
}

declare global {
  interface ImportMeta {
    env: Record<string, string>
  }
}

export const API_BASE = (typeof import.meta !== 'undefined' && import.meta.env?.VITE_API_BASE_URL) || 'http://localhost:3001'
