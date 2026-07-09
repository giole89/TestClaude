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
- [Persistenza dati — PostgreSQL](#persistenza-dati--postgresql)
- [Resilienza — Retry e Circuit Breaker](#resilienza--retry-e-circuit-breaker)
- [Test](#test)
- [Indicatori tecnici implementati](#indicatori-tecnici-implementati)
- [Sistema di caching](#sistema-di-caching)
- [Universo di strumenti supportati](#universo-di-strumenti-supportati)
- [Tema e personalizzazione](#tema-e-personalizzazione)

---

## Funzionalità

FINAI è composta da **14 sezioni** accessibili tramite la barra di navigazione superiore:

| Tab | Icona | Descrizione |
|-----|-------|-------------|
| **Mercato** | 📈 | Dashboard real-time con IndexBar (9 indici/valute), sentiment meter e griglia top/worst azioni + ETF |
| **Analisi** | 🔭 | Analisi completa di un ticker con grafico SMA, 8 metriche tecniche, segnale BUY/SELL/HOLD, DCF semplificato, news ticker |
| **Confronto** | ⚖️ | Confronto fianco a fianco di due strumenti con tutti gli indicatori e verdetto automatico |
| **Alert** | 🔔 | Alert sui prezzi (sopra/sotto soglia, variazione %) con notifiche browser native — persistiti su PostgreSQL |
| **Lungo Termine** | 🌱 | Score 0–100 su 7 criteri per valutare idoneità DCA + strategia di accumulo consigliata |
| **Portafoglio** | 💼 | Tracker P&L personale, benchmark vs S&P 500, dividendi, gain fiscale 26%, DCA simulator, correlazione, news |
| **Simulazione** | 🧪 | Paper trading con moneta virtuale (100.000€): acquisto/vendita titoli reali a prezzi live, P&L realizzato/non realizzato, storico operazioni |
| **Finanza Personale** | 💰 | Import estratto conto (PDF/Excel) con categorizzazione automatica, spese fisse, budget previsionale mese successivo, questionario investitore |
| **Mutui** | 🏠 | Calcolatore mutuo per l'acquisto di una casa: rata, LTV, rapporto rata/reddito, stress test tassi, capitale proprio e spese accessorie (notaio, istruttoria, perizia, agenzia in % o € con IVA automatica, imposte), fonti a cui attingere (liquidità, vendita di un'altra casa, fondo pensione, risparmio mensile) |
| **Finanziamenti** | 💳 | Calcolatore finanziamento/prestito personale con piano di ammortamento e rapporto rata/reddito |
| **Suggeriti** | 🎯 | 4 portafogli modello (Conservativo/Bilanciato/Crescita/Aggressivo) con allocazioni e metriche attese |
| **IPO** | 🏛️ | Monitoraggio IPO: calendario prossime quotazioni (NASDAQ), performance IPO recenti, watchlist con tracker lock-up |
| **Screener** | 🔍 | Screener azionario su ~85 ticker chiave con filtri P/E, dividend yield, YTD, mercato |
| **Watchlist** | ⭐ | Watchlist personale ticker con target price, distanza dal target, prezzi live — persistita su PostgreSQL |
| **Macro** | 🌍 | Dashboard macro: indici, valute, commodity, crypto, tassi USA 10/30Y, sentiment indicator globale |
| **Guida** | 📚 | Guida completa in italiano: glossario, indicatori tecnici, ETF, DCA, finanza personale, simulazione, errori comuni |

Ogni sezione include un **pannello chat AI** contestuale: l'assistente conosce i dati del ticker/portafoglio visualizzato e risponde in italiano con streaming in tempo reale.

---

## Nuove funzionalità (v2.0)

### Portafoglio avanzato
- **Benchmark vs S&P 500**: confronto performance portafoglio con S&P 500 su periodo 3m/6m/1y, con calcolo alpha
- **Dividendi**: sezione dedicata con rendimento, ex-date e income annuale stimato per posizione
- **Gain Fiscale Italia**: calcolo indicativo capital gain al 26% (imposte, gain netto per ogni posizione)
- **DCA Simulator**: simulazione acquisti mensili con prezzo medio carico, valore finale stimato
- **Correlazione**: matrice heatmap di Pearson sui ritorni giornalieri (max 8 ticker, dati 1y)
- **News portafoglio**: feed news da Yahoo Finance per i ticker in portafoglio

### Analisi avanzata
- **DCF Semplificato**: fair value da EPS corrente, growth rate, tasso sconto, terminal growth con semaforo
- **News ticker**: news Yahoo Finance direttamente nella pagina di analisi

### Nuove tab
- **Screener**: filtra azioni per P/E, yield, YTD, mercato; bottoni "Analizza" e "Aggiungi a portafoglio"
- **Watchlist**: monitora ticker con target price; distanza dal target calcolata live; persistita su DB
- **Macro Dashboard**: indici globali, valute, commodity, crypto (BTC/ETH), tassi USA; sentiment indicator

### Finanza personale e simulazione (v3.0)
- **Import estratto conto**: upload PDF (PDFBox) o Excel (Apache POI) con parsing heuristico multi-strategia e categorizzazione automatica per keyword (14 categorie di spesa, 5 di entrata)
- **Spese fisse**: gestione costi ricorrenti mensili (affitto, mutuo, utenze, abbonamenti…) usati come base del budget; può essere marcata come debito/finanziamento indicando un tasso di interesse annuo
- **Budget previsionale**: stima di entrate/uscite/risparmio investibile del mese successivo basata sulla media degli ultimi mesi completi, con fallback sul mese in corso quando non c'è ancora storico utilizzabile; il saldo previsto può essere negativo (mese in perdita), distinto dalla quota investibile (sempre ≥ 0)
- **Suggerimenti di risparmio**: analisi reale dei movimenti importati (categorie sovrappesate, regola 50/30/20, pagamenti ricorrenti, trend di spesa in aumento) con stima del risparmio mensile potenziale
- **Questionario investitore**: obiettivo, orizzonte temporale e liquidità già accantonata → motore a regole che propone un'allocazione equity/bond/liquidità, arricchita da un portafoglio esempio in ETF reali
- **Pesatura di portafoglio stile Markowitz**: il peso core/satellite di ciascun bucket (azionario/obbligazionario) è calcolato risolvendo la formula chiusa del portafoglio tangente a 2 asset (rendimento, volatilità e correlazione su storico a 3 anni), non più un confronto isolato di Sharpe ratio
- **Controlli pre-investimento**: prima di consigliare di investire la quota disponibile, segnala se il fondo di emergenza (liquidità ≥ 3 mesi di spese) non è ancora adeguato o se tra le spese fisse c'è un debito ad alto interesse (≥ 6%/anno) da estinguere con priorità
- **Suggerimento PAC**: la quota investibile è un risparmio mensile ricorrente, non una somma unica: il consiglio propone un piano di accumulo (dollar-cost averaging) invece di un investimento in un'unica soluzione
- **Simulatore (paper trading)**: wallet virtuale da 100.000€, acquisti/vendite ai prezzi live di Yahoo Finance, calcolo P&L realizzato e non realizzato, reset in qualsiasi momento

### Mutui (v3.1)
- **Calcolatore mutuo**: da importo immobile, importo richiesto, tasso annuo (TAN) e anni, calcola la rata con piano di ammortamento **alla francese** (rata costante), il costo totale e gli interessi totali
- **LTV (Loan-to-Value)**: rapporto mutuo/valore immobile, con avviso se supera l'80% tipico dei mutui fondiari italiani
- **Rapporto rata/reddito complessivo**: somma la nuova rata alle rate di eventuali altri debiti/finanziamenti già tra le spese fisse (quelli con un tasso di interesse dichiarato), non solo la rata isolata; reddito netto mensile dichiarabile manualmente o stimato automaticamente dal budget; classificazione **Sostenibile** (≤30%) / **Al limite** (30-35%) / **Rischioso** (>35%)
- **Stress test tassi**: simula un rialzo di 2 punti percentuali (rilevante per mutui a tasso variabile) e segnala se la sostenibilità verrebbe compromessa
- **Tutto ciò che non rientra nel mutuo**: capitale proprio (prezzo − mutuo) più spese accessorie — notaio, istruttoria bancaria, perizia, agenzia immobiliare, imposta di registro/IVA (stimata in base al tipo di acquisto: prima/seconda casa, da privato/costruttore) — dichiarabili o stimate automaticamente
- **Commissione di agenzia in % o in €**: selezionabile come percentuale (IVA al 22% aggiunta automaticamente, prassi tipica "commissione + IVA") oppure come importo finale in euro già comprensivo di ogni imposta (es. da preventivo)
- **Fonti a cui attingere**: confronto tra il totale da pagare e la liquidità disponibile (dichiarata o dal questionario Finanza Personale); se dichiari gli anni di iscrizione a un fondo pensione complementare, verifica l'idoneità all'anticipazione per prima casa (D.Lgs. 252/2005: richiede almeno 8 anni di iscrizione, mai ammessa per la seconda casa) e ne stima l'importo (fino al 75% del montante); infine, per il fabbisogno residuo, stima i mesi di risparmio necessari in base al budget e propone altre opzioni pratiche
- **Vendita di un'altra casa**: da valore di vendita, prezzo di acquisto, anni di possesso e (opzionali) abitazione principale, mutuo/finanziamento residuo, spese di agenzia e tempistica prevista, calcola la plusvalenza e la sua tassazione (imposta sostitutiva 26% se posseduto da meno di 5 anni e non abitazione principale, art. 67 TUIR; esente altrimenti), sottrae l'eventuale mutuo residuo da estinguere e restituisce il capitale netto disponibile (segnalando se il risultato è negativo), più un confronto tra la tempistica di vendita prevista e il tempo medio di vendita in Italia (~6 mesi). Il capitale netto positivo si somma alla liquidità disponibile per ridurre il fabbisogno residuo del nuovo mutuo

### Finanziamenti (v3.1)
- **Calcolatore finanziamento/prestito personale**: stesso motore di calcolo alla francese (durata in mesi), senza LTV né spese accessorie, per prestiti personali o cessioni del quinto, con lo stesso rapporto rata/reddito complessivo dei mutui

---

## Architettura

```
┌────────────────────────────────────────────────────────────────────────────┐
│                            Browser (React 18)                               │
│                                                                             │
│  ┌──────────┐  ┌─────────────────┐  ┌──────────┐  ┌──────────────────┐   │
│  │ Zustand  │  │  React Query    │  │ Recharts │  │  Framer Motion   │   │
│  │ (UI/chat)│  │ (CRUD + cache)  │  │ (grafici)│  │  (animazioni)    │   │
│  └──────────┘  └─────────────────┘  └──────────┘  └──────────────────┘   │
│                        │ HTTP / SSE                                         │
└────────────────────────┼───────────────────────────────────────────────────┘
                         │ :5173 → proxy → :3001
                         ▼
┌────────────────────────────────────────────────────────────────────────────┐
│             Backend Java 21 — Spring Boot 3.3 (thread virtuali)            │
│                                                                             │
│  ┌──────────────────────────────────────────────────────────────────────┐  │
│  │                          REST Controllers                             │  │
│  │  /api/quote[/full] │ /api/batch │ /api/history │ /api/indices        │  │
│  │  /api/search       │ /api/portfolio (CRUD) │ /api/alerts (CRUD+fire) │  │
│  │  /api/ipo/upcoming|recent │ /api/ipo/watchlist (CRUD)                │  │
│  │  /api/watchlist (CRUD) │ /api/screener │ /api/sim (wallet/buy/sell)  │  │
│  │  /api/finance (estratti, spese fisse, budget, questionario)         │  │
│  │  /api/portfolio/benchmark │ /api/portfolio/correlation               │  │
│  │  /api/dividends │ /api/earnings │ /api/news                         │  │
│  │  /api/ai/chat (SSE) │ /health │ /swagger-ui.html                     │  │
│  └──────────────────────────────────────────────────────────────────────┘  │
│                    │                        │                    │          │
│                    ▼                        ▼                    ▼          │
│         ┌─────────────────┐   ┌──────────────────┐   ┌──────────────────┐ │
│         │ YahooFinance    │   │  NASDAQ IPO API  │   │ Anthropic Claude │ │
│         │ Service         │   │  Service         │   │ Service (SSE)    │ │
│         │ (quote/history/ │   │  (calendar:      │   │                  │ │
│         │  search/batch)  │   │   upcoming/recent│   │                  │ │
│         └────────┬────────┘   └────────┬─────────┘   └──────────────────┘ │
│                  └──────────┬──────────┘                                   │
│                             ▼                                               │
│         ┌──────────────────────────────────────────────────────────────┐   │
│         │           Resilience4j (Retry + Circuit Breaker)              │   │
│         │  @Retry(3 tentativi, backoff 2s→4s→8s, per Yahoo e NASDAQ)   │   │
│         │  @CircuitBreaker(apre al 50% errori su 10 call, reset 60s)   │   │
│         └──────────────────────────┬───────────────────────────────────┘   │
│                                    │                                        │
│         ┌──────────────────────────▼───────────────────────────────────┐   │
│         │                  Caffeine Cache (5 tier)                       │   │
│         │  quotes:60s │ batch:120s │ history:4h │ ipo:1h │ search:30s  │   │
│         └──────────────────────────┬───────────────────────────────────┘   │
│                                    │                                        │
│         ┌──────────────────────────▼───────────────────────────────────┐   │
│         │              Spring Data JPA + Flyway                          │   │
│         │  9 repository: Portfolio, Alert, IpoWatchlist, Watchlist,      │   │
│         │  SimWallet/SimPosition/SimTrade, BankTransaction,              │   │
│         │  FixedExpense, InvestorProfile                                 │   │
│         └──────────────────────────┬───────────────────────────────────┘   │
│                                    │                                        │
└────────────────────────────────────┼───────────────────────────────────────┘
                                     │
                         ┌───────────▼──────────────┐
                         │   PostgreSQL 16            │
                         │   portfolio_items          │
                         │   alerts                   │
                         │   ipo_watchlist            │
                         │   watchlist_items          │
                         │   sim_wallet/positions/trades │
                         │   bank_transactions          │
                         │   fixed_expenses             │
                         │   investor_profile           │
                         └──────────────────────────┘
```

### Perché Java/Spring Boot per il backend?

- **Thread virtuali (Java 21)**: il server gestisce migliaia di chiamate SSE concorrenti senza esaurire i thread OS
- **Spring Data JPA + Flyway**: ORM type-safe con schema versionato — nessuna migrazione manuale
- **Resilience4j**: retry e circuit breaker decorativi con `@Retry`/`@CircuitBreaker` — zero codice boilerplate
- **OpenAPI/Swagger**: documentazione automatica dell'API su `/swagger-ui.html`
- **Type safety**: Java strict typing + Bean Validation (`@Valid`) garantisce contratti API robusti

### Perché un backend separato (proxy)?

Yahoo Finance non espone un'API pubblica CORS-safe: qualsiasi chiamata diretta dal browser viene bloccata. Il backend Spring Boot agisce da proxy server-side, recupera i dati, li elabora con `IndicatorsService`, li cachea in Caffeine e li serve al frontend già pronti.

### Flusso dati — Mercato

```
React Query (refetch ogni 60s)
  → GET /api/batch?tickers=AAPL,MSFT,...
  → BatchController → YahooFinanceService
  → @Retry + @CircuitBreaker → yahoo v7/finance/quote
  → calcRangePosition() → risposta JSON + Caffeine cache 120s
  → frontend: sort per dayChangePct → top/worst grid
```

### Flusso dati — Analisi completa

```
useFullQuote(ticker)
  → GET /api/quote/:ticker/full
  → QuoteController → YahooFinanceService.fetchQuote() + fetchHistory("1y") in parallelo
  → IndicatorsService: calcRsi(14), calcSma(20/50/200), calcVolatility,
    calcMomentum(30), calcBullScore() → tutto su array closes[]
  → FullQuoteDto con history inclusa + cache 60s
  → StockHero, PriceChart (SMA overlay), SignalBadge, PredictionCard
```

### Flusso dati — AI Chat (SSE)

```
useChat.sendMessage(text, context)
  → POST /api/ai/chat  { messages, context: {tab, tickerData,...} }
  → AiController → thread virtuale
  → AnthropicService.streamChat()
    → buildSystemPrompt(context) — prompt specializzato per tab
    → WebClient.post() → anthropic/v1/messages con stream:true
    → per ogni event.type="content_block_delta": emitter.send({"text":"..."})
    → [DONE] → emitter.complete()
  → frontend: reader loop → appendToMessage per chunk → blinking cursor
```

### Flusso dati — Finanza Personale (import + budget)

```
StatementUpload (drag&drop PDF/Excel)
  → POST /api/finance/statements/upload (multipart)
  → FinanceController → StatementParserService
    → PDF: PDFBox PDFTextStripper + 3 strategie regex (tabella larga / riga-con-importo / layout a colonne)
    → Excel: Apache POI WorkbookFactory + rilevamento header per keyword (o fallback posizionale)
  → TransactionCategorizer.classify() → keyword matching su 14 categorie spesa + 5 entrata
  → BankTransactionRepository.saveAll() (dedup su data+descrizione+importo)
  → risposta {imported, skipped, duplicates}

useFinance() (React Query)
  → GET /api/finance/budget/next-month
  → BudgetService: media mesi storici completi con almeno un'entrata
    (fallback sul mese in corso se non c'è storico utilizzabile)
  → BudgetDto {estimatedIncome, fixed, variableEstimate, variableByCategory, basedOnCurrentMonthOnly}
  → BudgetSummary + ExpensesPieChart
```

### Flusso dati — Simulazione (paper trading)

```
SimulatorPage: form acquisto/vendita
  → POST /api/sim/buy { ticker, qty }
  → SimulatorController → SimulatorService
    → YahooFinanceService.fetchQuote(ticker) → prezzo live
    → verifica liquidità disponibile → upsert SimPosition (prezzo medio ponderato)
    → SimWallet.cashBalance -= costo → SimTrade (side=BUY) salvato
  → GET /api/sim/summary → posizioni valorizzate a prezzo live + P&L totale
  → reset(): POST /api/sim/reset → azzera posizioni/trade, ripristina capitale iniziale
```

---

## Stack tecnologico

### Backend (Java 21 / Spring Boot 3.3)

| Libreria | Versione | Ruolo |
|----------|----------|-------|
| `spring-boot-starter-web` | 3.3.5 | REST API, MVC, Tomcat embedded |
| `spring-boot-starter-webflux` | 3.3.5 | WebClient (outbound HTTP), SSE |
| `spring-boot-starter-data-jpa` | 3.3.5 | ORM con Hibernate 6.5 |
| `spring-boot-starter-validation` | 3.3.5 | Bean Validation (`@Valid`, `@NotBlank`, ecc.) |
| `spring-boot-starter-cache` | 3.3.5 | Astrazione cache |
| `spring-boot-starter-actuator` | 3.3.5 | Health check, metriche |
| `postgresql` | runtime | Driver JDBC PostgreSQL |
| `flyway-core` + `flyway-database-postgresql` | incluso in Boot | Migrazioni schema versionato (V1-V6) |
| `caffeine` | incluso in Boot | Cache in-memory LRU con TTL |
| `resilience4j-spring-boot3` + `resilience4j-reactor` | 2.2.0 | Retry + Circuit Breaker annotazionali |
| `springdoc-openapi-starter-webmvc-ui` | 2.6.0 | Swagger UI automatico |
| `lombok` | incluso in Boot | Riduzione boilerplate (getter/setter) |
| `jackson-datatype-jsr310` | incluso in Boot | Serializzazione `java.time.*` (LocalDate, Instant) |
| `pdfbox` | 3.0.3 | Estrazione testo da estratti conto PDF |
| `poi-ooxml` | 5.3.0 | Parsing estratti conto Excel (.xlsx/.xls) |
| `junit-jupiter` | incluso in Boot | Test unitari e di integrazione |
| `testcontainers` | 1.20.3 | PostgreSQL reale nei test CI |
| `jacoco-maven-plugin` | 0.8.12 | Report e soglia di copertura (70%) |

### Frontend (React 18 / Vite / TypeScript)

| Libreria | Versione | Ruolo |
|----------|----------|-------|
| `react` + `react-dom` | ^18.3 | UI framework |
| `vite` | ^5.3 | Build tool + dev server con proxy |
| `@tanstack/react-query` | ^5.45 | Data fetching, mutations CRUD, caching |
| `zustand` | ^4.5 | State UI/chat (tab attivo, tema) |
| `recharts` | ^2.12 | Grafici LineChart con SMA overlay |
| `framer-motion` | ^11.3 | Animazioni |
| `typescript` | ^5.4 | Type checking strict |

---

## Struttura del progetto

```
finai/
├── package.json                # Root: script dev/build/test con concurrently
├── docker-compose.yml          # PostgreSQL + PgAdmin per sviluppo locale
│
├── backend/                    # Java 21 / Spring Boot 3.3
│   ├── pom.xml                 # Maven: dipendenze, JaCoCo, Surefire
│   ├── .env.example
│   └── src/
│       ├── main/
│       │   ├── java/com/finai/
│       │   │   ├── FinaiApplication.java
│       │   │   ├── config/
│       │   │   │   ├── CacheConfig.java          # Caffeine multi-tier
│       │   │   │   ├── OpenApiConfig.java         # Swagger UI
│       │   │   │   ├── RateLimitInterceptor.java  # Token bucket per IP
│       │   │   │   ├── WebClientConfig.java       # WebClient Yahoo/NASDAQ/Anthropic
│       │   │   │   └── WebMvcConfig.java          # CORS + registrazione interceptor
│       │   │   ├── controller/
│       │   │   │   ├── QuoteController.java       # GET /:ticker[/full]
│       │   │   │   ├── BatchController.java       # GET /?tickers=...
│       │   │   │   ├── HistoryController.java     # GET /:ticker?range=1y
│       │   │   │   ├── IndicesController.java     # GET / → 9 indici
│       │   │   │   ├── SearchController.java      # GET /?q= autocomplete
│       │   │   │   ├── PortfolioController.java   # CRUD + POST /refresh
│       │   │   │   ├── AlertController.java       # CRUD + POST /:id/fire
│       │   │   │   ├── IpoController.java         # upcoming/recent + watchlist CRUD
│       │   │   │   ├── WatchlistController.java   # CRUD watchlist personale
│       │   │   │   ├── ScreenerController.java    # GET /?minPE&maxPE&minYield&minYtd&market&limit
│       │   │   │   ├── BenchmarkController.java   # GET /api/portfolio/benchmark?period=
│       │   │   │   ├── CorrelationController.java # GET /api/portfolio/correlation
│       │   │   │   ├── DividendController.java    # GET /?tickers=
│       │   │   │   ├── EarningsController.java    # GET /?tickers=
│       │   │   │   ├── NewsController.java        # GET /?tickers=&count=
│       │   │   │   ├── SimulatorController.java   # wallet/summary/trades + buy/sell/reset
│       │   │   │   ├── FinanceController.java     # estratti conto, spese fisse, budget, questionario, mutuo/finanziamenti
│       │   │   │   ├── AiController.java          # POST /chat SSE (thread virtuali)
│       │   │   │   └── HealthController.java
│       │   │   ├── domain/
│       │   │   │   ├── entity/
│       │   │   │   │   ├── PortfolioItem.java     # @Entity JPA
│       │   │   │   │   ├── Alert.java             # @Entity JPA con isActive()
│       │   │   │   │   ├── IpoWatchlistItem.java  # @Entity + lockupRemainingDays()
│       │   │   │   │   ├── WatchlistItem.java     # @Entity ticker + targetPrice + note
│       │   │   │   │   ├── SimWallet.java         # @Entity wallet virtuale (singola riga "default")
│       │   │   │   │   ├── SimPosition.java       # @Entity posizione simulata, avgPrice ponderato
│       │   │   │   │   ├── SimTrade.java          # @Entity storico append-only BUY/SELL
│       │   │   │   │   ├── BankTransaction.java   # @Entity movimento importato da estratto conto
│       │   │   │   │   ├── FixedExpense.java      # @Entity costo fisso mensile
│       │   │   │   │   └── InvestorProfile.java   # @Entity profilo da questionario (singola riga)
│       │   │   │   └── enums/
│       │   │   │       └── AlertType.java         # ABOVE|BELOW|CHANGE_UP|CHANGE_DOWN
│       │   │   ├── dto/                           # Java records immutabili
│       │   │   │   ├── quote/    QuoteDto, FullQuoteDto, HistoryPoint
│       │   │   │   ├── portfolio/ AddPortfolioItemRequest, PortfolioItemDto
│       │   │   │   ├── alert/    AddAlertRequest, FireAlertRequest, AlertDto, AlertsResponse
│       │   │   │   ├── ipo/      UpcomingIpoDto, RecentIpoDto, IpoWatchlistItemDto,
│       │   │   │   │             AddIpoWatchlistRequest, UpdateIpoWatchlistRequest
│       │   │   │   ├── search/   SearchResultDto
│       │   │   │   ├── watchlist/ AddWatchlistRequest, WatchlistItemDto
│       │   │   │   ├── screener/ ScreenerDto
│       │   │   │   ├── benchmark/ BenchmarkDto
│       │   │   │   ├── correlation/ CorrelationDto
│       │   │   │   ├── dividend/ DividendDto
│       │   │   │   ├── earnings/ EarningsDto
│       │   │   │   ├── news/     NewsItemDto
│       │   │   │   ├── simulator/ SimWalletDto, SimSummaryDto, SimTradeDto, BuyRequest, SellRequest, ResetRequest
│       │   │   │   ├── finance/  TransactionDto, TransactionUpdateRequest, TransactionCategoriesDto,
│       │   │   │   │             FixedExpenseDto, FixedExpenseRequest, BudgetDto, MonthlyExpensesDto,
│       │   │   │   │             StatementUploadResultDto, DeleteCountDto, InvestorProfileDto,
│       │   │   │   │             QuestionnaireRequest, RecommendationDto, AllocationDto
│       │   │   │   ├── finance/mortgage/ MortgageRequest, MortgageSimulationDto, LoanRequest,
│       │   │   │   │             LoanSimulationDto, AmortizationYearDto, IncomeEstimateDto,
│       │   │   │   │             PensionFundAdviceDto, BudgetAdviceDto, HomeSaleRequest,
│       │   │   │   │             HomeSaleAdviceDto
│       │   │   │   ├── analytics/ (DTO condivisi metriche)
│       │   │   │   └── ai/       ChatRequest, ChatMessage
│       │   │   ├── exception/
│       │   │   │   ├── FinaiException.java        # Eccezione con statusCode HTTP
│       │   │   │   └── GlobalExceptionHandler.java # @RestControllerAdvice → JSON uniforme
│       │   │   ├── repository/
│       │   │   │   ├── PortfolioRepository.java   # JPA + @Modifying per update bulk
│       │   │   │   ├── AlertRepository.java
│       │   │   │   ├── IpoWatchlistRepository.java
│       │   │   │   ├── WatchlistRepository.java
│       │   │   │   ├── SimWalletRepository.java
│       │   │   │   ├── SimPositionRepository.java
│       │   │   │   ├── SimTradeRepository.java
│       │   │   │   ├── BankTransactionRepository.java
│       │   │   │   ├── FixedExpenseRepository.java
│       │   │   │   └── InvestorProfileRepository.java
│       │   │   └── service/
│       │   │       ├── YahooFinanceService.java   # @Retry + @CircuitBreaker + @Cacheable
│       │   │       ├── YahooCrumbProvider.java     # gestione crumb/cookie Yahoo
│       │   │       ├── NasdaqService.java         # @Retry + @CircuitBreaker + @Cacheable
│       │   │       ├── AnthropicService.java      # SSE streaming + buildSystemPrompt()
│       │   │       ├── IndicatorsService.java     # RSI, SMA, volatilità, momentum, BullScore
│       │   │       ├── PortfolioService.java      # CRUD + refresh bulk prezzi
│       │   │       ├── PortfolioAnalyticsService.java # metriche derivate portafoglio
│       │   │       ├── PortfolioBuilderService.java   # costruzione portafogli modello (Suggeriti)
│       │   │       ├── AlertService.java          # CRUD + fire idempotente
│       │   │       ├── IpoService.java            # calendario + watchlist CRUD
│       │   │       ├── WatchlistService.java      # CRUD watchlist, dedup ticker
│       │   │       ├── ScreenerService.java       # filtri su universe fisso ~85 ticker, @Cacheable
│       │   │       ├── BenchmarkService.java      # performance pesata vs S&P 500 (^GSPC), @Cacheable
│       │   │       ├── CorrelationService.java    # matrice Pearson su ritorni giornalieri, @Cacheable
│       │   │       ├── DividendService.java       # rendimento/ex-date per ticker
│       │   │       ├── EarningsService.java       # date/stime EPS per ticker
│       │   │       ├── NewsService.java           # news Yahoo Finance per ticker
│       │   │       ├── SimulatorService.java       # wallet virtuale, buy/sell, P&L, reset
│       │   │       ├── FinanceService.java        # orchestrazione modulo finanza personale
│       │   │       ├── StatementParserService.java # parsing PDF (PDFBox) ed Excel (Apache POI)
│       │   │       ├── TransactionCategorizer.java # categorizzazione per keyword (14+5 categorie)
│       │   │       ├── BudgetService.java         # budget previsionale mese successivo
│       │   │       ├── InvestmentAdvisorService.java # motore a regole goal+horizon → allocazione
│       │   │       └── MortgageService.java       # rata alla francese, LTV, rata/reddito, stress test
│       │   └── resources/
│       │       ├── application.yml               # Config principale
│       │       ├── application-test.yml          # Override per test (PostgreSQL test DB)
│       │       └── db/migration/
│       │           ├── V1__create_tables.sql     # Schema iniziale (3 tabelle)
│       │           ├── V2__add_indexes.sql       # Indici per query frequenti
│       │           ├── V3__fix_alert_type_constraint.sql
│       │           ├── V4__create_watchlist.sql  # watchlist_items
│       │           ├── V5__create_simulation.sql # sim_wallet, sim_positions, sim_trades
│       │           └── V6__create_personal_finance.sql # bank_transactions, fixed_expenses, investor_profile
│       └── test/
│           └── java/com/finai/
│               ├── service/
│               │   ├── IndicatorsServiceTest.java   # 24 test unit (RSI, SMA, vol, score)
│               │   ├── PortfolioServiceTest.java    # 7 test unit (Mockito)
│               │   ├── AlertServiceTest.java        # 7 test unit (Mockito)
│               │   ├── IpoServiceTest.java          # 6 test unit (Mockito)
│               │   └── AnthropicServiceTest.java    # 6 test unit (system prompt)
│               ├── controller/
│               │   ├── PortfolioControllerTest.java # 7 test funzionali (MockMvc)
│               │   └── AlertControllerTest.java     # 6 test funzionali (MockMvc)
│               └── integration/
│                   ├── PortfolioIntegrationTest.java # 7 test integrazione (PostgreSQL)
│                   ├── AlertIntegrationTest.java     # 6 test integrazione (PostgreSQL)
│                   └── IpoIntegrationTest.java       # 5 test integrazione (PostgreSQL)
│
└── frontend/
    ├── package.json
    ├── tsconfig.json             # bundler + noEmit + strict
    ├── vite.config.ts            # proxy /api → :3001, alias @/ → src/
    ├── index.html
    └── src/
        ├── main.tsx
        ├── App.tsx               # QueryClientProvider + lazy tab router (16 tab)
        ├── styles.css
        ├── lib/
        │   ├── constants.ts      # STOCK_UNIVERSE (160+ ticker geografici), ETF, INDICES
        │   ├── formatters.ts
        │   └── indicators.ts     # RSI/SMA/EMA/MACD/Bollinger (lato client, per grafici)
        ├── store/
        │   ├── useAppStore.ts
        │   └── useChatStore.ts
        ├── hooks/
        │   ├── useQuote.ts       useFullQuote.ts
        │   ├── useHistory.ts
        │   ├── useMarketBatch.ts
        │   ├── useChat.ts
        │   ├── useAlerts.ts
        │   ├── usePortfolio.ts   # React Query CRUD portafoglio
        │   ├── useAlertsBackend.ts
        │   ├── useSearch.ts      # debounce 300ms + AbortController
        │   ├── useIPO.ts
        │   ├── useWatchlist.ts   # React Query CRUD watchlist
        │   ├── useScreener.ts
        │   ├── useSimulator.ts   # React Query wallet/summary/trades + buy/sell/reset
        │   ├── useFinance.ts     # React Query estratti, spese fisse, budget, questionario
        │   └── useMortgage.ts    # React Query stima reddito + simulazione mutuo/finanziamento
        ├── components/
        │   ├── layout/   Header, NavTabs (16 tab), PandaLoader
        │   ├── common/   SearchInput (autocomplete)
        │   ├── market/   IndexBar, SentimentMeter, MarketGrid, MarketRow
        │   ├── analyze/  StockHero, PriceChart, SignalBadge, PredictionCard
        │   ├── portfolio/ CorrelationHeatmap e altri widget portafoglio
        │   ├── finance/  StatementUpload, FixedExpensesManager, BudgetSummary,
        │   │             ExpensesPieChart, QuestionnaireWizard
        │   ├── mortgage/ MortgageCalculator, LoanCalculator
        │   └── chat/     ChatPanel, ChatMessage
        └── pages/
            ├── MarketPage, AnalyzePage, ComparePage, AlertsPage
            ├── LongTermPage, PortfolioPage, IPOPage
            ├── SuggestedPage, ScreenerPage, WatchlistPage, MacroPage
            ├── SimulatorPage, PersonalFinancePage, MortgagePage, FinancingPage, GuidePage
```

---

## Installazione e avvio

### Opzione A — Docker (consigliata, zero prerequisiti)

L'unico requisito è **Docker Desktop** installato.

```bash
git clone https://github.com/giole89/TestClaude.git
cd TestClaude/finai
git checkout claude/finai-web-app-I6tE2

# 1. Crea il file .env con la tua API key Anthropic
cp .env.example .env
# Modifica .env e inserisci: ANTHROPIC_API_KEY=sk-ant-api03-...

# 2. Avvia tutto (prima volta: ~3-5 minuti per la build)
docker compose up -d --build

# 3. Apri il browser
#    http://localhost:5173
```

Comandi utili:

```bash
docker compose logs -f          # Segui i log in tempo reale
docker compose down             # Ferma tutto (dati preservati)
docker compose down -v          # Ferma tutto e cancella il DB
docker compose up -d            # Riavvia (senza rebuild)
docker compose up -d --build    # Riavvia con rebuild delle immagini
```

---

### Opzione B — Manuale (Java + Node + PostgreSQL nativi)

#### Prerequisiti

| Tool | Versione minima | Note |
|------|-----------------|------|
| **Java JDK** | 21 | OpenJDK o Oracle JDK 21 LTS |
| **Apache Maven** | 3.9 | `mvn -version` |
| **Node.js** | 18 | `node -v` |
| **PostgreSQL** | 16 | nativo o via `docker compose up -d postgres` |
| **API key Anthropic** | — | Obbligatoria per la chat AI |

#### 1. Clona il repository

```bash
git clone https://github.com/giole89/TestClaude.git
cd TestClaude/finai
git checkout claude/finai-web-app-I6tE2
```

#### 2. Crea il database

```bash
# Con Docker (solo PostgreSQL)
docker compose up -d postgres

# oppure con PostgreSQL nativo
sudo -u postgres psql -c "CREATE USER finai WITH PASSWORD 'finai';"
sudo -u postgres psql -c "CREATE DATABASE finai OWNER finai;"
```

#### 3. Configura le variabili d'ambiente

```bash
cp backend/.env.example backend/.env
```

Modifica `backend/.env`:

```env
ANTHROPIC_API_KEY=sk-ant-api03-...    # Obbligatoria
PORT=3001
DB_URL=jdbc:postgresql://localhost:5432/finai
DB_USER=finai
DB_PASSWORD=finai
AI_MODEL=claude-haiku-4-5-20251001
```

> Flyway applica automaticamente le migrazioni SQL al primo avvio. Non serve nessun setup manuale dello schema.

#### 4. Avvia l'applicazione

```bash
# Terminale 1 — Backend Java
cd backend
mvn spring-boot:run

# Terminale 2 — Frontend
cd frontend
npm install
npm run dev
```

### URL applicazione

```
http://localhost:5173                    # App principale
http://localhost:3001/swagger-ui.html   # Swagger UI (documentazione API)
http://localhost:3001/health            # Health check
```

Il dev server Vite fa da proxy per tutte le richieste `/api/*` verso il backend.

### Build di produzione

```bash
npm run build
```

- Backend: `mvn package -DskipTests` → `backend/target/finai-backend-1.0.0.jar`
- Frontend: `vite build` → `frontend/dist/`

Per avviare in produzione:

```bash
# Backend (jar eseguibile, include Tomcat embedded)
java -jar backend/target/finai-backend-1.0.0.jar

# Frontend (servire con nginx, Caddy, serve, ecc.)
npx serve frontend/dist -p 80
```

### Risoluzione problemi comuni

| Problema | Causa | Soluzione |
|----------|-------|-----------|
| `Connection refused: localhost:5432` | PostgreSQL non avviato | `npm run db:up` o `systemctl start postgresql` |
| `FlywayException: Schema not empty` | DB esistente con schema diverso | In dev: `DROP DATABASE finai; CREATE DATABASE finai OWNER finai;` |
| `ANTHROPIC_API_KEY not set` | `.env` mancante | Copia `.env.example` e inserisci la chiave |
| `Port 3001 already in use` | Processo in conflitto | Cambia `PORT` nel `.env` |
| IPO upcoming vuoti | NASDAQ API irraggiungibile | Normale: circuit breaker attivo, ritenta dopo 60s |
| Build Maven lenta (prima volta) | Download dipendenze Maven | Successivi avvii: cache locale Maven in `~/.m2` |

---

## Variabili d'ambiente

### Backend (`backend/.env`)

| Variabile | Default | Obbl. | Descrizione |
|-----------|---------|:-----:|-------------|
| `ANTHROPIC_API_KEY` | — | ✅ | Chiave API Anthropic per la chat AI |
| `PORT` | `3001` | | Porta del server Tomcat embedded |
| `DB_URL` | `jdbc:postgresql://localhost:5432/finai` | | URL JDBC PostgreSQL |
| `DB_USER` | `finai` | | Username database |
| `DB_PASSWORD` | `finai` | | Password database |
| `AI_MODEL` | `claude-haiku-4-5-20251001` | | Modello Claude (haiku/sonnet/opus) |

### Frontend (`frontend/.env`)

| Variabile | Default | Descrizione |
|-----------|---------|-------------|
| `VITE_API_BASE_URL` | `http://localhost:3001` | URL backend (in produzione punta al server remoto) |

### Test (`application-test.yml`)

| Variabile | Default | Descrizione |
|-----------|---------|-------------|
| `TEST_DB_URL` | `jdbc:postgresql://127.0.0.1:5432/finai_test` | DB dedicato per i test |
| `TEST_DB_USER` | `finai` | Utente DB di test |
| `TEST_DB_PASSWORD` | `finai` | Password DB di test |

---

## API Backend

Tutti gli endpoint sono documentati con Swagger UI su `/swagger-ui.html`. Rate limiter: **100 req/min** per IP, **20 req/min** per `/api/ai/*`.

### Quote e mercato

| Endpoint | Descrizione | Cache |
|----------|-------------|-------|
| `GET /api/quote/:ticker` | Quote base | 60s |
| `GET /api/quote/:ticker/full` | Quote + indicatori + storia 1y | 60s |
| `GET /api/batch?tickers=A,B,C` | Quote multiple in parallelo | 120s |
| `GET /api/history/:ticker?range=1y` | Storico OHLCV (`1m/3m/6m/1y`) | 4h |
| `GET /api/indices` | 9 indici globali (S&P500, NDX, VIX…) | 120s |
| `GET /api/search?q=apple` | Autocomplete ticker/nome | 30s |

### Portfolio

| Endpoint | Descrizione |
|----------|-------------|
| `GET /api/portfolio` | Lista posizioni |
| `POST /api/portfolio` | Aggiunge posizione (body: `{id, ticker, name, qty, loadPrice, currency}`) |
| `DELETE /api/portfolio/:id` | Rimuove posizione |
| `POST /api/portfolio/refresh` | Aggiorna tutti i prezzi correnti da Yahoo Finance |

### Alert

| Endpoint | Descrizione |
|----------|-------------|
| `GET /api/alerts` | Risposta `{active: [], history: []}` |
| `POST /api/alerts` | Crea alert (body: `{id, ticker, type, value}`) |
| `DELETE /api/alerts/:id` | Elimina alert |
| `POST /api/alerts/:id/fire` | Segna scattato (body: `{price}`) — idempotente |

Tipi: `above` | `below` | `change_up` | `change_down`

### IPO

| Endpoint | Descrizione |
|----------|-------------|
| `GET /api/ipo/upcoming` | Prossime IPO (NASDAQ calendar, cache 1h) |
| `GET /api/ipo/recent` | IPO recenti con performance |
| `GET /api/ipo/watchlist` | Watchlist personale |
| `POST /api/ipo/watchlist` | Aggiunge a watchlist |
| `PATCH /api/ipo/watchlist/:id` | Aggiornamento parziale (patch semantics) |
| `DELETE /api/ipo/watchlist/:id` | Rimuove dalla watchlist |

### AI Chat

```
POST /api/ai/chat
Content-Type: application/json

{
  "messages": [{"role": "user", "content": "Analizza AAPL"}],
  "context": {
    "tab": "analyze",
    "ticker": "AAPL",
    "tickerData": {"price": 190.0, "rsi": 58, "bullScore": 85}
  }
}
```

Risposta SSE:
```
data: {"text":"Apple Inc. è in una fase..."}
data: {"text":" rialzista di medio termine."}
data: [DONE]
```

Tab supportate nel context: `analyze` | `compare` | `portfolio` | `longterm` | `ipo` | `market`

### Watchlist

| Endpoint | Descrizione |
|----------|-------------|
| `GET /api/watchlist` | Lista elementi watchlist |
| `POST /api/watchlist` | Aggiunge ticker (body: `{id, ticker, name, targetPrice, note}`), 409 se duplicato |
| `DELETE /api/watchlist/:id` | Rimuove elemento, 404 se non trovato |

### Screener

| Endpoint | Descrizione |
|----------|-------------|
| `GET /api/screener?minPE=&maxPE=&minYield=&minYtd=&market=&limit=` | Screener su universe fisso (~85 ticker); `market` in `us\|it\|de\|fr\|null`; `limit` default 50, max 100. Cache 120s |

### Benchmark, Correlazione, Dividendi, Earnings, News (Portafoglio)

| Endpoint | Descrizione | Cache |
|----------|-------------|-------|
| `GET /api/portfolio/benchmark?period=1y` | Performance portafoglio vs S&P 500 (`3m\|6m\|1y\|3y`), calcolo alpha | sì |
| `GET /api/portfolio/correlation` | Matrice di correlazione Pearson sui ritorni giornalieri (max 8 ticker per market value) | sì |
| `GET /api/dividends?tickers=A,B,C` | Dividendi (yield, ex-date, income annuale stimato) solo per ticker con dividendo positivo, max 30 | — |
| `GET /api/earnings?tickers=A,B,C` | Date e stime EPS earnings, max 30 ticker | — |
| `GET /api/news?tickers=A,B,C&count=10` | News Yahoo Finance per ticker, max 10 ticker, `count` 1-20 | — |

### Simulatore (paper trading)

| Endpoint | Descrizione |
|----------|-------------|
| `GET /api/sim/wallet` | Stato grezzo wallet (liquidità + capitale iniziale) |
| `GET /api/sim/summary` | Riepilogo: liquidità, posizioni valorizzate a prezzo live, P&L totale |
| `GET /api/sim/trades` | Storico operazioni, più recenti prime |
| `POST /api/sim/buy` | Acquista titolo a prezzo live (body: `{ticker, qty}`) |
| `POST /api/sim/sell` | Vende posizione, parziale o totale (body: `{ticker, qty}`) |
| `POST /api/sim/reset` | Azzera simulazione e ripristina capitale iniziale (body opzionale: `{startingBalance}`) |

### Finanza Personale

| Endpoint | Descrizione |
|----------|-------------|
| `POST /api/finance/statements/upload` | Importa estratto conto PDF/XLSX/XLS (multipart, campo `file`) → categorizzazione automatica |
| `GET /api/finance/transactions` | Elenco movimenti importati, più recenti prime |
| `GET /api/finance/transactions/categories` | Categorie note per il menu di correzione manuale |
| `PUT /api/finance/transactions/:id` | Corregge categoria/tipo di un movimento |
| `DELETE /api/finance/transactions/:id` | Elimina un movimento |
| `DELETE /api/finance/transactions` | Elimina più movimenti (body: lista di id) |
| `DELETE /api/finance/transactions/all` | Elimina tutti i movimenti |
| `GET /api/finance/fixed-expenses` | Elenco spese fisse mensili |
| `POST /api/finance/fixed-expenses` | Crea spesa fissa (body: `{name, category, amount, active?, interestRatePct?}`); `interestRatePct` se è la rata di un debito/finanziamento |
| `PUT /api/finance/fixed-expenses/:id` | Aggiorna spesa fissa |
| `DELETE /api/finance/fixed-expenses/:id` | Elimina spesa fissa |
| `GET /api/finance/budget/next-month` | Budget previsionale mese successivo (entrate, costi fissi/variabili, saldo previsto `balance` anche negativo, quota investibile `investableAmount` ≥ 0, flag `deficit`) |
| `GET /api/finance/expenses/current-month` | Spese variabili del mese corrente per categoria (pie chart) |
| `GET /api/finance/insights` | Analizza i movimenti reali e produce suggerimenti di risparmio (categorie sovrappesate, regola 50/30/20, ricorrenze, trend) con stima del risparmio mensile potenziale |
| `GET /api/finance/questionnaire` | Stato del questionario investitore |
| `POST /api/finance/questionnaire` | Invia risposte (body: `{goal, goalNote, horizon, liquidSavings?}`) → consiglio di investimento; `liquidSavings` opzionale, usata per il controllo del fondo di emergenza |
| `GET /api/finance/recommendation` | Consiglio basato sull'ultimo questionario completato: allocazione, portafoglio esempio in ETF reali, avviso fondo di emergenza/debiti ad alto interesse se rilevanti, suggerimento PAC |

> Il budget previsionale si basa sulla media degli ultimi mesi storici **completi** che contengono almeno un'entrata; se non ce n'è ancora nessuno (storico vuoto o solo movimenti isolati senza entrate), la stima ricade sul mese in corso e la risposta segnala `basedOnCurrentMonthOnly: true`.
>
> Il consiglio di investimento segnala `emergencyFundWarning` se la liquidità dichiarata copre meno di 3 mesi di spese, e `highInterestDebtWarning` se tra le spese fisse c'è un debito con tasso ≥ 6%/anno: in entrambi i casi, sistemare la propria situazione finanziaria di base ha priorità rispetto a investire la quota disponibile.

### Mutui e Finanziamenti

| Endpoint | Descrizione |
|----------|-------------|
| `GET /api/finance/mortgage/income-estimate` | Reddito netto mensile stimato dal budget, per precompilare il calcolatore |
| `POST /api/finance/mortgage/simulate` | Simula un mutuo (body: `{propertyValue, loanAmount, interestRatePct, years, monthlyNetIncome?, purchaseType?, notaryCosts?, originationFees?, appraisalFees?, agencyFeePct?, agencyFeeAmount?, registrationTax?, liquidSavings?, pensionFundYears?, pensionFundBalance?, homeSale?}`) → rata, LTV, rapporto rata/reddito, stress test, capitale proprio, spese accessorie, capitale disponibile (incluso da un'eventuale vendita), fabbisogno residuo, idoneità fondo pensione, consigli di budget, piano di ammortamento |
| `POST /api/finance/loan/simulate` | Simula un finanziamento/prestito personale (body: `{loanAmount, interestRatePct, months, monthlyNetIncome?}`) → rata, rapporto rata/reddito, piano di ammortamento |

> Rata calcolata con piano di ammortamento **alla francese**: `R = C · i / (1 - (1+i)⁻ⁿ)`, con `i` tasso mensile e `n` numero di rate. Il rapporto rata/reddito è **complessivo**: somma la nuova rata alle rate di altri debiti già tra le spese fisse (quelli con `interestRatePct` valorizzato), non la rata isolata. Senza `monthlyNetIncome` dichiarato, viene usata la stima del budget (`incomeEstimated: true` nella risposta); se nessuna delle due è disponibile, l'endpoint risponde 422.
>
> Per il mutuo, `notaryCosts`/`originationFees`/`appraisalFees`/`registrationTax` sono opzionali: se non dichiarati, vengono stimati (notaio ~2% dell'immobile, istruttoria ~0.5% del mutuo, perizia 300€ flat; l'imposta di registro/IVA dipende da `purchaseType` — `PRIMA_CASA_PRIVATO` 2% min 1.000€, `PRIMA_CASA_COSTRUTTORE` IVA 4%+600€, `SECONDA_CASA_PRIVATO` 9% min 1.000€, `SECONDA_CASA_COSTRUTTORE` IVA 10%+600€ — stima approssimata sul prezzo dichiarato, non sul valore catastale). Per l'agenzia, `agencyFeePct` (percentuale, ha priorità: l'IVA al 22% viene aggiunta automaticamente) o `agencyFeeAmount` (importo finale in euro, nessuna IVA aggiuntiva); senza nessuno dei due, stima 3%+IVA. `totalOutOfPocketCost` (capitale proprio + spese accessorie) è confrontato con `totalAvailableCapital` (liquidità dichiarata o dal profilo investitore, più l'eventuale capitale netto da `homeSale`) per calcolare `shortfall`. Se `pensionFundYears` è dichiarato, `pensionFund` verifica l'idoneità all'anticipazione fondo pensione per prima casa (richiede almeno 8 anni di iscrizione, D.Lgs. 252/2005, mai ammessa per seconda casa) e stima l'importo anticipabile (75% del `pensionFundBalance`, se dichiarato). `budgetAdvice` elenca in ordine le fonti a cui attingere per il fabbisogno residuo (liquidità, vendita di un'altra casa se dichiarata, fondo pensione se idoneo, risparmio mensile stimato dal budget, altre opzioni).
>
> `homeSale` (opzionale, body: `{saleValue, purchasePrice, yearsOwned, mainResidence?, residualMortgageBalance?, saleAgencyFees?, monthsUntilSale?}`) stima il capitale disponibile dalla vendita di un'altra casa: la plusvalenza (`saleValue - purchasePrice`) è tassata con imposta sostitutiva del 26% solo se `yearsOwned < 5` e `mainResidence` non è true (art. 67 TUIR), altrimenti è esente; `netProceeds = saleValue - saleAgencyFees - residualMortgageBalance - capitalGainsTax` (può essere negativo, segnalato esplicitamente); `monthsUntilSale`, se dichiarato, produce una nota di confronto con il tempo medio di vendita in Italia (~6 mesi).

---

## Persistenza dati — PostgreSQL

Portfolio, alert, watchlist IPO/personale, simulazione e finanza personale sono persistiti su **PostgreSQL 16** via Spring Data JPA.
Le migrazioni sono gestite da **Flyway** con versioning incrementale (9 tabelle totali, V1-V7; V7 aggiunge `liquid_savings` a `investor_profile` e `interest_rate_pct` a `fixed_expenses` per i controlli pre-investimento).

### Schema

```sql
-- portfolio_items: posizioni del portafoglio con prezzo di carico e corrente
CREATE TABLE portfolio_items (
    id             VARCHAR(36)      PRIMARY KEY,   -- UUID v4 generato dal frontend
    ticker         VARCHAR(20)      NOT NULL,
    name           TEXT             NOT NULL,
    qty            NUMERIC(18,6)    NOT NULL CHECK (qty > 0),
    load_price     NUMERIC(18,4)    NOT NULL CHECK (load_price > 0),
    current_price  NUMERIC(18,4),                 -- aggiornato da /api/portfolio/refresh
    currency       VARCHAR(10)      NOT NULL DEFAULT 'USD',
    created_at     TIMESTAMPTZ      NOT NULL DEFAULT NOW()
);

-- alerts: alert sui prezzi con ciclo di vita active → history
CREATE TABLE alerts (
    id           VARCHAR(36)    PRIMARY KEY,
    ticker       VARCHAR(20)    NOT NULL,
    type         VARCHAR(20)    NOT NULL              -- ABOVE|BELOW|CHANGE_UP|CHANGE_DOWN
                     CHECK (type IN ('ABOVE','BELOW','CHANGE_UP','CHANGE_DOWN')),
    value        NUMERIC(18,4)  NOT NULL CHECK (value > 0),
    fired_at     TIMESTAMPTZ,                         -- NULL = alert attivo
    fired_price  NUMERIC(18,4),
    created_at   TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

-- ipo_watchlist: IPO monitorate con calcolo lock-up
CREATE TABLE ipo_watchlist (
    id             VARCHAR(36)    PRIMARY KEY,
    ticker         VARCHAR(20),
    company_name   TEXT           NOT NULL,
    expected_date  DATE,
    exchange       VARCHAR(50),
    sector         VARCHAR(100),
    lockup_days    INTEGER        NOT NULL DEFAULT 180 CHECK (lockup_days > 0),
    ipo_price      NUMERIC(18,4)  CHECK (ipo_price > 0),
    ipo_date       DATE,                              -- aggiornato post-quotazione
    notes          TEXT,
    created_at     TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

-- watchlist_items: watchlist personale ticker con target price opzionale
CREATE TABLE watchlist_items (
    id           VARCHAR(50)     PRIMARY KEY,
    ticker       VARCHAR(20)     NOT NULL UNIQUE,
    name         VARCHAR(255),
    target_price DECIMAL(12, 4),
    note         TEXT,
    created_at   BIGINT          NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW()) * 1000
);

-- sim_wallet: riga singola ("default") con la liquidità virtuale del paper trading
CREATE TABLE sim_wallet (
    id                VARCHAR(20)     PRIMARY KEY,
    cash_balance      NUMERIC(18, 4)  NOT NULL CHECK (cash_balance >= 0),
    starting_balance  NUMERIC(18, 4)  NOT NULL CHECK (starting_balance > 0),
    created_at        TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    reset_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- sim_positions: posizioni aperte nel portafoglio simulato (prezzo medio ponderato)
CREATE TABLE sim_positions (
    id          VARCHAR(36)     PRIMARY KEY,
    ticker      VARCHAR(20)     NOT NULL UNIQUE,
    name        TEXT            NOT NULL,
    qty         NUMERIC(18, 6)  NOT NULL CHECK (qty > 0),
    avg_price   NUMERIC(18, 4)  NOT NULL CHECK (avg_price > 0),
    currency    VARCHAR(10)     NOT NULL DEFAULT 'USD',
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- sim_trades: storico append-only delle operazioni simulate BUY/SELL
CREATE TABLE sim_trades (
    id           VARCHAR(36)     PRIMARY KEY,
    ticker       VARCHAR(20)     NOT NULL,
    name         TEXT            NOT NULL,
    side         VARCHAR(10)     NOT NULL CHECK (side IN ('BUY', 'SELL')),
    qty          NUMERIC(18, 6)  NOT NULL CHECK (qty > 0),
    price        NUMERIC(18, 4)  NOT NULL CHECK (price > 0),
    amount       NUMERIC(18, 4)  NOT NULL,
    realized_pnl NUMERIC(18, 4),                  -- popolato solo per SELL
    currency     VARCHAR(10)     NOT NULL DEFAULT 'USD',
    executed_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- bank_transactions: movimenti importati da estratto conto (PDF/Excel)
CREATE TABLE bank_transactions (
    id            VARCHAR(36) PRIMARY KEY,
    tx_date       DATE NOT NULL,
    description   VARCHAR(500) NOT NULL,
    amount        NUMERIC(14,2) NOT NULL,         -- negativo = uscita, positivo = entrata
    category      VARCHAR(50) NOT NULL,
    type          VARCHAR(20) NOT NULL CHECK (type IN ('INCOME', 'VARIABLE_EXPENSE')),
    source_file   VARCHAR(255),
    imported_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- fixed_expenses: costi fissi mensili inseriti manualmente (base del budget)
CREATE TABLE fixed_expenses (
    id                 VARCHAR(36) PRIMARY KEY,
    name               VARCHAR(100) NOT NULL,
    category           VARCHAR(50) NOT NULL,
    amount             NUMERIC(14,2) NOT NULL,
    active             BOOLEAN NOT NULL DEFAULT TRUE,
    interest_rate_pct  NUMERIC(5,2),              -- tasso annuo (%) se è la rata di un debito/finanziamento (V7)
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- investor_profile: profilo da questionario (singola riga "default", no multi-utente)
CREATE TABLE investor_profile (
    id              VARCHAR(20) PRIMARY KEY DEFAULT 'default',
    goal            VARCHAR(50),                 -- EMERGENCY|MAJOR_PURCHASE|RETIREMENT|GROWTH|OTHER
    goal_note       VARCHAR(255),
    horizon         VARCHAR(30),                 -- UNDER_1Y|Y1_3|Y3_5|Y5_10|OVER_10Y
    liquid_savings  NUMERIC(14,2),                -- liquidità accantonata, per il controllo del fondo di emergenza (V7)
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

### Migrazioni Flyway

| Versione | File | Descrizione |
|----------|------|-------------|
| V1 | `V1__create_tables.sql` | Schema iniziale (3 tabelle) |
| V2 | `V2__add_indexes.sql` | Indici su ticker, fired_at, expected_date |
| V3 | `V3__fix_alert_type_constraint.sql` | Constraint alert_type in uppercase |
| V4 | `V4__create_watchlist.sql` | Tabella `watchlist_items` |
| V5 | `V5__create_simulation.sql` | Tabelle `sim_wallet` (seed 100.000€), `sim_positions`, `sim_trades` |
| V6 | `V6__create_personal_finance.sql` | Tabelle `bank_transactions`, `fixed_expenses`, `investor_profile` (seed riga "default") |
| V7 | `V7__finance_savings_and_debt.sql` | Aggiunge `liquid_savings` a `investor_profile` e `interest_rate_pct` a `fixed_expenses` |

---

## Resilienza — Retry e Circuit Breaker

Tutte le chiamate a Yahoo Finance e NASDAQ sono protette da Resilience4j (`backend/src/main/resources/application.yml`):

### Retry con backoff esponenziale

```
Tentativo 1 → attende 2s → Tentativo 2 → attende 4s → Tentativo 3 → errore propagato
```

- 3 tentativi, delay base 2000ms, moltiplicatore 2
- Solo per: `IOException`, `WebClientRequestException`

### Circuit Breaker (pattern sliding window)

```
CHIUSO (normale)
  ↓ 50% errori su 10 chiamate
APERTO (blocca per 60s → ritorna null o lista vuota)
  ↓ dopo 60s
SEMI-APERTO (lascia passare 3 chiamate di test)
  ↓ successo
CHIUSO
```

I fallback methods restituiscono `null` o liste vuote — il frontend mostra l'ultimo dato in cache o un messaggio "dati non disponibili".

---

## Test

### Struttura e copertura

```
153 test totali — tutti verdi — JaCoCo coverage ≥ 70%

Service tests (137 test unit — Mockito, zero dipendenze esterne)
├── IndicatorsServiceTest          24 test (RSI, SMA, volatilità, momentum, BullScore, rangePos)
├── PortfolioServiceTest            7 test
├── PortfolioAnalyticsServiceTest   6 test
├── AlertServiceTest                7 test
├── IpoServiceTest                  6 test
├── AnthropicServiceTest            6 test (buildSystemPrompt per ogni tab)
├── WatchlistServiceTest            8 test
├── CorrelationServiceTest         12 test (matrice Pearson, casi limite)
├── BenchmarkServiceTest            8 test (vs S&P 500, alpha)
├── BudgetServiceTest               5 test (media storica, fallback mese corrente)
├── FinanceServiceTest              8 test
├── TransactionCategorizerTest      8 test (categorizzazione per keyword)
├── StatementParserServiceTest      5 test (parsing PDF/Excel)
└── FiscalCalculationTest           8 test (gain fiscale 26%)

Controller tests (13 test funzionali — @WebMvcTest + MockMvc)
├── PortfolioControllerTest 7 test (200/201/400/404/409 HTTP status)
└── AlertControllerTest     6 test

Integration tests (22 test — Spring Boot + PostgreSQL reale)
├── PortfolioIntegrationTest 9 test (ciclo CRUD completo)
├── AlertIntegrationTest     7 test (creazione → fire → history)
└── IpoIntegrationTest       6 test (watchlist + lock-up calc)
```

### Eseguire i test

```bash
# Tutti i test (unitari + funzionali + integrazione)
npm run test:backend
# oppure:
cd backend && mvn test

# Solo unit test (rapidi, no DB)
cd backend && mvn test -Dtest="*ServiceTest,*ControllerTest"

# Solo integration test (richiedono PostgreSQL)
cd backend && mvn test -Dtest="*IntegrationTest"

# Report copertura JaCoCo
cd backend && mvn test && open target/site/jacoco/index.html
```

### Prerequisiti per i test di integrazione

Il profilo `test` si connette a `finai_test` su PostgreSQL locale. Setup:

```bash
sudo -u postgres psql -c "CREATE USER finai WITH PASSWORD 'finai';"
sudo -u postgres psql -c "CREATE DATABASE finai_test OWNER finai;"
```

> In ambienti CI con Docker disponibile, sostituire con Testcontainers aggiungendo
> `@Testcontainers`, `@Container PostgreSQLContainer` e `@DynamicPropertySource`
> ai test di integrazione.

---

## Indicatori tecnici implementati

Calcolati server-side in `IndicatorsService` e replicati client-side in `indicators.ts` (per grafici/scenari):

| Indicatore | Descrizione | Parametri |
|------------|-------------|-----------|
| **RSI** | Relative Strength Index (smoothing Wilder) | period = 14 |
| **SMA** | Simple Moving Average | period = 20, 50, 200 |
| **EMA** | Exponential Moving Average | period configurabile |
| **MACD** | Moving Average Convergence/Divergence | EMA12 - EMA26, signal EMA9 |
| **Bande di Bollinger** | Upper/Middle/Lower band | period = 20, σ = 2 |
| **Volatilità** | σ log-return annualizzata (`√252 × σ_daily`) | finestra = tutti i giorni |
| **Momentum** | Variazione % su N giorni | days = 30 |
| **BullScore** | Score composito 0–100 | 7 criteri pesati |
| **LongTermScore** | Idoneità DCA 0–100 | 7 criteri |

### BullScore — criteri e pesi

```
Prezzo > SMA200   → +25pt   trend primario rialzista
Prezzo > SMA50    → +20pt   trend intermedio
Prezzo > SMA20    → +15pt   trend breve
RSI ∈ [50, 70]    → +15pt   forza senza ipercomprato
Momentum30 > 0    → +10pt   slancio positivo
Volatilità < 30%  → +10pt   stabilità
RangePos52W > 50% →  +5pt   vicino ai massimi annuali
                  ──────────
                    100pt max
```

---

## Sistema di caching

Il backend usa Caffeine con 5 tier indipendenti, tutti con eviction LRU:

```
┌──────────────┬─────────┬──────────────────────────────────────────────┐
│ Cache name   │   TTL   │ Motivazione                                  │
├──────────────┼─────────┼──────────────────────────────────────────────┤
│ quotes       │  60 sec │ Dati real-time: si aggiornano ogni minuto     │
│ batch        │ 120 sec │ Batch market: leggermente più stabili         │
│ history      │   4 ore │ Storico: non cambia durante la giornata       │
│ search       │  30 sec │ Autocomplete: freshness breve                 │
│ ipo          │   1 ora │ Calendario IPO: aggiornato raramente          │
└──────────────┴─────────┴──────────────────────────────────────────────┘
```

Il frontend ha un secondo layer di caching con **React Query** (`staleTime` allineati ai TTL del backend).

---

## Universo di strumenti supportati

FINAI include **160+ azioni** suddivise per area geografica più **28 ETF**:

| Area | Indice | Ticker inclusi |
|------|--------|----------------|
| **USA** | S&P 500 / Nasdaq | AAPL, MSFT, NVDA, AMZN, GOOGL, META, TSLA, BRK-B, JPM, V, MA, UNH, XOM, LLY, JNJ, AMD, INTC, QCOM, NFLX, DIS, BAC, WMT, HD, CVX, COST, ADBE, CRM, NOW, PANW, SNOW… (60 ticker) |
| **Germania** | DAX 40 | SAP.DE, SIE.DE, ALV.DE, BMW.DE, BAYN.DE, MBG.DE, ADS.DE, MUV2.DE, DTE.DE, EOAN.DE, BAS.DE, DBK.DE, VOW3.DE, RWE.DE… (27 ticker) |
| **Francia** | CAC 40 | MC.PA, OR.PA, SU.PA, AI.PA, KER.PA, RMS.PA, BNP.PA, SAN.PA, TTE.PA, AIR.PA, CS.PA, BN.PA, DG.PA, ACA.PA, STMPA.PA… (25 ticker) |
| **Italia** | FTSE MIB | ISP.MI, ENI.MI, RACE.MI, MONC.MI, LDO.MI, ENEL.MI, UCG.MI, TIT.MI, STM.MI, G.MI, BAMI.MI… (20 ticker) |
| **Paesi Bassi** | AEX | ASML.AS, INGA.AS, ADYEN.AS, BESI.AS, UNA.AS, HEIA.AS, RDSA.AS, NN.AS… (11 ticker) |
| **Spagna** | IBEX 35 | ITX.MC, IBE.MC, SAN.MC, BBVA.MC, REP.MC, TEF.MC, AMS.MC… (11 ticker) |
| **Svizzera** | SMI | NESN.SW, ROG.SW, NOVN.SW, ABBN.SW, ZURN.SW, UBSG.SW (6 ticker) |

**ETF**: globali UCITS (VWCE.DE, IWDA.AS, EQQQ.AS), USA (SPY, QQQ, VTI, SCHD), obbligazionari (AGGH.AS, TLT), tematici (AI, clean energy, robotica).

Qualsiasi ticker Yahoo Finance può essere cercato con l'autocomplete nelle tab Analisi, Confronto e Lungo Termine.

---

## Tema e personalizzazione

Dark mode (default) e light mode, commutabili dall'header. Il tema è persistito in `localStorage` e applicato prima del mount React (no flash).

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
