import { useState } from 'react'

const SECTIONS = [
  { id: 'intro', icon: '🧭', title: 'Introduzione' },
  { id: 'mindset', icon: '🧠', title: 'Mindset' },
  { id: 'rischio', icon: '⚖️', title: 'Rischio e Rendimento' },
  { id: 'termini', icon: '🔤', title: 'Acronimi & Termini' },
  { id: 'indicatori', icon: '📊', title: 'Indicatori Tecnici' },
  { id: 'etf', icon: '📦', title: 'ETF & Fondi' },
  { id: 'analisi', icon: '🔍', title: 'Come Analizzare' },
  { id: 'dca', icon: '📅', title: 'Strategia DCA' },
  { id: 'portafoglio', icon: '🗂️', title: 'Costruire un Portafoglio' },
  { id: 'errori', icon: '🚫', title: 'Errori Comuni' },
  { id: 'finai', icon: '🚀', title: 'Come Usare FINAI' },
]

const CONTENT: Record<string, React.ReactNode> = {
  intro: (
    <div>
      <h2>🧭 Introduzione ai Mercati Finanziari</h2>
      <p>Il mercato finanziario è un sistema dove si acquistano e vendono strumenti finanziari come azioni, obbligazioni ed ETF.</p>
      <h3>Azioni vs ETF</h3>
      <ul>
        <li><strong>Azioni:</strong> Rappresentano una quota di proprietà in un'azienda. Alto rischio/rendimento, richiedono analisi approfondita.</li>
        <li><strong>ETF (Exchange Traded Fund):</strong> Fondi che replicano un indice (es. S&P 500). Diversificazione automatica, costi bassi, ideali per investitori passivi.</li>
      </ul>
      <p>Per la maggior parte degli investitori, un portafoglio core di ETF globali è la scelta più efficiente.</p>
    </div>
  ),
  mindset: (
    <div>
      <h2>🧠 Il Mindset dell'Investitore</h2>
      <p><strong>Le 5 regole d'oro:</strong></p>
      <ul>
        <li>Non investire denaro che potrebbe servirti nei prossimi 3–5 anni</li>
        <li>Non guardare il portafoglio ogni giorno — causa ansia e decisioni sbagliate</li>
        <li>Rimani investito durante i crolli: i mercati si riprendono sempre</li>
        <li>Non cercare di prevedere il mercato: nessuno ci riesce in modo consistente</li>
        <li>La diversificazione è la tua migliore protezione</li>
      </ul>
      <blockquote style={{ borderLeft: '3px solid var(--acc)', paddingLeft: 16, color: 'var(--muted2)', fontStyle: 'italic' }}>
        "Sii avido quando gli altri hanno paura, e timoroso quando gli altri sono avidi." — Warren Buffett
      </blockquote>
    </div>
  ),
  rischio: (
    <div>
      <h2>⚖️ Rischio e Rendimento</h2>
      <p>Ogni strumento finanziario ha un profilo rischio/rendimento diverso:</p>
      <table style={{ width: '100%', borderCollapse: 'collapse', marginTop: 16 }}>
        <thead>
          <tr style={{ background: 'var(--s3)' }}>
            {['Strumento', 'Rischio', 'Rendimento atteso', 'Orizzonte'].map(h => (
              <th key={h} style={{ padding: '8px 12px', textAlign: 'left', fontFamily: 'Syne', fontSize: 12, color: 'var(--muted)' }}>{h}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {[
            ['Conto deposito', 'Basso', '1–3%', '< 1 anno'],
            ['Obbligazioni govt', 'Basso-Medio', '2–4%', '1–5 anni'],
            ['ETF azionari globali', 'Medio', '6–9% storico', '5–10+ anni'],
            ['Azioni singole', 'Alto', '0–20%+', '5+ anni'],
            ['Crypto', 'Molto alto', 'Imprevedibile', '10+ anni'],
          ].map((row, i) => (
            <tr key={i} style={{ borderBottom: '1px solid var(--border)' }}>
              {row.map((cell, j) => (
                <td key={j} style={{ padding: '8px 12px', fontFamily: j === 0 ? 'Syne' : 'JetBrains Mono', fontSize: 13, color: 'var(--text)' }}>{cell}</td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
      <h3>Drawdown</h3>
      <p>Il drawdown è la perdita massima dal picco. L'S&P 500 ha avuto drawdown del -50% nel 2008-09. Chi rimase investito ha poi recuperato e guadagnato molto.</p>
    </div>
  ),
  termini: (
    <div>
      <h2>🔤 Glossario Finanziario</h2>
      {[
        ['ETF', 'Exchange Traded Fund — fondo che replica un indice, quotato in borsa'],
        ['RSI', 'Relative Strength Index — indicatore di forza relativa (0-100). > 70 ipercomprato, < 30 ipervenduto'],
        ['SMA', 'Simple Moving Average — media mobile semplice dei prezzi di chiusura su N giorni'],
        ['EMA', 'Exponential Moving Average — media mobile che pesa di più i dati recenti'],
        ['MACD', 'Moving Average Convergence Divergence — indicatore di trend basato su EMA'],
        ['DCA', 'Dollar Cost Averaging — strategia di acquisto periodico a importo fisso'],
        ['P/E', 'Price/Earnings — rapporto prezzo/utili. Indica quanto si paga per ogni euro di utile'],
        ['EPS', 'Earnings Per Share — utile per azione'],
        ['TER', 'Total Expense Ratio — costo annuo di un ETF (es. 0.07%)'],
        ['NAV', 'Net Asset Value — valore patrimoniale netto di un fondo'],
        ['ATH', 'All Time High — massimo storico del prezzo'],
        ['YTD', 'Year To Date — variazione dall\'inizio dell\'anno corrente'],
        ['52W High/Low', 'Massimo e minimo degli ultimi 52 settimane (1 anno)'],
        ['Volatilità', 'Misura della variabilità del prezzo, espressa in % annualizzata'],
        ['Drawdown', 'Perdita massima dal picco al minimo successivo'],
        ['Dividendo', 'Parte degli utili distribuita agli azionisti (es. trimestralmente)'],
      ].map(([term, def]) => (
        <div key={term} style={{ display: 'flex', gap: 16, padding: '8px 0', borderBottom: '1px solid var(--border)' }}>
          <span style={{ fontFamily: 'JetBrains Mono', fontWeight: 700, fontSize: 13, color: 'var(--acc)', minWidth: 80 }}>{term}</span>
          <span style={{ fontFamily: 'Syne', fontSize: 13, color: 'var(--text)' }}>{def}</span>
        </div>
      ))}
    </div>
  ),
  indicatori: (
    <div>
      <h2>📊 Indicatori Tecnici</h2>
      <h3>RSI (Relative Strength Index)</h3>
      <p>Scala da 0 a 100:</p>
      <div style={{ display: 'flex', gap: 4, marginBottom: 16 }}>
        {[
          { range: '0–30', label: 'Ipervenduto', color: 'var(--red)' },
          { range: '30–45', label: 'Ribassista', color: '#f87171' },
          { range: '45–55', label: 'Neutro', color: 'var(--muted2)' },
          { range: '55–70', label: 'Rialzista', color: 'var(--acc)' },
          { range: '70–100', label: 'Ipercomprato', color: 'var(--acc3)' },
        ].map(s => (
          <div key={s.range} style={{
            flex: 1, background: s.color, borderRadius: 6,
            padding: '8px 6px', textAlign: 'center',
          }}>
            <div style={{ fontFamily: 'JetBrains Mono', fontSize: 11, color: '#07080a', fontWeight: 700 }}>{s.range}</div>
            <div style={{ fontFamily: 'Syne', fontSize: 10, color: '#07080a' }}>{s.label}</div>
          </div>
        ))}
      </div>
      <h3>Medie Mobili (SMA/EMA)</h3>
      <ul>
        <li><strong>Golden Cross:</strong> SMA50 incrocia sopra SMA200 → segnale rialzista</li>
        <li><strong>Death Cross:</strong> SMA50 incrocia sotto SMA200 → segnale ribassista</li>
        <li>Prezzo sopra SMA200 → trend di lungo termine rialzista</li>
      </ul>
      <h3>MACD</h3>
      <p>Differenza tra EMA12 e EMA26. Quando il MACD supera la linea segnale (EMA9 del MACD) → impulso rialzista.</p>
    </div>
  ),
  etf: (
    <div>
      <h2>📦 ETF e Fondi</h2>
      <p>Gli ETF replicano passivamente un indice con costi minimi (TER 0.03%–0.20%).</p>
      <h3>ETF vs Fondi Attivi</h3>
      <ul>
        <li>Il 90%+ dei fondi attivi non batte il mercato nel lungo periodo</li>
        <li>Gli ETF hanno costi 10-50x inferiori ai fondi attivi</li>
        <li>La differenza di costo si accumula enormemente in 20-30 anni</li>
      </ul>
      <h3>I principali ETF per investitori italiani</h3>
      {[
        ['VWCE.DE', 'Vanguard FTSE All-World', '0.22%', '3700+ aziende globali — il più diversificato'],
        ['IWDA.AS', 'iShares MSCI World', '0.20%', 'Solo mercati sviluppati (23 paesi)'],
        ['SPY / IVV / VOO', 'S&P 500 ETF', '0.03%–0.09%', 'Le 500 maggiori aziende USA'],
        ['QQQ / EQQQ.AS', 'Nasdaq-100', '0.20%–0.30%', 'Le 100 maggiori aziende Nasdaq (tech focus)'],
      ].map(([ticker, name, ter, desc]) => (
        <div key={ticker} style={{ padding: '10px 0', borderBottom: '1px solid var(--border)' }}>
          <div style={{ display: 'flex', gap: 8, marginBottom: 4 }}>
            <span style={{ fontFamily: 'JetBrains Mono', fontWeight: 700, color: 'var(--acc)', fontSize: 13 }}>{ticker}</span>
            <span style={{ fontFamily: 'Syne', fontWeight: 700, fontSize: 13 }}>{name}</span>
            <span style={{ fontFamily: 'JetBrains Mono', fontSize: 11, color: 'var(--acc2)', marginLeft: 'auto' }}>TER: {ter}</span>
          </div>
          <div style={{ fontFamily: 'Syne', fontSize: 12, color: 'var(--muted)' }}>{desc}</div>
        </div>
      ))}
    </div>
  ),
  analisi: (
    <div>
      <h2>🔍 Come Analizzare un Titolo</h2>
      <p>Il metodo in 5 passi:</p>
      {[
        ['1. Fondamentali', 'Leggi bilancio, P/E ratio, crescita fatturato e utili negli ultimi 3-5 anni'],
        ['2. Trend di lungo termine', 'Il prezzo è sopra SMA200? Il trend primario è rialzista?'],
        ['3. Analisi tecnica', 'Verifica RSI, momentum e posizione rispetto alle medie mobili'],
        ['4. Valutazione (DCF)', 'Usa il modello DCF semplificato nella tab Analisi per stimare il fair value partendo da EPS, crescita attesa e tasso di sconto.'],
        ['5. Contesto settoriale', 'Il settore sta crescendo? Ci sono venti contrari regolatori o macro?'],
        ['6. Valutazione rischio', 'Qual è il tuo stop loss? Quanto puoi permetterti di perdere?'],
      ].map(([step, desc]) => (
        <div key={step} style={{ display: 'flex', gap: 16, padding: '12px 0', borderBottom: '1px solid var(--border)' }}>
          <span style={{ fontFamily: 'Syne', fontWeight: 700, fontSize: 13, color: 'var(--acc)', minWidth: 160 }}>{step}</span>
          <span style={{ fontFamily: 'Syne', fontSize: 13, color: 'var(--text)' }}>{desc}</span>
        </div>
      ))}
    </div>
  ),
  dca: (
    <div>
      <h2>📅 Strategia DCA (Dollar Cost Averaging)</h2>
      <p>Investi una somma fissa a intervalli regolari, indipendentemente dal prezzo.</p>
      <h3>Come funziona</h3>
      <ul>
        <li>Elimina il problema del "market timing" — non devi indovinare il momento giusto</li>
        <li>Acquisti più quote quando il prezzo è basso, meno quando è alto</li>
        <li>Riduce la volatilità del prezzo medio di carico nel tempo</li>
      </ul>
      <h3>Esempio pratico</h3>
      <table style={{ width: '100%', borderCollapse: 'collapse', marginTop: 12 }}>
        <thead>
          <tr style={{ background: 'var(--s3)' }}>
            {['Mese', 'Importo', 'Prezzo ETF', 'Quote acquistate'].map(h => (
              <th key={h} style={{ padding: '8px 12px', textAlign: 'left', fontFamily: 'Syne', fontSize: 12, color: 'var(--muted)' }}>{h}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {[
            ['Gen', '200€', '100€', '2.00'],
            ['Feb', '200€', '80€', '2.50'],
            ['Mar', '200€', '110€', '1.82'],
            ['Apr', '200€', '95€', '2.11'],
            ['', 'Totale: 800€', 'Media: 93€', '8.43 quote'],
          ].map((row, i) => (
            <tr key={i} style={{ borderBottom: '1px solid var(--border)', background: i === 4 ? 'var(--s3)' : 'transparent' }}>
              {row.map((cell, j) => (
                <td key={j} style={{ padding: '8px 12px', fontFamily: 'JetBrains Mono', fontSize: 13, color: i === 4 ? 'var(--acc)' : 'var(--text)', fontWeight: i === 4 ? 700 : 400 }}>{cell}</td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  ),
  portafoglio: (
    <div>
      <h2>🗂️ Costruire un Portafoglio</h2>
      <h3>I tre modelli principali</h3>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
        {[
          { name: 'Lazy Portfolio', desc: '1–2 ETF globali (VWCE.DE). Massima semplicità, ottima diversificazione. Ideale per chi vuole il minimo attrito.' },
          { name: '60/40 Portfolio', desc: '60% azionario globale (VWCE) + 40% obbligazionario (AGGH). Equilibrio tra crescita e stabilità.' },
          { name: 'Core-Satellite', desc: '70% core ETF globale + 30% satellite (settori/paesi specifici). Permette personalizzazione mantenendo una base solida.' },
        ].map(m => (
          <div key={m.name} style={{ background: 'var(--s3)', borderRadius: 10, padding: '14px 16px' }}>
            <div style={{ fontFamily: 'Syne', fontWeight: 700, fontSize: 14, color: 'var(--acc)', marginBottom: 6 }}>{m.name}</div>
            <div style={{ fontFamily: 'Syne', fontSize: 13, color: 'var(--text)' }}>{m.desc}</div>
          </div>
        ))}
      </div>
      <h3>Regola d'oro della diversificazione</h3>
      <p>Non mettere più del 5% in un singolo titolo, non più del 20% in un singolo settore, non più del 30% in un singolo paese.</p>
    </div>
  ),
  errori: (
    <div>
      <h2>🚫 Errori Comuni da Evitare</h2>
      {[
        ['FOMO', 'Fear Of Missing Out — comprare dopo un forte rialzo per paura di perdere il treno. I titoli scaldati raramente continuano all\'infinito.'],
        ['Mediare le perdite', 'Comprare più quote di un titolo in perdita sperando che "torni su". Funziona solo con aziende/ETF fondamentalmente solidi.'],
        ['Overtrading', 'Comprare e vendere troppo frequentemente. Le commissioni e le tasse erodono i rendimenti.'],
        ['Concentrazione eccessiva', 'Mettere tutto su 1–2 titoli. Un singolo evento aziendale può dimezzare il portafoglio.'],
        ['Ignorare l\'inflazione', 'Tenere tutto in contante non è "sicuro": l\'inflazione erode il potere d\'acquisto ogni anno.'],
        ['Panic selling', 'Vendere durante i crolli di mercato. I crolli sono parte del gioco — i mercati si riprendono sempre (storicamente).'],
      ].map(([err, desc]) => (
        <div key={err} style={{ padding: '12px 0', borderBottom: '1px solid var(--border)' }}>
          <div style={{ fontFamily: 'Syne', fontWeight: 700, fontSize: 13, color: 'var(--red)', marginBottom: 4 }}>❌ {err}</div>
          <div style={{ fontFamily: 'Syne', fontSize: 13, color: 'var(--text)' }}>{desc}</div>
        </div>
      ))}
    </div>
  ),
  finai: (
    <div>
      <h2>🚀 Come Usare FINAI</h2>
      {[
        ['📈 Mercato', 'Dashboard real-time con i migliori e peggiori titoli/ETF per performance giornaliera e YTD. Clicca su qualsiasi riga per analizzarla.'],
        ['🔭 Analisi', 'Analisi completa di un titolo: grafico interattivo, indicatori tecnici (RSI, SMA), segnale AI, previsioni, DCF semplificato e ultime news.'],
        ['⚖️ Confronto', 'Confronta due strumenti fianco a fianco con tutti gli indicatori chiave.'],
        ['🔔 Alert', 'Imposta alert sui prezzi — ricevi notifiche quando le condizioni si verificano.'],
        ['🌱 Lungo Termine', 'Score 0-100 per valutare se un titolo è adatto per DCA a lungo termine.'],
        ['💼 Portafoglio', 'Traccia il portafoglio con P&L in tempo reale. Include: dividendi, benchmark vs S&P 500, simulatore DCA, stima gain fiscale (26% italiano), matrice di correlazione e news aggiornate.'],
        ['🎯 Suggeriti', 'Portafogli modello pre-costruiti per 4 profili di rischio (Conservativo, Bilanciato, Crescita, Aggressivo). Chiedi all\'AI un portafoglio personalizzato.'],
        ['🌐 Macro', 'Dashboard macroeconomica: indici globali (S&P 500, Nasdaq, DAX…), valute, commodity, crypto (BTC, ETH) e tassi USA (10Y/30Y). Sentiment aggregato Risk On/Off.'],
        ['🔍 Screener', 'Filtra l\'universo di oltre 170 titoli per variazione giornaliera, YTD e posizione nel range 52 settimane. Trova opportunità rapidamente.'],
        ['📌 Watchlist', 'Tieni traccia dei tuoi titoli preferiti. Aggiungi target price per monitorare quando un titolo raggiunge il tuo obiettivo.'],
        ['📚 Guida', 'Sei qui! Torna quando hai dubbi su termini, indicatori o strategie di investimento.'],
      ].map(([tab, desc]) => (
        <div key={tab} style={{ display: 'flex', gap: 16, padding: '10px 0', borderBottom: '1px solid var(--border)' }}>
          <span style={{ fontFamily: 'Syne', fontWeight: 700, fontSize: 13, color: 'var(--acc2)', minWidth: 140 }}>{tab}</span>
          <span style={{ fontFamily: 'Syne', fontSize: 13, color: 'var(--text)' }}>{desc}</span>
        </div>
      ))}
    </div>
  ),
}

export function GuidePage() {
  const [active, setActive] = useState('intro')

  return (
    <div style={{ display: 'flex', height: 'calc(100vh - 112px)' }}>
      <nav style={{
        width: 220, background: 'var(--s1)',
        borderRight: '1px solid var(--border)',
        padding: '16px 12px', overflowY: 'auto',
        flexShrink: 0,
      }}>
        {SECTIONS.map(s => (
          <button
            key={s.id}
            onClick={() => setActive(s.id)}
            style={{
              display: 'flex', alignItems: 'center', gap: 8,
              width: '100%', padding: '9px 12px', borderRadius: 8,
              border: 'none', cursor: 'pointer', textAlign: 'left',
              background: active === s.id ? 'var(--acc)' : 'transparent',
              color: active === s.id ? '#07080a' : 'var(--muted2)',
              fontFamily: 'Syne', fontWeight: 600, fontSize: 13,
              marginBottom: 2, transition: 'all 0.15s',
            }}
          >
            <span>{s.icon}</span>
            <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{s.title}</span>
          </button>
        ))}
      </nav>

      <div style={{
        flex: 1, overflowY: 'auto', padding: '32px 40px',
        fontFamily: 'Syne', color: 'var(--text)', lineHeight: 1.8,
        fontSize: 14,
      }}
        className="guide-content"
      >
        {CONTENT[active] ?? <div>Sezione in arrivo…</div>}
      </div>
    </div>
  )
}
