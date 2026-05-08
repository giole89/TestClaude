// ─── Stocks ───────────────────────────────────────────────────────────────────

// USA — Mega cap + S&P 500 leaders
export const STOCKS_US = [
  'AAPL', 'MSFT', 'NVDA', 'AMZN', 'GOOGL', 'META', 'TSLA', 'BRK-B', 'JPM', 'V',
  'UNH', 'XOM', 'LLY', 'JNJ', 'WMT', 'MA', 'PG', 'HD', 'MRK', 'AVGO',
  'CVX', 'PEP', 'COST', 'ABBV', 'KO', 'ADBE', 'CSCO', 'ACN', 'MCD', 'CRM',
  'BAC', 'TMO', 'ABT', 'NFLX', 'ORCL', 'AMD', 'INTC', 'QCOM', 'TXN', 'PM',
  'AMGN', 'GE', 'HON', 'CAT', 'GS', 'BKNG', 'SPGI', 'BLK', 'ISRG', 'NOW',
  'UBER', 'ABNB', 'SNOW', 'PLTR', 'ARM', 'SMCI', 'CRWD', 'PANW', 'ZS', 'NET',
]

// Germany — DAX 40
export const STOCKS_DE = [
  'SAP.DE', 'SIE.DE', 'ALV.DE', 'DTE.DE', 'BAYN.DE', 'BMW.DE', 'MBG.DE',
  'ADS.DE', 'BASF.DE', 'VOW3.DE', 'DBK.DE', 'RWE.DE', 'EOAN.DE', 'MUV2.DE',
  'IFX.DE', 'ZAL.DE', 'BEI.DE', 'HEI.DE', 'VNA.DE', 'RHM.DE', 'PUM.DE',
  'DHL.DE', 'FME.DE', 'QIA.DE', 'CON.DE', 'MTX.DE', 'SHL.DE',
]

// France — CAC 40
export const STOCKS_FR = [
  'MC.PA', 'OR.PA', 'TTE.PA', 'BNP.PA', 'SAN.PA', 'AIR.PA',
  'SU.PA', 'AI.PA', 'DG.PA', 'CS.PA', 'ORA.PA', 'ENGI.PA', 'KER.PA',
  'SGO.PA', 'BN.PA', 'VIE.PA', 'RMS.PA', 'CAP.PA', 'DSY.PA', 'PUB.PA',
  'HO.PA', 'RNO.PA', 'ACA.PA', 'WLN.PA', 'LR.PA',
]

// Italy — FTSE MIB
export const STOCKS_IT = [
  'ISP.MI', 'ENI.MI', 'ENEL.MI', 'TIT.MI', 'MB.MI', 'UCG.MI', 'STM.MI',
  'G.MI', 'PRY.MI', 'RACE.MI', 'MONC.MI', 'LDO.MI', 'STLA.MI', 'A2A.MI',
  'CNHI.MI', 'BAMI.MI', 'REC.MI', 'DIA.MI', 'FBK.MI', 'CPR.MI',
]

// Netherlands — AEX
export const STOCKS_NL = [
  'ASML.AS', 'INGA.AS', 'PHIA.AS', 'AD.AS', 'AKZA.AS',
  'NN.AS', 'WKL.AS', 'ADYEN.AS', 'HEIA.AS', 'BESI.AS', 'IMCD.AS',
]

// Spain — IBEX 35
export const STOCKS_ES = [
  'ITX.MC', 'IBE.MC', 'SAN.MC', 'BBVA.MC', 'REP.MC',
  'TEF.MC', 'AMS.MC', 'ELE.MC', 'FER.MC', 'CLNX.MC', 'ANA.MC',
]

// Switzerland
export const STOCKS_CH = [
  'NESN.SW', 'ROG.SW', 'NOVN.SW', 'ABBN.SW', 'ZURN.SW', 'LONN.SW',
]

export const STOCK_UNIVERSE = [
  ...STOCKS_US,
  ...STOCKS_DE,
  ...STOCKS_FR,
  ...STOCKS_IT,
  ...STOCKS_NL,
  ...STOCKS_ES,
  ...STOCKS_CH,
]

// ─── ETF ─────────────────────────────────────────────────────────────────────

export const ETF_UNIVERSE = [
  // Global / World
  'VWCE.DE', 'IWDA.AS', 'VUSA.AS', 'CSPX.AS', 'EQQQ.AS', 'WSML.AS',
  // US
  'SPY', 'QQQ', 'IVV', 'VOO', 'VTI', 'VIG', 'SCHD', 'XLK', 'XLF', 'XLE', 'XLV',
  // Sector / factor
  'IUIT.AS', 'IUHC.AS', 'IUFS.AS', 'EXV1.DE',
  // Bonds
  'AGGH.AS', 'IEAG.AS', 'IBTM.AS', 'VGEA.AS', 'IGLO.AS',
  // Thematic
  'IQQH.DE', 'CLEAN.AS', '2B76.DE', 'WTAI.AS', 'ROBO.AS', 'HEAL.AS',
  // Gold / commodities
  'SGLD.AS', 'IGLN.AS',
  // Emerging markets
  'EIMI.AS', 'VFEM.AS',
]

// ─── Indices ──────────────────────────────────────────────────────────────────

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
  'AAPL', 'MSFT', 'NVDA', 'GOOGL', 'TSLA', 'SAP.DE', 'ASML.AS',
  'ISP.MI', 'ENEL.MI', 'MC.PA', 'IWDA.AS', 'VWCE.DE', 'SPY', 'QQQ',
]

// ─── Portfolio templates ──────────────────────────────────────────────────────

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
