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

FINAI è composta da **9 sezioni** accessibili tramite la barra di navigazione superiore:

| Tab | Icona | Descrizione |
|-----|-------|-------------|
| **Mercato** | 📈 | Dashboard real-time con IndexBar (9 indici/valute), sentiment meter e griglia top/worst azioni + ETF |
| **Analisi** | 🔭 | Analisi completa di un ticker con ricerca autocomplete: grafico SMA, 8 metriche tecniche, segnale BUY/SELL/HOLD, scenari 30gg |
| **Confronto** | ⚖️ | Confronto fianco a fianco di due strumenti con tutti gli indicatori e verdetto automatico |
| **Alert** | 🔔 | Alert sui prezzi (sopra/sotto soglia, variazione %) con notifiche browser native — persistiti su PostgreSQL |
| **Lungo Termine** | 🌱 | Score 0–100 su 7 criteri per valutare idoneità DCA + strategia di accumulo consigliata |
| **Portafoglio** | 💼 | Tracker P&L personale: aggiunge posizioni, aggiorna prezzi live, mostra gain/loss — persistito su PostgreSQL |
| **IPO** | 🏛️ | Monitoraggio IPO: calendario prossime quotazioni (NASDAQ), performance IPO recenti, watchlist con tracker lock-up |
| **Suggeriti** | 🎯 | 4 portafogli modello (Conservativo/Bilanciato/Crescita/Aggressivo) con allocazioni e metriche attese |
| **Guida** | 📚 | Guida completa in italiano: glossario, indicatori tecnici, ETF, DCA, errori comuni |

Ogni sezione include un **pannello chat AI** contestuale: l'assistente conosce i dati del ticker/portafoglio visualizzato e risponde in italiano con streaming in tempo reale.

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
│         │  PortfolioRepository │ AlertRepository │ IpoWatchlistRepository│   │
│         └──────────────────────────┬───────────────────────────────────┘   │
│                                    │                                        │
└────────────────────────────────────┼───────────────────────────────────────┘
                                     │
                         ┌───────────▼──────────────┐
                         │   PostgreSQL 16            │
                         │   portfolio_items          │
                         │   alerts                   │
                         │   ipo_watchlist            │
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
| `flyway-core` | incluso in Boot | Migrazioni schema versionato |
| `caffeine` | incluso in Boot | Cache in-memory LRU con TTL |
| `resilience4j-spring-boot3` | 2.2.0 | Retry + Circuit Breaker annotazionali |
| `springdoc-openapi-starter-webmvc-ui` | 2.6.0 | Swagger UI automatico |
| `lombok` | incluso in Boot | Riduzione boilerplate (getter/setter) |
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
│       │   │   │   ├── AiController.java          # POST /chat SSE (thread virtuali)
│       │   │   │   └── HealthController.java
│       │   │   ├── domain/
│       │   │   │   ├── entity/
│       │   │   │   │   ├── PortfolioItem.java     # @Entity JPA
│       │   │   │   │   ├── Alert.java             # @Entity JPA con isActive()
│       │   │   │   │   └── IpoWatchlistItem.java  # @Entity + lockupRemainingDays()
│       │   │   │   └── enums/
│       │   │   │       └── AlertType.java         # ABOVE|BELOW|CHANGE_UP|CHANGE_DOWN
│       │   │   ├── dto/                           # Java records immutabili
│       │   │   │   ├── quote/    QuoteDto, FullQuoteDto, HistoryPoint
│       │   │   │   ├── portfolio/ AddPortfolioItemRequest, PortfolioItemDto
│       │   │   │   ├── alert/    AddAlertRequest, FireAlertRequest, AlertDto, AlertsResponse
│       │   │   │   ├── ipo/      UpcomingIpoDto, RecentIpoDto, IpoWatchlistItemDto,
│       │   │   │   │             AddIpoWatchlistRequest, UpdateIpoWatchlistRequest
│       │   │   │   ├── search/   SearchResultDto
│       │   │   │   └── ai/       ChatRequest, ChatMessage
│       │   │   ├── exception/
│       │   │   │   ├── FinaiException.java        # Eccezione con statusCode HTTP
│       │   │   │   └── GlobalExceptionHandler.java # @RestControllerAdvice → JSON uniforme
│       │   │   ├── repository/
│       │   │   │   ├── PortfolioRepository.java   # JPA + @Modifying per update bulk
│       │   │   │   ├── AlertRepository.java
│       │   │   │   └── IpoWatchlistRepository.java
│       │   │   └── service/
│       │   │       ├── YahooFinanceService.java   # @Retry + @CircuitBreaker + @Cacheable
│       │   │       ├── NasdaqService.java         # @Retry + @CircuitBreaker + @Cacheable
│       │   │       ├── AnthropicService.java      # SSE streaming + buildSystemPrompt()
│       │   │       ├── IndicatorsService.java     # RSI, SMA, volatilità, momentum, BullScore
│       │   │       ├── PortfolioService.java      # CRUD + refresh bulk prezzi
│       │   │       ├── AlertService.java          # CRUD + fire idempotente
│       │   │       └── IpoService.java            # calendario + watchlist CRUD
│       │   └── resources/
│       │       ├── application.yml               # Config principale
│       │       ├── application-test.yml          # Override per test (PostgreSQL test DB)
│       │       └── db/migration/
│       │           ├── V1__create_tables.sql     # Schema iniziale (3 tabelle)
│       │           ├── V2__add_indexes.sql       # Indici per query frequenti
│       │           └── V3__fix_alert_type_constraint.sql
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
        ├── App.tsx               # QueryClientProvider + lazy tab router (9 tab)
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
        │   └── useIPO.ts
        ├── components/
        │   ├── layout/   Header, NavTabs (9 tab), PandaLoader
        │   ├── common/   SearchInput (autocomplete)
        │   ├── market/   IndexBar, SentimentMeter, MarketGrid, MarketRow
        │   ├── analyze/  StockHero, PriceChart, SignalBadge, PredictionCard
        │   └── chat/     ChatPanel, ChatMessage
        └── pages/
            ├── MarketPage, AnalyzePage, ComparePage, AlertsPage
            ├── LongTermPage, PortfolioPage, IPOPage
            ├── SuggestedPage, GuidePage
```

---

## Installazione e avvio

### Prerequisiti

| Tool | Versione minima | Note |
|------|-----------------|------|
| **Java JDK** | 21 | OpenJDK o Oracle JDK 21 LTS |
| **Apache Maven** | 3.9 | `mvn -version` |
| **Node.js** | 18 | `node -v` |
| **PostgreSQL** | 16 | oppure Docker con `docker compose up -d postgres` |
| **Docker** | 24+ | solo per `docker compose`, opzionale se PostgreSQL nativo |
| **API key Anthropic** | — | Obbligatoria per la chat AI |

### 1. Clona il repository

```bash
git clone https://github.com/giole89/TestClaude.git
cd TestClaude/finai
git checkout claude/finai-web-app-I6tE2
```

### 2. Avvia il database PostgreSQL

#### Con Docker Compose (consigliato)

```bash
# Avvia solo PostgreSQL in background
npm run db:up
# oppure direttamente:
docker compose up -d postgres
```

#### Con PostgreSQL nativo

```bash
# Crea il database e l'utente
sudo -u postgres psql -c "CREATE USER finai WITH PASSWORD 'finai';"
sudo -u postgres psql -c "CREATE DATABASE finai OWNER finai;"
```

### 3. Configura le variabili d'ambiente

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

### 4. Installa le dipendenze frontend

```bash
npm run install:all
# oppure solo:
cd frontend && npm install
```

Il backend Java usa Maven e non richiede `npm install`.

### 5. Avvia l'applicazione

#### Avvio simultaneo (consigliato)

```bash
# Dalla root finai/
npm run dev
```

Avvia in parallelo:
- **Backend**: `mvn spring-boot:run` sulla porta **3001**
- **Frontend**: `vite` sulla porta **5173**

Output atteso:
```
[backend]  Tomcat started on port 3001 (http)
[backend]  Started FinaiApplication in 4.2 seconds
[backend]  Flyway: Successfully applied 3 migrations to schema "public"
[frontend] Local: http://localhost:5173
```

#### Avvio separato

```bash
# Terminale 1 — Backend Java
cd backend
mvn spring-boot:run

# Terminale 2 — Frontend
cd frontend
npm run dev
```

### 6. Apri nel browser

```
http://localhost:5173        # App principale
http://localhost:3001/swagger-ui.html    # Swagger UI (documentazione API)
http://localhost:3001/health             # Health check
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

---

## Persistenza dati — PostgreSQL

Portfolio, alert e watchlist IPO sono persistiti su **PostgreSQL 16** via Spring Data JPA.
Le migrazioni sono gestite da **Flyway** con versioning incrementale.

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
```

### Migrazioni Flyway

| Versione | File | Descrizione |
|----------|------|-------------|
| V1 | `V1__create_tables.sql` | Schema iniziale (3 tabelle) |
| V2 | `V2__add_indexes.sql` | Indici su ticker, fired_at, expected_date |
| V3 | `V3__fix_alert_type_constraint.sql` | Constraint alert_type in uppercase |

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
81 test totali — tutti verdi — JaCoCo coverage ≥ 70%

Service tests (50 test unit — Mockito, zero dipendenze esterne)
├── IndicatorsServiceTest  24 test (RSI, SMA, volatilità, momentum, BullScore, rangePos)
├── PortfolioServiceTest    7 test
├── AlertServiceTest        7 test
├── IpoServiceTest          6 test
└── AnthropicServiceTest    6 test (buildSystemPrompt per ogni tab)

Controller tests (13 test funzionali — @WebMvcTest + MockMvc)
├── PortfolioControllerTest 7 test (200/201/400/404/409 HTTP status)
└── AlertControllerTest     6 test

Integration tests (18 test — Spring Boot + PostgreSQL reale)
├── PortfolioIntegrationTest 7 test (ciclo CRUD completo)
├── AlertIntegrationTest     6 test (creazione → fire → history)
└── IpoIntegrationTest       5 test (watchlist + lock-up calc)
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
