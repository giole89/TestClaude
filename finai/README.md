# 🐼 FINAI — Advisor Finanziario Personale

> **FINAI** è un'applicazione web full-stack per investitori italiani: dashboard real-time dei mercati, analisi tecnica avanzata, alert sui prezzi, gestione portafoglio P&L, monitoraggio IPO, portafogli modello e un advisor AI con streaming via Claude.

---

## Indice

- [Funzionalità](#funzionalità)
- [Architettura](#architettura)
- [Stack tecnologico](#stack-tecnologico)
- [Struttura del progetto](#struttura-del-progetto)
- [Installazione e avvio](#installazione-e-avvio)
- [Variabili d'ambiente](#variabili-dambiente)
- [API Backend](#api-backend)
- [Persistenza dati — SQLite](#persistenza-dati--sqlite)
- [Resilienza — Retry e Circuit Breaker](#resilienza--retry-e-circuit-breaker)
- [Indicatori tecnici implementati](#indicatori-tecnici-implementati)
- [Sistema di caching](#sistema-di-caching)
- [Universo di strumenti supportati](#universo-di-strumenti-supportati)
- [Tema e personalizzazione](#tema-e-personalizzazione)

---

## Funzionalità

FINAI è composta da **9 sezioni** accessibili tramite la barra di navigazione superiore:

| Tab | Icona | Descrizione |
|-----|-------|-------------|
| **Mercato** | 📈 | Dashboard real-time con IndexBar (9 indici/valute), sentiment meter e griglia top/worst azioni + ETF |
| **Analisi** | 🔭 | Analisi completa di un ticker con ricerca autocomplete: grafico con SMA, 8 metriche tecniche, segnale BUY/SELL/HOLD, scenari 30gg |
| **Confronto** | ⚖️ | Confronto fianco a fianco di due strumenti con tutti gli indicatori e verdetto automatico |
| **Alert** | 🔔 | Alert sui prezzi (sopra/sotto soglia, variazione %) con notifiche browser native — persistiti su SQLite |
| **Lungo Termine** | 🌱 | Score 0–100 su 7 criteri per valutare idoneità DCA + strategia di accumulo consigliata |
| **Portafoglio** | 💼 | Tracker P&L personale: aggiunge posizioni, aggiorna prezzi live, mostra gain/loss per asset e totale — persistito su SQLite |
| **IPO** | 🏛️ | Monitoraggio IPO: calendario prossime quotazioni (NASDAQ), performance IPO recenti, watchlist personale con tracker lock-up period |
| **Suggeriti** | 🎯 | 4 portafogli modello (Conservativo/Bilanciato/Crescita/Aggressivo) con allocazioni e metriche attese |
| **Guida** | 📚 | Guida completa in italiano: glossario, indicatori tecnici, ETF, DCA, errori comuni |

Ogni sezione include un **pannello chat AI** contestuale: l'assistente conosce i dati del ticker/portafoglio visualizzato e risponde in italiano con streaming in tempo reale.

---

## Architettura

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           Browser (React 18)                             │
│                                                                          │
│  ┌──────────┐  ┌───────────────┐  ┌──────────┐  ┌───────────────────┐  │
│  │ Zustand  │  │ React Query   │  │ Recharts │  │  Framer Motion    │  │
│  │  Store   │  │ (mutations +  │  │  Charts  │  │  Animations       │  │
│  │ (UI/chat)│  │  cache)       │  │          │  │                   │  │
│  └──────────┘  └───────────────┘  └──────────┘  └───────────────────┘  │
│                       │ fetch / SSE / mutations                          │
└───────────────────────┼─────────────────────────────────────────────────┘
                        │ HTTP :5173 → proxy → :3001
                        ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                      Backend (Express + TypeScript)                      │
│                                                                          │
│  ┌────────────────────────────────────────────────────────────────────┐ │
│  │                             Routes                                  │ │
│  │  /api/quote/:ticker[/full]   /api/batch   /api/history/:ticker     │ │
│  │  /api/indices                /api/search?q=                        │ │
│  │  /api/portfolio  (CRUD)      /api/alerts  (CRUD + fire)            │ │
│  │  /api/ipo/upcoming|recent    /api/ipo/watchlist (CRUD)             │ │
│  │  /api/ai/chat (SSE)                                                │ │
│  └────────────────────────────────────────────────────────────────────┘ │
│          │                      │                      │                 │
│          ▼                      ▼                      ▼                 │
│  ┌───────────────┐   ┌──────────────────┐   ┌──────────────────────┐   │
│  │ yahoo-finance2│   │   NASDAQ API     │   │  Anthropic Claude    │   │
│  │ (quote, hist, │   │ (IPO calendar:   │   │  (SSE streaming      │   │
│  │  search)      │   │  upcoming/recent)│   │   risposte AI)       │   │
│  └───────────────┘   └──────────────────┘   └──────────────────────┘   │
│          │                      │                                        │
│          └──────────┬───────────┘                                        │
│                     ▼                                                    │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │                   Retry + Circuit Breaker                         │   │
│  │  withRetry(fn, 3 tentativi, backoff 2s→4s→8s)                   │   │
│  │  CircuitBreaker(apre dopo 5 errori, reset dopo 60s)              │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                     │                                                    │
│                     ▼                                                    │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │                     NodeCache (5 tier)                            │   │
│  │  quotes: 60s │ history: 4h │ batch: 120s │ AI: 30min │ IPO: 1h  │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                     │                                                    │
│                     ▼                                                    │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │              SQLite — better-sqlite3 (WAL mode)                   │   │
│  │  portfolio_items │ alerts │ ipo_watchlist                         │   │
│  └──────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────┘
```

### Perché un backend separato?

Yahoo Finance non espone un'API pubblica CORS-safe: qualsiasi chiamata diretta dal browser viene bloccata. Il backend Node.js agisce da proxy server-side, recupera i dati, li calcola, li serve al frontend già elaborati e li persiste su SQLite. Questo elimina il problema CORS alla radice senza workaround.

### Flusso dati — Mercato

```
React Query (refetch ogni 60s)
  → GET /api/batch?tickers=AAPL,MSFT,...
  → backend: chunking in gruppi da 10, fetchBatch parallelo
  → withCircuitBreaker(withRetry(yahoo-finance2.quote()))
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

### Flusso dati — IPO

```
useUpcomingIPOs()
  → GET /api/ipo/upcoming
  → backend: NASDAQ API https://api.nasdaq.com/api/ipo/calendar?date=YYYY-MM
    con headers User-Agent/Referer/Origin per bypassare la restrizione browser
  → parsing risposta → normalizzazione UpcomingIPO[]
  → cache 1h
  → frontend: tabella con data, exchange, settore, prezzo stimato

useIPOWatchlist()
  → GET/POST/PATCH/DELETE /api/ipo/watchlist
  → SQLite: ipo_watchlist table
  → calcolo stato lock-up: giorni rimanenti dal ipoDate + lockupDays
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
| `yahoo-finance2` | **^2.9.0** | Dati di mercato, ricerca ticker (pinned: v2.14+ è ESM-only) |
| `better-sqlite3` | ^9.4.3 | Persistenza SQLite sincrona con WAL mode |
| `@anthropic-ai/sdk` | ^0.27 | Claude API con streaming SSE |
| `node-cache` | ^5.1 | Cache in-memory multi-tier |
| `axios` | ^1.7.2 | Client HTTP per NASDAQ API |
| `zod` | ^3.23.8 | Validazione runtime degli input API |
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
| `@tanstack/react-query` | ^5.45 | Data fetching, mutations CRUD, caching, refetch automatico |
| `zustand` | ^4.5 | State management per UI/chat (tab attivo, tema, chat history) |
| `recharts` | ^2.12 | Grafici LineChart con SMA overlay |
| `framer-motion` | ^11.3 | Animazioni (PandaLoader, barre animate, scenari) |
| `axios` | ^1.7 | Client HTTP |
| `typescript` | ^5.4 | Type checking strict |

---

## Struttura del progetto

```
finai/
├── package.json              # Root: script dev/build/install:all con concurrently
│
├── backend/
│   ├── package.json
│   ├── tsconfig.json         # CommonJS + moduleResolution: node + strict: false
│   ├── .env.example
│   └── src/
│       ├── index.ts          # Express app: helmet, cors, rate limit, routes, DB init
│       ├── middleware/
│       │   ├── rateLimit.ts  # apiLimiter (100/min), aiLimiter (20/min)
│       │   └── errorHandler.ts
│       ├── routes/
│       │   ├── quote.ts      # GET /:ticker  e  GET /:ticker/full
│       │   ├── batch.ts      # GET /?tickers=... (chunked parallelo)
│       │   ├── history.ts    # GET /:ticker?range=1y
│       │   ├── indices.ts    # GET / → 9 indici globali
│       │   ├── search.ts     # GET /?q=... → autocomplete ticker/nome
│       │   ├── portfolio.ts  # CRUD portafoglio + POST /refresh prezzi live
│       │   ├── alerts.ts     # CRUD alert + POST /:id/fire
│       │   ├── ipo.ts        # GET upcoming/recent + CRUD watchlist
│       │   └── ai.ts         # POST /chat → SSE streaming
│       └── services/
│           ├── database.ts       # SQLite setup (WAL), 3 tabelle, 3 service objects
│           ├── retry.ts          # withRetry(3 tentativi, backoff 2s) + CircuitBreaker
│           ├── yahooFinance.ts   # fetchQuote/Batch/History/Indices + search + IPO fetchers
│           ├── indicators.ts     # calcRSI, calcSMA, calcVolatility, calcMomentum, calcBullScore
│           ├── anthropic.ts      # streamChatResponse + buildSystemPrompt
│           └── cache.ts          # 5 istanze NodeCache con TTL differenziati
│
└── frontend/
    ├── package.json
    ├── tsconfig.json         # bundler + noEmit + strict: true
    ├── vite.config.ts        # proxy /api → :3001, alias @/ → src/
    ├── index.html            # font Google: Syne, Instrument Serif, JetBrains Mono
    └── src/
        ├── main.tsx          # tema dark/light da localStorage prima del mount
        ├── App.tsx           # QueryClientProvider + lazy-loaded tab router (9 tab)
        ├── styles.css        # CSS custom properties: dark (default) + light theme
        │
        ├── lib/
        │   ├── constants.ts  # STOCK_UNIVERSE (160+ ticker per area geografica),
        │   │                 # ETF_UNIVERSE, INDICES, PORTFOLIO_TEMPLATES
        │   ├── formatters.ts # formatPrice, formatPct, formatLargeNumber (locale it-IT)
        │   └── indicators.ts # calcRSI/SMA/EMA/MACD/Bollinger/BullScore/LongTermScore
        │
        ├── store/
        │   ├── useAppStore.ts    # activeTab (9 tab), theme toggle, pendingOps loader
        │   └── useChatStore.ts   # histories per tab-key, streaming append
        │
        ├── hooks/
        │   ├── useQuote.ts         # useQuote + useFullQuote via React Query
        │   ├── useHistory.ts       # useHistory con staleTime 4h
        │   ├── useMarketBatch.ts   # useIndices, useStockBatch, useEtfBatch, useMarketData
        │   ├── useChat.ts          # SSE reader loop, appendToMessage per token
        │   ├── useAlerts.ts        # polling 60s, checkAlert, Notification API
        │   ├── usePortfolio.ts     # React Query CRUD portfolio (add/remove/refresh)
        │   ├── useAlertsBackend.ts # React Query CRUD alert + fire mutation
        │   ├── useSearch.ts        # ricerca ticker debounced 300ms + AbortController
        │   └── useIPO.ts           # useUpcomingIPOs, useRecentIPOs, useIPOWatchlist
        │
        ├── components/
        │   ├── layout/
        │   │   ├── Header.tsx       # Logo panda SVG + theme toggle
        │   │   ├── NavTabs.tsx      # 9 tab con highlight attivo
        │   │   └── PandaLoader.tsx  # Overlay animato (bounce + shimmer) su pendingOps > 0
        │   ├── common/
        │   │   └── SearchInput.tsx  # Autocomplete ticker: debounce, dropdown con tipo/exchange
        │   ├── market/
        │   │   ├── IndexBar.tsx     # Pill scrollabile con 9 indici (refresh 60s)
        │   │   ├── SentimentMeter.tsx  # Barra animata + emoji sentiment
        │   │   ├── MarketGrid.tsx   # 4 colonne: top/worst azioni+ETF, toggle oggi/YTD
        │   │   └── MarketRow.tsx    # Riga con ticker, prezzo, %, barra range 52W
        │   ├── analyze/
        │   │   ├── StockHero.tsx    # Header ticker + 8 metriche tecniche colorate
        │   │   ├── PriceChart.tsx   # LineChart recharts + SMA20/50 dashed overlay
        │   │   ├── SignalBadge.tsx  # BUY/SELL/HOLD badge con motivazione
        │   │   └── PredictionCard.tsx  # Direzione + probabilità + 3 scenari 30gg
        │   └── chat/
        │       ├── ChatPanel.tsx    # Chat UI con quick actions, textarea, send
        │       └── ChatMessage.tsx  # Render markdown leggero + blinking cursor
        │
        └── pages/
            ├── MarketPage.tsx
            ├── AnalyzePage.tsx    # usa SearchInput per autocomplete
            ├── ComparePage.tsx
            ├── AlertsPage.tsx     # dati da SQLite via useAlertsBackend
            ├── LongTermPage.tsx
            ├── PortfolioPage.tsx  # dati da SQLite via usePortfolio
            ├── IPOPage.tsx        # 3 sub-tab: Upcoming / Recenti / Watchlist
            ├── SuggestedPage.tsx
            └── GuidePage.tsx
```

---

## Installazione e avvio

### Prerequisiti

- **Node.js** ≥ 18.x
- **npm** ≥ 9.x
- Una **API key Anthropic** (per la funzione AI chat) — ottienila su [console.anthropic.com](https://console.anthropic.com)

### 1. Clona il repository

```bash
git clone https://github.com/giole89/TestClaude.git
cd TestClaude/finai
```

> Il progetto si trova sul branch `claude/finai-web-app-I6tE2`. Se hai clonato `main`, fai:
> ```bash
> git checkout claude/finai-web-app-I6tE2
> ```

### 2. Installa tutte le dipendenze

```bash
npm run install:all
```

Questo script esegue in sequenza:
- `npm install` nella root (installa `concurrently`)
- `cd backend && npm install`
- `cd frontend && npm install`

> `better-sqlite3` compila un modulo nativo C++ durante `npm install`. Assicurati di avere `build-essential` (Linux) o Xcode Command Line Tools (macOS) installati.

### 3. Configura le variabili d'ambiente del backend

```bash
cp backend/.env.example backend/.env
```

Modifica `backend/.env`:

```env
ANTHROPIC_API_KEY=sk-ant-api03-...    # La tua chiave Anthropic (obbligatoria)
PORT=3001
NODE_ENV=development
DB_PATH=./finai.db                    # Path del database SQLite
CACHE_TTL_QUOTES=60                   # TTL cache quote in secondi
CACHE_TTL_HISTORY=14400               # TTL cache storico (4 ore)
AI_MODEL=claude-haiku-4-5-20251001   # Modello Claude da usare
```

Il database SQLite viene creato automaticamente al primo avvio: non è necessario eseguire migrazioni manualmente.

### 4. Avvia l'applicazione

#### Avvio simultaneo (consigliato)

Dalla root della cartella `finai/`:

```bash
npm run dev
```

Avvia in parallelo backend (porta **3001**) e frontend (porta **5173**) tramite `concurrently`.

Output atteso:
```
[backend]  FINAI backend avviato su http://localhost:3001
[backend]  Database SQLite inizializzato: ./finai.db
[frontend] Local: http://localhost:5173
```

#### Avvio separato

```bash
# Terminale 1 — Backend
cd backend
npm run dev

# Terminale 2 — Frontend
cd frontend
npm run dev
```

### 5. Apri nel browser

```
http://localhost:5173
```

Il dev server Vite fa da proxy per tutte le richieste `/api/*` verso `http://localhost:3001`: nessun problema CORS in sviluppo.

### Build di produzione

```bash
npm run build
```

- Il backend viene compilato da TypeScript in `backend/dist/`
- Il frontend viene bundlato in `frontend/dist/` (18 chunk ottimizzati)

Per avviare in produzione:

```bash
# Avvia il backend
node backend/dist/index.js

# Servi il frontend con nginx, serve, o simili
npx serve frontend/dist -p 80
```

> In produzione imposta `VITE_API_BASE_URL` nel frontend per puntare all'URL del backend remoto.

### Risoluzione problemi comuni

| Problema | Causa | Soluzione |
|----------|-------|-----------|
| `Cannot find module 'better-sqlite3'` | Modulo nativo non compilato | `cd backend && npm rebuild better-sqlite3` |
| `Error: ANTHROPIC_API_KEY not set` | File `.env` mancante | Copia `.env.example` e inserisci la chiave |
| Porta 3001 già in uso | Altro processo attivo | Cambia `PORT` nel `.env` o termina il processo |
| IPO upcoming vuoti | NASDAQ API temporaneamente irraggiungibile | Riprova: il circuit breaker si resetta dopo 60s |
| Dati portafoglio spariti | Path DB diverso tra avvii | Verifica che `DB_PATH` sia consistente nel `.env` |

---

## Variabili d'ambiente

### Backend (`backend/.env`)

| Variabile | Default | Obbligatoria | Descrizione |
|-----------|---------|:------------:|-------------|
| `ANTHROPIC_API_KEY` | — | ✅ | Chiave API Anthropic per la chat AI |
| `PORT` | `3001` | | Porta del server Express |
| `NODE_ENV` | `development` | | Ambiente (`development` \| `production`) |
| `DB_PATH` | `./finai.db` | | Path del file SQLite (relativo alla dir backend) |
| `CACHE_TTL_QUOTES` | `60` | | TTL cache quote in secondi |
| `CACHE_TTL_HISTORY` | `14400` | | TTL cache storico (4 ore = 14400s) |
| `AI_MODEL` | `claude-haiku-4-5-20251001` | | Modello Claude per le risposte AI |

### Frontend (`frontend/.env`)

| Variabile | Default | Descrizione |
|-----------|---------|-------------|
| `VITE_API_BASE_URL` | `http://localhost:3001` | URL base del backend (in prod punta al server remoto) |

---

## API Backend

Tutti gli endpoint rispondono in JSON. Rate limiter globale: **100 req/min** per IP. AI endpoint: **20 req/min**.

### Quote e mercato

#### `GET /api/quote/:ticker`

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

#### `GET /api/quote/:ticker/full`

Quote completa con indicatori tecnici e storico 1 anno. Include tutto il precedente, più:

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
    { "date": "2024-01-02", "open": 185.0, "high": 186.5, "low": 184.2, "close": 185.9, "volume": 48000000 }
  ]
}
```

#### `GET /api/batch?tickers=AAPL,MSFT,NVDA`

Quote base di più ticker in parallelo. Chunk da 10.

#### `GET /api/history/:ticker?range=1y`

Storico prezzi OHLCV. Parametro `range`: `1m` | `3m` | `6m` | `1y`.

#### `GET /api/indices`

Quote dei 9 indici/asset di riferimento: `^GSPC`, `^NDX`, `^DJI`, `^STOXX50E`, `FTSEMIB.MI`, `^VIX`, `EURUSD=X`, `GC=F`, `CL=F`.

#### `GET /api/search?q=apple`

Ricerca autocomplete ticker e nomi. Utilizza `yahoo-finance2.search()`.

```json
[
  { "ticker": "AAPL", "name": "Apple Inc.", "exchange": "NasdaqGS", "type": "EQUITY" },
  { "ticker": "AAPL.BA", "name": "Apple Inc.", "exchange": "Buenos Aires", "type": "EQUITY" }
]
```

Cache: 30s. Minimo 2 caratteri di query.

---

### Portafoglio

#### `GET /api/portfolio`

Restituisce tutte le posizioni salvate.

```json
[
  {
    "id": "abc123",
    "ticker": "AAPL",
    "name": "Apple Inc.",
    "qty": 10,
    "loadPrice": 175.00,
    "currentPrice": 189.84,
    "currency": "USD",
    "createdAt": "2024-01-15T10:30:00.000Z"
  }
]
```

#### `POST /api/portfolio`

Aggiunge una posizione. Body (validato con Zod):

```json
{
  "id": "abc123",
  "ticker": "AAPL",
  "name": "Apple Inc.",
  "qty": 10,
  "loadPrice": 175.00,
  "currency": "USD"
}
```

#### `DELETE /api/portfolio/:id`

Rimuove una posizione per ID.

#### `POST /api/portfolio/refresh`

Aggiorna tutti i `currentPrice` delle posizioni via Yahoo Finance e restituisce il portafoglio aggiornato.

---

### Alert

#### `GET /api/alerts`

```json
{
  "active": [
    { "id": "x1", "ticker": "AAPL", "type": "above", "value": 200, "createdAt": "..." }
  ],
  "history": [
    { "id": "x0", "ticker": "MSFT", "type": "below", "value": 300, "firedAt": "...", "firedPrice": 295.0 }
  ]
}
```

Tipi supportati: `above` | `below` | `change_up` | `change_down`.

#### `POST /api/alerts`

Body: `{ "id", "ticker", "type", "value" }`. Il ticker viene normalizzato in uppercase.

#### `DELETE /api/alerts/:id`

Rimuove un alert attivo.

#### `POST /api/alerts/:id/fire`

Segna un alert come scattato, sposta in history con prezzo e timestamp.

```json
{ "price": 201.50 }
```

---

### IPO

#### `GET /api/ipo/upcoming`

Prossime IPO dal NASDAQ calendar API. Cache 1h.

```json
[
  {
    "id": "ipo_xyz",
    "company": "Acme Corp",
    "ticker": "ACME",
    "expectedDate": "2024-03-15",
    "priceRange": "$18-$22",
    "shares": "10M",
    "exchange": "NASDAQ",
    "sector": "Technology"
  }
]
```

#### `GET /api/ipo/recent`

IPO recenti degli ultimi 30 giorni con prezzo corrente e performance rispetto al prezzo IPO.

#### `GET /api/ipo/watchlist`

Watchlist personale IPO salvata su SQLite.

#### `POST /api/ipo/watchlist`

Aggiunge alla watchlist. Body (validato con Zod):

```json
{
  "id": "w1",
  "companyName": "Acme Corp",
  "ticker": "ACME",
  "expectedDate": "2024-03-15",
  "exchange": "NASDAQ",
  "sector": "Technology",
  "lockupDays": 180,
  "ipoPrice": 20.0,
  "notes": "Interessante per il settore AI"
}
```

#### `PATCH /api/ipo/watchlist/:id`

Aggiorna campi parziali (es. `notes`, `ipoPrice`).

#### `DELETE /api/ipo/watchlist/:id`

Rimuove dalla watchlist.

---

### AI Chat

#### `POST /api/ai/chat`

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
    "tickerData": { "price": 189.84, "rsi": 58, "bullScore": 85 }
  }
}
```

**Response (SSE stream):**
```
data: {"text":"Apple Inc. è attualmente..."}
data: {"text":" in una fase rialzista..."}
data: [DONE]
```

Il campo `context.tab` determina il system prompt specializzato: `analyze`, `compare`, `longterm`, `portfolio`, `suggested`, `market`, `ipo`.

#### `GET /health`

Health check: `{ "status": "ok", "timestamp": 1700000000000 }`

---

## Persistenza dati — SQLite

Portfolio, alert e watchlist IPO sono persistiti su un database **SQLite** tramite `better-sqlite3` (sincrono, WAL mode per massima affidabilità e performance in scrittura concorrente).

### Schema

```sql
CREATE TABLE portfolio_items (
  id          TEXT PRIMARY KEY,
  ticker      TEXT NOT NULL,
  name        TEXT NOT NULL,
  qty         REAL NOT NULL,
  load_price  REAL NOT NULL,
  current_price REAL,
  currency    TEXT NOT NULL DEFAULT 'USD',
  created_at  TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE alerts (
  id          TEXT PRIMARY KEY,
  ticker      TEXT NOT NULL,
  type        TEXT NOT NULL,   -- above | below | change_up | change_down
  value       REAL NOT NULL,
  fired_at    TEXT,            -- NULL finché non scatta
  fired_price REAL,
  created_at  TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE ipo_watchlist (
  id            TEXT PRIMARY KEY,
  ticker        TEXT,
  company_name  TEXT NOT NULL,
  expected_date TEXT,
  exchange      TEXT,
  sector        TEXT,
  lockup_days   INTEGER NOT NULL DEFAULT 180,
  ipo_price     REAL,
  ipo_date      TEXT,          -- data effettiva quotazione (aggiornata con PATCH)
  notes         TEXT,
  created_at    TEXT NOT NULL DEFAULT (datetime('now'))
);
```

### Dove viene salvato il file

Il file `finai.db` viene creato nella directory specificata da `DB_PATH` nel `.env` (default: `./finai.db` relativo alla cartella `backend/`). In sviluppo troverai il file in `backend/finai.db`.

> In produzione considera di montare il file su un volume persistente (es. Docker volume) per non perdere i dati ad ogni restart del container.

---

## Resilienza — Retry e Circuit Breaker

Tutte le chiamate a Yahoo Finance e NASDAQ sono protette da due livelli di resilienza (`backend/src/services/retry.ts`):

### Retry con backoff esponenziale

```
Tentativo 1 → attendi 2s → Tentativo 2 → attendi 4s → Tentativo 3 → errore
```

3 tentativi, delay base 2000ms (raddoppia ad ogni retry). Utile per errori transienti di rete.

### Circuit Breaker

```
CHIUSO (normale) → 5 errori consecutivi → APERTO (blocca chiamate per 60s)
                                               ↓ dopo 60s
                                         SEMI-APERTO → prova 1 chiamata
                                               ↓ successo
                                            CHIUSO
```

Quando il circuit breaker è aperto, le chiamate falliscono immediatamente (senza attendere i timeout di rete) e ritornano `undefined`. Il frontend mostra l'ultimo dato in cache o un messaggio di errore.

---

## Indicatori tecnici implementati

Gli indicatori sono calcolati sia lato backend (risposte API) che lato frontend (grafici e previsioni).

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
RangePos52W > 50% →  +5 pt   (vicino ai massimi annuali)
                  ─────────
                    100 pt max
```

---

## Sistema di caching

Il backend utilizza 5 istanze `NodeCache` indipendenti con TTL ottimizzati:

```
┌──────────────┬─────────┬──────────────────────────────────────────────┐
│ Cache        │   TTL   │ Motivazione                                  │
├──────────────┼─────────┼──────────────────────────────────────────────┤
│ quotesCache  │  60 sec │ Dati real-time: aggiornati ogni minuto        │
│ batchCache   │ 120 sec │ Batch market: leggermente più stabile         │
│ historyCache │   4 ore │ Storico: non cambia durante la giornata       │
│ aiCache      │  30 min │ Risposte AI: riusabili per stesso contesto    │
│ ipoCache     │   1 ora │ IPO calendar: si aggiorna raramente           │
└──────────────┴─────────┴──────────────────────────────────────────────┘
```

La cache `search` (autocomplete) ha TTL 30s e usa la chiave `search:<query>`.

Anche il frontend ha il proprio layer di caching tramite **React Query** con `staleTime` allineati ai TTL del backend.

---

## Universo di strumenti supportati

FINAI include un universo pre-configurato di **160+ azioni** suddiviso per area geografica, più 28 ETF.

### Azioni per area geografica

| Area | Indice di riferimento | Ticker inclusi |
|------|----------------------|----------------|
| **USA** | S&P 500 / Nasdaq | AAPL, MSFT, NVDA, AMZN, GOOGL, META, TSLA, BRK-B, JPM, V, MA, UNH, XOM, LLY, JNJ, AMD, INTC, QCOM, NFLX, DIS, BAC, WMT, HD, CVX, COST, ADBE, CRM, NOW, PANW, SNOW… (60 ticker) |
| **Germania** | DAX 40 | SAP.DE, SIE.DE, ALV.DE, BMW.DE, BAYN.DE, MBG.DE, ADS.DE, MUV2.DE, DTE.DE, EOAN.DE, BAS.DE, DBK.DE, VOW3.DE, RWE.DE, HEIA.DE, HEN3.DE, FRE.DE, CON.DE, ZAL.DE… (27 ticker) |
| **Francia** | CAC 40 | MC.PA, OR.PA, SU.PA, AI.PA, KER.PA, RMS.PA, BNP.PA, SAN.PA, TTE.PA, AIR.PA, CS.PA, BN.PA, DG.PA, ACA.PA, STMPA.PA, ORA.PA, VIE.PA, SGO.PA… (25 ticker) |
| **Italia** | FTSE MIB | ISP.MI, ENI.MI, RACE.MI, MONC.MI, LDO.MI, ENEL.MI, UCG.MI, TIT.MI, STM.MI, G.MI, BAMI.MI, CPR.MI, AMP.MI, MB.MI, FCA.MI… (20 ticker) |
| **Paesi Bassi** | AEX | ASML.AS, INGA.AS, ADYEN.AS, BESI.AS, UNA.AS, HEIA.AS, RDSA.AS, NN.AS, PHIA.AS, WKL.AS, AKZA.AS (11 ticker) |
| **Spagna** | IBEX 35 | ITX.MC, IBE.MC, SAN.MC, BBVA.MC, REP.MC, TEF.MC, AMS.MC, FER.MC, ENG.MC, IAG.MC, BKT.MC (11 ticker) |
| **Svizzera** | SMI | NESN.SW, ROG.SW, NOVN.SW, ABBN.SW, ZURN.SW, UBSG.SW (6 ticker) |

### ETF supportati

| Categoria | ETF inclusi |
|-----------|-------------|
| **Globali UCITS** | VWCE.DE, IWDA.AS, EQQQ.AS, WSML.AS, IEMA.AS, ISAC.AS |
| **USA** | SPY, QQQ, IVV, VOO, VTI, SCHD, IWM, XLK, XLF |
| **Obbligazionari** | AGGH.AS, IEAG.AS, IBTM.AS, IBTS.AS, HYG, TLT |
| **Tematici** | IQQH.DE (clean energy), WTAI.AS (AI & tech), 2B76.DE (robotica), IQQB.DE (biotecnologia) |
| **Materie prime** | GLD, IAU, SLV, USO |

Qualsiasi ticker Yahoo Finance può essere cercato manualmente tramite la barra di ricerca autocomplete nelle tab **Analisi**, **Confronto** e **Lungo Termine**.

---

## Tema e personalizzazione

L'app supporta **dark mode** (default) e **light mode**, commutabili tramite il bottone in header. Il tema viene persistito in `localStorage` e applicato sull'elemento `<html>` tramite l'attributo `data-theme` prima del mount di React (elimina il flash di tema errato al caricamento).

I colori principali del tema dark:

```css
--acc:    #e8f542   /* lime-yellow — accento primario */
--acc2:   #42f5d4   /* teal — accento secondario */
--acc3:   #f5a742   /* arancione — warning/neutro */
--red:    #f54242   /* rosso — negativo/sell */
--bg:     #07080a   /* sfondo principale */
```

---

## Licenza

MIT — Uso libero per scopi personali ed educativi.

> ⚠️ **Disclaimer**: FINAI è uno strumento informativo e non costituisce consulenza finanziaria. Le analisi sono basate su dati storici e indicatori tecnici. Ogni decisione di investimento è di responsabilità esclusiva dell'utente.
