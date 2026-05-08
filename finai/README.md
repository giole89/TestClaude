# 🐼 FINAI — Advisor Finanziario Personale

> **FINAI** è un'applicazione web full-stack per investitori italiani: dashboard real-time dei mercati, analisi tecnica avanzata, alert sui prezzi, gestione portafoglio P&L, portafogli modello e un advisor AI con streaming via Claude.

---

## Indice

- [Demo e screenshot](#demo-e-screenshot)
- [Funzionalità](#funzionalità)
- [Architettura](#architettura)
- [Stack tecnologico](#stack-tecnologico)
- [Struttura del progetto](#struttura-del-progetto)
- [Installazione e avvio](#installazione-e-avvio)
- [Variabili d'ambiente](#variabili-dambiente)
- [API Backend](#api-backend)
- [Indicatori tecnici implementati](#indicatori-tecnici-implementati)
- [Sistema di caching](#sistema-di-caching)

---

## Funzionalità

FINAI è composta da **8 sezioni** accessibili tramite la barra di navigazione superiore:

| Tab | Icona | Descrizione |
|-----|-------|-------------|
| **Mercato** | 📈 | Dashboard real-time con IndexBar (9 indici/valute), sentiment meter e griglia top/worst azioni + ETF |
| **Analisi** | 🔭 | Analisi completa di un ticker: grafico con SMA, 8 metriche tecniche, segnale BUY/SELL/HOLD, scenari 30gg |
| **Confronto** | ⚖️ | Confronto fianco a fianco di due strumenti con tutti gli indicatori e verdetto automatico |
| **Alert** | 🔔 | Alert sui prezzi (sopra/sotto soglia, variazione %) con notifiche browser native |
| **Lungo Termine** | 🌱 | Score 0–100 su 7 criteri per valutare idoneità DCA + strategia di accumulo consigliata |
| **Portafoglio** | 💼 | Tracker P&L personale: aggiunge posizioni, aggiorna prezzi live, mostra gain/loss per asset e totale |
| **Suggeriti** | 🎯 | 4 portafogli modello (Conservativo/Bilanciato/Crescita/Aggressivo) con allocazioni e metriche attese |
| **Guida** | 📚 | Guida completa in italiano: glossario, indicatori tecnici, ETF, DCA, errori comuni |

Ogni sezione include un **pannello chat AI** contestuale: l'assistente conosce i dati del ticker/portafoglio visualizzato e risponde in italiano con streaming in tempo reale.

---

## Architettura

```
┌─────────────────────────────────────────────────────────────────────┐
│                          Browser (React 18)                          │
│                                                                      │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌───────────────────┐  │
│  │ Zustand  │  │  React   │  │ Recharts │  │  Framer Motion    │  │
│  │  Store   │  │  Query   │  │  Charts  │  │  Animations       │  │
│  └──────────┘  └──────────┘  └──────────┘  └───────────────────┘  │
│                       │ fetch / SSE                                  │
└───────────────────────┼─────────────────────────────────────────────┘
                        │ HTTP :5173 → proxy → :3001
                        ▼
┌─────────────────────────────────────────────────────────────────────┐
│                     Backend (Express + TypeScript)                   │
│                                                                      │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │                        Routes                                 │  │
│  │  GET /api/quote/:ticker         GET /api/batch?tickers=...   │  │
│  │  GET /api/quote/:ticker/full    GET /api/history/:ticker      │  │
│  │  GET /api/indices               POST /api/ai/chat (SSE)       │  │
│  └──────────────────────────────────────────────────────────────┘  │
│                        │                    │                        │
│            ┌───────────┘                    └───────────┐           │
│            ▼                                            ▼           │
│  ┌──────────────────┐                    ┌──────────────────────┐  │
│  │  yahoo-finance2  │                    │   Anthropic Claude   │  │
│  │  (dati mercato   │                    │   (SSE streaming     │  │
│  │   server-side)   │                    │    risposte AI)      │  │
│  └──────────────────┘                    └──────────────────────┘  │
│            │                                                         │
│            ▼                                                         │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │                     NodeCache (4 tier)                        │  │
│  │  quotes: 60s  │  history: 4h  │  batch: 120s  │  AI: 30min  │  │
│  └──────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────┘
```

### Perché un backend separato?

Yahoo Finance non espone un'API pubblica CORS-safe: qualsiasi chiamata diretta dal browser viene bloccata. Il backend Node.js agisce da proxy server-side, recupera i dati, li calcola e li serve al frontend già elaborati. Questo approccio elimina il problema CORS alla radice senza workaround.

### Flusso dati — Mercato

```
React Query (refetch ogni 60s)
  → GET /api/batch?tickers=AAPL,MSFT,...
  → backend: chunking in gruppi da 10, fetchBatch parallelo
  → yahoo-finance2.quote() per ogni ticker
  → calcolo rangePosition 52W
  → risposta JSON + set cache 120s
  → frontend: sort per dayChangePct → top/worst grid
```

### Flusso dati — Analisi completa

```
useFullQuote(ticker)
  → GET /api/quote/:ticker/full
  → backend: fetchQuote() + fetchHistory('1y') in parallelo
  → calcolo su array closes[]: RSI(14), SMA(20/50/200),
    volatilità annualizzata, momentum 30gg, BullScore 0-100
  → risposta JSON con history inclusa + set cache 60s
  → frontend: StockHero, PriceChart (SMA overlay), SignalBadge, PredictionCard
```

### Flusso dati — AI Chat (SSE)

```
useChat.sendMessage(text, context)
  → POST /api/ai/chat  { messages, context: { tab, tickerData, ... } }
  → backend: buildSystemPrompt(context) con dati contestuali
  → client.messages.stream() → Anthropic API
  → res.write("data: {text: chunk}\n\n") per ogni token
  → frontend reader loop: appendToMessage() su ogni chunk
  → blinking cursor durante streaming, finalizeMessage() al [DONE]
```

---

## Stack tecnologico

### Backend

| Libreria | Versione | Ruolo |
|----------|----------|-------|
| `express` | ^4.19 | HTTP server + routing |
| `yahoo-finance2` | **^2.9.0** | Dati di mercato (pinned: v2.14+ è ESM-only, incompatibile con CommonJS) |
| `@anthropic-ai/sdk` | ^0.27 | Claude API con streaming SSE |
| `node-cache` | ^5.1 | Cache in-memory multi-tier |
| `helmet` | ^7.1 | Security headers HTTP |
| `express-rate-limit` | ^7.4 | Rate limiting (100/min globale, 20/min AI) |
| `cors` | ^2.8 | CORS headers |
| `dotenv` | ^16.4 | Variabili d'ambiente |
| `tsx` | ^4.15 | Esecuzione TypeScript in dev (watch mode) |

> **Nota sul pinning di yahoo-finance2**: Le versioni ≥ 2.14 sono ESM-only e non funzionano con `"module": "CommonJS"` in tsconfig. La versione 2.9.x è l'ultima con supporto CommonJS completo.

### Frontend

| Libreria | Versione | Ruolo |
|----------|----------|-------|
| `react` + `react-dom` | ^18.3 | UI framework |
| `vite` | ^5.3 | Build tool + dev server con proxy |
| `@tanstack/react-query` | ^5.45 | Data fetching, caching, refetch automatico |
| `zustand` | ^4.5 | State management globale con persistenza localStorage |
| `recharts` | ^2.12 | Grafici LineChart con SMA overlay |
| `framer-motion` | ^11.3 | Animazioni (PandaLoader, barre animate, scenari) |
| `axios` | ^1.7 | Client HTTP |
| `typescript` | ^5.4 | Type checking |

---

## Struttura del progetto

```
finai/
├── package.json              # Root: script dev/build con concurrently
│
├── backend/
│   ├── package.json
│   ├── tsconfig.json         # CommonJS + moduleResolution: node
│   ├── .env.example
│   └── src/
│       ├── index.ts          # Express app: helmet, cors, rate limit, routes
│       ├── middleware/
│       │   ├── rateLimit.ts  # apiLimiter (100/min), aiLimiter (20/min)
│       │   └── errorHandler.ts
│       ├── routes/
│       │   ├── quote.ts      # GET /:ticker  e  GET /:ticker/full
│       │   ├── batch.ts      # GET /?tickers=... (chunked)
│       │   ├── history.ts    # GET /:ticker?range=1y
│       │   ├── indices.ts    # GET / → 9 indici globali
│       │   └── ai.ts         # POST /chat → SSE streaming
│       └── services/
│           ├── yahooFinance.ts   # fetchQuote, fetchBatch, fetchHistory, fetchIndices
│           ├── indicators.ts     # calcRSI, calcSMA, calcVolatility, calcMomentum, calcBullScore
│           ├── anthropic.ts      # streamChatResponse + buildSystemPrompt
│           └── cache.ts          # 4 istanze NodeCache con TTL differenziati
│
└── frontend/
    ├── package.json
    ├── tsconfig.json         # bundler + noEmit + strict
    ├── vite.config.ts        # proxy /api → :3001, alias @/ → src/
    ├── index.html            # font Google: Syne, Instrument Serif, JetBrains Mono
    └── src/
        ├── main.tsx          # tema dark/light da localStorage prima del mount
        ├── App.tsx           # QueryClientProvider + lazy-loaded tab router
        ├── styles.css        # CSS custom properties: dark (default) + light theme
        │
        ├── lib/
        │   ├── constants.ts  # STOCK_UNIVERSE, ETF_UNIVERSE, INDICES, PORTFOLIO_TEMPLATES
        │   ├── formatters.ts # formatPrice, formatPct, formatLargeNumber (locale it-IT)
        │   └── indicators.ts # calcRSI/SMA/EMA/MACD/Bollinger/BullScore/LongTermScore
        │
        ├── store/
        │   ├── useAppStore.ts       # activeTab, theme toggle, pendingOps loader
        │   ├── usePortfolioStore.ts # items[], addItem, removeItem, updatePrice (persisted)
        │   ├── useAlertStore.ts     # alerts[], history[], addAlert, fireAlert (persisted)
        │   └── useChatStore.ts      # histories per tab-key, streaming append
        │
        ├── hooks/
        │   ├── useQuote.ts        # useQuote + useFullQuote via React Query
        │   ├── useHistory.ts      # useHistory con staleTime 4h
        │   ├── useMarketBatch.ts  # useIndices, useStockBatch, useEtfBatch, useMarketData
        │   ├── useChat.ts         # SSE reader loop, appendToMessage per token
        │   └── useAlerts.ts       # polling 60s, checkAlert, Notification API
        │
        ├── components/
        │   ├── layout/
        │   │   ├── Header.tsx      # Logo panda SVG + theme toggle
        │   │   ├── NavTabs.tsx     # 8 tab con highlight attivo
        │   │   └── PandaLoader.tsx # Overlay animato (bounce + shimmer) su pendingOps > 0
        │   ├── market/
        │   │   ├── IndexBar.tsx    # Pill scrollabile con 9 indici (refresh 60s)
        │   │   ├── SentimentMeter.tsx # Barra animata + emoji sentiment
        │   │   ├── MarketGrid.tsx  # 4 colonne: top/worst azioni+ETF, toggle oggi/YTD
        │   │   └── MarketRow.tsx   # Riga con ticker, prezzo, %, barra range 52W
        │   ├── analyze/
        │   │   ├── StockHero.tsx   # Header ticker + 8 metriche tecniche colorate
        │   │   ├── PriceChart.tsx  # LineChart recharts + SMA20/50 dashed overlay
        │   │   ├── SignalBadge.tsx # BUY/SELL/HOLD badge con motivazione
        │   │   └── PredictionCard.tsx # Direzione + probabilità + 3 scenari 30gg
        │   └── chat/
        │       ├── ChatPanel.tsx   # Chat UI con quick actions, textarea, send
        │       └── ChatMessage.tsx # Render markdown leggero + blinking cursor
        │
        └── pages/
            ├── MarketPage.tsx
            ├── AnalyzePage.tsx
            ├── ComparePage.tsx
            ├── AlertsPage.tsx
            ├── LongTermPage.tsx
            ├── PortfolioPage.tsx
            ├── SuggestedPage.tsx
            └── GuidePage.tsx
```

---

## Installazione e avvio

### Prerequisiti

- **Node.js** ≥ 18.x
- **npm** ≥ 9.x
- Una **API key Anthropic** (per la funzione AI chat)

### 1. Clona il repository

```bash
git clone https://github.com/giole89/TestClaude.git
cd TestClaude/finai
```

### 2. Installa tutte le dipendenze

```bash
npm run install:all
```

Questo script esegue in sequenza:
- `npm install` nella root (installa `concurrently`)
- `cd backend && npm install`
- `cd frontend && npm install`

### 3. Configura le variabili d'ambiente del backend

```bash
cp backend/.env.example backend/.env
```

Modifica `backend/.env`:

```env
ANTHROPIC_API_KEY=sk-ant-api03-...    # La tua chiave Anthropic
PORT=3001
NODE_ENV=development
CACHE_TTL_QUOTES=60                   # TTL cache quote in secondi
CACHE_TTL_HISTORY=14400               # TTL cache storico in secondi (4 ore)
AI_MODEL=claude-haiku-4-5-20251001   # Modello Claude da usare
```

### 4. Avvia l'applicazione

#### Avvio simultaneo (consigliato)

Dalla root della cartella `finai/`:

```bash
npm run dev
```

Avvia in parallelo backend (porta **3001**) e frontend (porta **5173**) tramite `concurrently`.

#### Avvio separato

```bash
# Terminale 1 — Backend
cd backend
npm run dev
# → FINAI backend avviato su http://localhost:3001

# Terminale 2 — Frontend
cd frontend
npm run dev
# → Local: http://localhost:5173
```

### 5. Apri nel browser

```
http://localhost:5173
```

Il dev server Vite fa da proxy per tutte le richieste `/api/*` verso `http://localhost:3001`, quindi non c'è alcun problema CORS in sviluppo.

### Build di produzione

```bash
npm run build
```

- Il backend viene compilato da TypeScript in `backend/dist/`
- Il frontend viene bundlato in `frontend/dist/`

Per avviare in produzione:

```bash
node backend/dist/index.js
# Servi frontend/dist/ con un web server (nginx, serve, etc.)
```

---

## Variabili d'ambiente

### Backend (`backend/.env`)

| Variabile | Default | Descrizione |
|-----------|---------|-------------|
| `ANTHROPIC_API_KEY` | — | **Obbligatoria.** Chiave API Anthropic |
| `PORT` | `3001` | Porta del server Express |
| `NODE_ENV` | `development` | Ambiente di esecuzione |
| `CACHE_TTL_QUOTES` | `60` | TTL cache quote in secondi |
| `CACHE_TTL_HISTORY` | `14400` | TTL cache storico (4 ore) |
| `AI_MODEL` | `claude-haiku-4-5-20251001` | Modello Claude per le risposte AI |

### Frontend (`frontend/.env`)

| Variabile | Default | Descrizione |
|-----------|---------|-------------|
| `VITE_API_BASE_URL` | `http://localhost:3001` | URL base del backend (in prod punta al server remoto) |

---

## API Backend

Tutti gli endpoint rispondono in JSON. Il rate limiter globale è **100 req/min** per IP; l'endpoint AI ha un limite separato di **20 req/min**.

### `GET /api/quote/:ticker`

Quote base di un titolo.

```json
{
  "ticker": "AAPL",
  "name": "Apple Inc.",
  "price": 189.84,
  "dayChange": 1.23,
  "dayChangePct": 0.65,
  "high52w": 199.62,
  "low52w": 143.90,
  "volume": 54321000,
  "marketCap": 2950000000000,
  "pe": 29.4,
  "currency": "USD",
  "exchange": "NasdaqGS",
  "rangePosition": 72,
  "timestamp": 1700000000000
}
```

### `GET /api/quote/:ticker/full`

Quote completa con indicatori tecnici e storico 1 anno. Include tutto quanto sopra, più:

```json
{
  "rsi": 58,
  "sma20": 187.40,
  "sma50": 182.10,
  "sma200": 173.55,
  "volatility": 24,
  "momentum30": 3.2,
  "bullScore": 85,
  "history": [
    { "date": "2024-01-02", "open": 185.0, "high": 186.5, "low": 184.2, "close": 185.9, "volume": 48000000 },
    ...
  ]
}
```

### `GET /api/batch?tickers=AAPL,MSFT,NVDA`

Quote base di più ticker in una sola chiamata. I ticker vengono processati in chunk da 10 in parallelo.

### `GET /api/history/:ticker?range=1y`

Storico prezzi OHLCV. Parametro `range`: `1m` | `3m` | `6m` | `1y`.

### `GET /api/indices`

Quote dei 9 indici/asset di riferimento globali:

| Ticker | Nome |
|--------|------|
| `^GSPC` | S&P 500 |
| `^NDX` | Nasdaq 100 |
| `^DJI` | Dow Jones |
| `^STOXX50E` | Euro Stoxx 50 |
| `FTSEMIB.MI` | FTSE MIB |
| `^VIX` | VIX |
| `EURUSD=X` | EUR/USD |
| `GC=F` | Oro |
| `CL=F` | WTI Oil |

### `POST /api/ai/chat`

Risposta AI in streaming SSE. Rate limit: 20 req/min.

**Request body:**
```json
{
  "messages": [
    { "role": "user", "content": "Dimmi tutto su AAPL" }
  ],
  "context": {
    "tab": "analyze",
    "ticker": "AAPL",
    "tickerData": { "price": 189.84, "rsi": 58, "bullScore": 85, ... }
  }
}
```

**Response (SSE stream):**
```
data: {"text":"Apple Inc. è attualmente..."}
data: {"text":" in una fase rialzista..."}
data: [DONE]
```

Il campo `context.tab` determina il system prompt specializzato: `analyze`, `compare`, `longterm`, `portfolio`, `suggested`, `market`.

### `GET /health`

Health check: `{ "status": "ok", "timestamp": 1700000000000 }`

---

## Indicatori tecnici implementati

Tutti gli indicatori sono calcolati sia lato backend (per le risposte API) che lato frontend (per i grafici e le previsioni).

| Indicatore | Descrizione | Parametri |
|------------|-------------|-----------|
| **RSI** | Relative Strength Index | period = 14 |
| **SMA** | Simple Moving Average | period = 20, 50, 200 |
| **EMA** | Exponential Moving Average | period configurabile |
| **MACD** | Moving Average Convergence/Divergence | EMA12 - EMA26, signal EMA9 |
| **Bande di Bollinger** | Upper/Middle/Lower band | period = 20, σ = 2 |
| **Volatilità** | Deviazione standard log-return annualizzata | √252 × σ_giornaliera |
| **Momentum** | Variazione % su N giorni | days = 30 |
| **BullScore** | Score composito 0–100 | 7 criteri pesati |
| **LongTermScore** | Idoneità investimento DCA 0–100 | 7 criteri con punti |

### BullScore — criteri e pesi

```
Prezzo > SMA200   → +25 pt   (trend primario rialzista)
Prezzo > SMA50    → +20 pt   (trend intermedio)
Prezzo > SMA20    → +15 pt   (trend breve)
RSI 50–70         → +15 pt   (forza senza ipercomprato)
Momentum30 > 0    → +10 pt   (slancio positivo)
Volatilità < 30%  → +10 pt   (stabilità)
RangePos52W > 50% → +5 pt    (vicino ai massimi annuali)
                  ─────────
                    100 pt max
```

---

## Sistema di caching

Il backend utilizza 4 istanze `NodeCache` indipendenti con TTL ottimizzati per la natura di ogni dato:

```
┌──────────────┬─────────┬──────────────────────────────────────────┐
│ Cache        │   TTL   │ Motivazione                              │
├──────────────┼─────────┼──────────────────────────────────────────┤
│ quotesCache  │  60 sec │ Dati real-time: aggiornati ogni minuto   │
│ batchCache   │ 120 sec │ Batch: leggermente più stabile           │
│ historyCache │   4 ore │ Storico: non cambia durante la giornata  │
│ aiCache      │  30 min │ Risposte AI: riusabili per stesso contesto│
└──────────────┴─────────┴──────────────────────────────────────────┘
```

Anche il frontend ha il proprio layer di caching tramite **React Query** con `staleTime` allineati ai TTL del backend.

---

## Tema e personalizzazione

L'app supporta **dark mode** (default) e **light mode**, commutabili tramite il bottone in header. Il tema viene persistito in `localStorage` e applicato sull'elemento `<html>` tramite l'attributo `data-theme` prima del mount di React (evita il flash di tema errato).

I colori principali del tema dark:

```css
--acc:    #e8f542   /* lime-yellow — accento primario */
--acc2:   #42f5d4   /* teal — accento secondario */
--acc3:   #f5a742   /* arancione — warning/neutro */
--red:    #f54242   /* rosso — negativo/sell */
--bg:     #07080a   /* sfondo principale */
```

---

## Universo di strumenti supportati

FINAI include un universo pre-configurato di **60 azioni** e **28 ETF**, con copertura di:

- **USA**: mega-cap (AAPL, MSFT, NVDA, AMZN, GOOGL, META, TSLA…) + tech (AMD, INTC, QCOM…)
- **Italia**: ISP.MI, ENEL.MI, ENI.MI, TIT.MI, UCG.MI, STM.MI…
- **Europa**: ASML.AS, NESN.SW, SAP.DE, MC.PA, TTE.PA, AIR.PA…
- **ETF globali UCITS**: VWCE.DE, IWDA.AS, EQQQ.AS, WSML.AS…
- **ETF USA**: SPY, QQQ, IVV, VOO, VTI, SCHD…
- **ETF obbligazionari**: AGGH.AS, IEAG.AS, IBTM.AS…
- **ETF tematici**: IQQH.DE (clean energy), WTAI.AS (AI), 2B76.DE (robotica)

Qualsiasi ticker Yahoo Finance può comunque essere cercato manualmente nelle tab Analisi, Confronto e Lungo Termine.

---

## Licenza

MIT — Uso libero per scopi personali ed educativi.

> ⚠️ **Disclaimer**: FINAI è uno strumento informativo e non costituisce consulenza finanziaria. Le analisi sono basate su dati storici e indicatori tecnici. Ogni decisione di investimento è di responsabilità esclusiva dell'utente.
