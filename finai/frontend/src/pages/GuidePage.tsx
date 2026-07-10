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
  { id: 'finanzapersonale', icon: '💰', title: 'Finanza Personale' },
  { id: 'mutuo', icon: '🏠', title: 'Mutui' },
  { id: 'finanziamenti', icon: '💳', title: 'Finanziamenti' },
  { id: 'simulazione', icon: '🧪', title: 'Simulazione' },
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
  finanzapersonale: (
    <div>
      <h2>💰 Finanza Personale</h2>
      <p>Il modulo di finanza personale legge i tuoi movimenti bancari, calcola un budget previsionale per il mese successivo e suggerisce un'allocazione di investimento in base al tuo profilo.</p>
      <h3>1. Importa l'estratto conto</h3>
      <ul>
        <li>Carica un file <strong>PDF</strong> o <strong>Excel</strong> (.xlsx/.xls) — formati tipici delle principali banche italiane</li>
        <li>FINAI riconosce automaticamente data, descrizione e importo di ogni movimento (anche in PDF senza una tabella ben strutturata)</li>
        <li>I movimenti già presenti (stessa data, descrizione e importo) vengono scartati come duplicati e segnalati a parte — utile se carichi più volte lo stesso periodo o estratti che si sovrappongono</li>
        <li>Le righe non riconosciute vengono semplicemente saltate, senza bloccare l'importazione del resto</li>
      </ul>
      <h3>2. Categorizzazione automatica</h3>
      <p>Ogni movimento viene classificato come <strong>entrata</strong> (stipendio, pensione, rimborso, interessi, bonifico ricevuto) o <strong>spesa variabile</strong>, con una categoria specifica (alimentari, trasporti, casa e utenze, salute, abbonamenti, tempo libero, ristorazione, shopping, ecc.). Se la categoria non è corretta, puoi modificarla manualmente dall'elenco movimenti — la modifica resta salvata e non viene sovrascritta da import successivi.</p>
      <h3>3. Spese fisse</h3>
      <p>Inserisci manualmente i costi ricorrenti che non passano (o passano in modo irregolare) dall'estratto conto: affitto, mutuo, utenze, abbonamenti, assicurazioni. Sono la base fissa su cui il budget del mese successivo viene calcolato.</p>
      <h3>4. Budget previsionale del mese successivo</h3>
      <p>FINAI stima entrate, costi fissi, costi variabili e risparmio investibile per il mese che verrà, basandosi sulla <strong>media dei mesi storici completi</strong> che contengono almeno un movimento di entrata. Il mese in corso, non ancora concluso, non entra nella media (altrimenti la stima risulterebbe artificialmente bassa). Se non c'è ancora nessun mese storico utilizzabile — ad esempio al primo utilizzo, o se i mesi passati contengono solo movimenti isolati senza alcuna entrata — la stima si basa sul mese in corso, e questo viene segnalato chiaramente in etichetta come stima provvisoria. Il saldo previsto può risultare <strong>negativo</strong> (mese in perdita, segnalato con un avviso dedicato): in quel caso non c'è alcuna quota investibile, perché entrate stimate e costi non si bilanciano.</p>
      <h3>5. Suggerimenti di risparmio</h3>
      <p>FINAI analizza i movimenti reali importati per individuare situazioni concrete su cui agire: una <strong>categoria di spesa sovrappesata</strong> rispetto alle altre, il superamento della quota "wants" della regola 50/30/20, un <strong>pagamento ricorrente</strong> che si ripete con importo simile per più mesi, o un <strong>trend di spesa in aumento</strong> in una categoria. Ogni suggerimento riporta una stima del risparmio mensile potenziale se viene seguito.</p>
      <h3>6. Questionario investitore</h3>
      <p>Tre informazioni — <strong>obiettivo</strong> (liquidità di emergenza, acquisto importante, pensione, crescita del capitale, altro), <strong>orizzonte temporale</strong> (da meno di 1 anno a oltre 10 anni) e, opzionalmente, <strong>liquidità già accantonata</strong> — alimentano un motore a regole che propone un'allocazione indicativa tra azionario, obbligazionario e liquidità: più l'orizzonte è lungo, maggiore la quota azionaria suggerita; l'obiettivo "liquidità di emergenza" prevale sempre con un'allocazione quasi tutta in liquidità, indipendentemente dall'orizzonte scelto.</p>
      <h3>7. Controlli prima di investire</h3>
      <p>Investire non è sempre la priorità giusta, anche quando c'è una quota disponibile. Per questo, prima di mostrare il consiglio, FINAI verifica due condizioni di buon senso finanziario:</p>
      <ul>
        <li><strong>Fondo di emergenza</strong>: se la liquidità dichiarata nel questionario copre meno di 3 mesi di spese (fisse + variabili stimate), viene mostrato un avviso a completare prima questo "cuscinetto" di sicurezza, prima di destinare soldi ai mercati — un imprevisto non dovrebbe costringere a vendere investimenti in perdita.</li>
        <li><strong>Debiti ad alto interesse</strong>: se tra le spese fisse c'è un finanziamento/debito con tasso annuo dichiarato ≥ 6% (marcabile direttamente nella sezione spese fisse), viene mostrato un avviso a estinguerlo con priorità — il rendimento "garantito" dell'interesse evitato batte quasi sempre il rendimento atteso di un investimento, senza alcun rischio di mercato.</li>
      </ul>
      <h3>8. Portafoglio esempio e piano di accumulo (PAC)</h3>
      <p>Il consiglio è accompagnato da un portafoglio esempio in ETF UCITS reali ai prezzi di oggi. Il peso tra lo strumento "core" e quello "satellite" di ciascun bucket non è un fisso 70/30 o 60/40: viene calcolato risolvendo la formula del <strong>portafoglio tangente a due asset</strong> della Modern Portfolio Theory di Markowitz, usando rendimento, volatilità e <strong>correlazione</strong> tra i due strumenti su uno storico a 3 anni (più stabile di una finestra a 1 anno). Dato che la quota investibile è un risparmio che si ripete ogni mese e non una somma unica, il consiglio suggerisce sempre di investirla gradualmente con un <strong>Piano di Accumulo Capitale (PAC)</strong> piuttosto che in un'unica soluzione, per ridurre il rischio di investire tutto in un momento sfavorevole del mercato.</p>
    </div>
  ),
  mutuo: (
    <div>
      <h2>🏠 Mutui</h2>
      <p>Sezione dedicata all'acquisto di una casa: calcola la rata del mutuo, tutto ciò che <strong>non</strong> è coperto dal mutuo (capitale proprio e spese accessorie) e a quali fonti attingere per coprirlo, con gli stessi criteri che una banca userebbe in fase di istruttoria.</p>
      <h3>1. Calcolatore mutuo</h3>
      <p>Inserisci <strong>importo dell'immobile</strong>, <strong>importo richiesto a mutuo</strong>, <strong>tasso di interesse annuo (TAN)</strong> ed <strong>anni a disposizione</strong>. FINAI calcola la rata mensile con il piano di ammortamento <strong>alla francese</strong> (rata costante, standard dei mutui italiani: la quota interessi è più alta all'inizio e diminuisce nel tempo a favore della quota capitale) e mostra il piano anno per anno.</p>
      <h3>2. Loan-to-Value (LTV)</h3>
      <p>Il rapporto tra importo del mutuo e valore dell'immobile. Le banche italiane concedono in genere mutui fondiari fino all'<strong>80%</strong> del valore: oltre questa soglia FINAI mostra un avviso, perché in pratica comporta condizioni più severe (tassi più alti, garanzie aggiuntive).</p>
      <h3>3. Rapporto rata/reddito</h3>
      <p>Non guarda solo alla nuova rata isolata: viene sommata anche la rata di eventuali <strong>altri debiti/finanziamenti già segnalati tra le spese fisse</strong> (quelli con un tasso di interesse indicato), perché è il rapporto <strong>rata complessiva/reddito</strong> a determinare davvero la sostenibilità. Se non dichiari un reddito netto mensile, FINAI lo stima automaticamente dal budget (media delle entrate importate dagli estratti conto). Il risultato è classificato come <strong>Sostenibile</strong> (≤ 30%), <strong>Al limite</strong> (30-35%) o <strong>Rischioso</strong> (&gt; 35%), soglie in linea con la prassi bancaria.</p>
      <h3>4. Stress test tassi</h3>
      <p>FINAI simula anche cosa succederebbe con un rialzo di <strong>2 punti percentuali</strong> del tasso — scenario rilevante soprattutto per un mutuo a tasso variabile — mostrando se la rata risulterebbe ancora sostenibile o supererebbe la soglia consigliata.</p>
      <h3>5. Tutto ciò che non rientra nel mutuo</h3>
      <p>Oltre alla rata, comprare casa richiede di coprire di tasca propria il <strong>capitale proprio</strong> (differenza tra prezzo e mutuo) e le <strong>spese accessorie</strong>: notaio, istruttoria bancaria, perizia dell'immobile, agenzia immobiliare e imposta di registro/IVA. Puoi inserire gli importi reali (da un preventivo) o lasciare i campi vuoti: FINAI li stima (es. notaio ~2% dell'immobile, istruttoria ~0.5% del mutuo, perizia ~300€). L'imposta di registro/IVA dipende dal tipo di acquisto che selezioni — <strong>prima casa</strong> (2% sul valore catastale, o 4% di IVA da costruttore) oppure <strong>seconda casa</strong> (9%, o 10% di IVA da costruttore): la stima usa il prezzo dichiarato come proxy del valore catastale, quindi è indicativa e spesso più alta del reale — verifica sempre la cifra esatta con il notaio.</p>
      <h3>6. Commissione di agenzia: percentuale o importo, con IVA automatica</h3>
      <p>Per l'agenzia immobiliare puoi scegliere se indicare una <strong>percentuale</strong> (es. 3%) o un <strong>importo fisso in euro</strong>. Se scegli la percentuale, FINAI aggiunge automaticamente l'<strong>IVA al 22%</strong> al totale (la commissione di agenzia in Italia si applica tipicamente così: "3% + IVA"). Se invece indichi un importo finale in euro (es. da un preventivo dell'agenzia), lo considera già comprensivo di ogni imposta, senza aggiunte ulteriori. Senza alcun dato, la stima di default è del 3% + IVA sul valore dell'immobile.</p>
      <h3>7. Da dove attingere</h3>
      <p>FINAI confronta il totale da pagare oltre al mutuo con la tua <strong>liquidità disponibile</strong> (dichiarata qui o ripresa dal questionario di Finanza Personale) e mostra il <strong>fabbisogno residuo</strong> non coperto. Se dichiari gli <strong>anni di iscrizione al tuo fondo pensione complementare</strong>, FINAI verifica se puoi accedervi: la legge (D.Lgs. 252/2005) ammette un'<strong>anticipazione fino al 75% del montante</strong> per l'acquisto della prima casa (per te o per i tuoi figli), ma solo dopo almeno <strong>8 anni di iscrizione</strong> — con meno anni (es. 2) questa fonte non è ancora disponibile, e non lo è mai per la seconda casa. Se indichi anche il montante accumulato, FINAI stima l'importo anticipabile. Infine, se resta un fabbisogno scoperto, FINAI propone quanti mesi di risparmio servirebbero (in base alla quota investibile mensile del tuo budget) e altre opzioni pratiche (rinegoziare le spese accessorie, aumentare il mutuo se sostenibile, aiuto familiare, posticipare l'acquisto).</p>
      <h3>8. Vendere una casa per finanziare l'acquisto</h3>
      <p>Se hai un'altra casa da vendere, spunta la casella dedicata e indica <strong>valore di vendita stimato</strong>, <strong>prezzo di acquisto originario</strong> e <strong>anni di possesso</strong> (più, opzionalmente, se è stata la tua abitazione principale, un eventuale mutuo/finanziamento residuo da estinguere, le spese di agenzia per la vendita e tra quanti mesi prevedi di completarla). Anche qui la commissione di agenzia si può indicare in <strong>percentuale</strong> (con IVA al 22% aggiunta automaticamente) o come <strong>importo fisso in euro</strong> già finale, con lo stesso meccanismo del calcolatore mutuo. FINAI calcola:</p>
      <ul>
        <li><strong>La plusvalenza e la sua tassazione</strong>: la differenza tra prezzo di vendita e di acquisto è tassabile con <strong>imposta sostitutiva del 26%</strong> solo se l'immobile è posseduto da <strong>meno di 5 anni</strong> e <strong>non</strong> è stato abitazione principale per la maggior parte del periodo di possesso (art. 67 TUIR); in tutti gli altri casi è sempre esente.</li>
        <li><strong>Il mutuo residuo da estinguere</strong>: se sulla casa venduta c'è ancora un finanziamento in corso, viene sottratto dal ricavato, perché và estinto alla vendita prima di poter disporre del resto.</li>
        <li><strong>Il capitale netto disponibile</strong>: un riepilogo "a cascata" mostra passo per passo il percorso dal valore di vendita al netto in tasca — spese di agenzia (sempre fatturate, IVA inclusa), mutuo residuo ed eventuale imposta sulla plusvalenza sottratti uno per uno — con il <strong>guadagno totale (plusvalenza)</strong> e il <strong>netto disponibile finale</strong> messi in evidenza separatamente fin da subito. Se il risultato è negativo, FINAI te lo segnala chiaramente: la vendita non libererebbe capitale, anzi lascerebbe un debito residuo.</li>
        <li><strong>Un confronto sui tempi</strong>: se indichi tra quanti mesi prevedi di vendere, FINAI lo confronta con il tempo medio di vendita di un immobile in Italia (~6 mesi) e, se i tuoi tempi sono più stretti, suggerisce un margine di sicurezza, un mutuo ponte o un compromesso condizionato alla vendita.</li>
      </ul>
      <p>Il capitale netto positivo si somma alla liquidità dichiarata nella lista "Da dove attingere", riducendo il fabbisogno residuo del nuovo mutuo.</p>
      <p>Se spunti <strong>"Questi soldi devono coprire tutto"</strong> — perché non hai altra liquidità di riserva e la vendita è la tua unica fonte di capitale — FINAI verifica con priorità se il capitale netto della vendita, da solo, copre l'intero costo non finanziato dal mutuo (capitale proprio + spese accessorie). Se non basta, mostra subito quanto manca e propone alternative concrete in ordine di praticità: rinegoziare il prezzo di vendita richiesto, ridurre le spese accessorie con più preventivi, valutare un mutuo più alto se resta sostenibile, posticipare il compromesso di acquisto finché non hai un'offerta di vendita adeguata, ed evitare di firmare impegni non condizionati al buon esito della vendita.</p>
      <h3>9. Quanto mutuo potresti richiedere</h3>
      <p>Una sezione calcola <strong>al contrario</strong>, dagli stessi dati già inseriti, quale importo di mutuo potresti ragionevolmente permetterti — utile per capire se stai chiedendo troppo (o se hai margine per chiedere di più):</p>
      <ul>
        <li><strong>Massimo per reddito</strong>: dalla rata sostenibile (reddito meno gli altri debiti già in essere) alla soglia prudente (30%) e alla soglia limite (35%), invertendo la formula di ammortamento con il tasso e la durata indicati.</li>
        <li><strong>Massimo per LTV</strong>: l'80% del valore dell'immobile, il tetto tipico dei mutui fondiari italiani.</li>
        <li><strong>Mutuo massimo consigliato</strong>: il più basso tra i due — il vincolo più stringente, esattamente come farebbe una banca in istruttoria — con indicazione di quale dei due vincoli (reddito o LTV) sia quello che conta di più nel tuo caso, e un confronto diretto con l'importo che hai effettivamente simulato.</li>
        <li><strong>Mutuo minimo necessario dato il tuo capitale</strong>: se il capitale disponibile (liquidità + eventuale vendita) copre già buona parte dell'acquisto, FINAI calcola l'importo minimo di mutuo che ti servirebbe davvero, segnalando se stai chiedendo più del necessario (con interessi evitabili) o se il capitale non basta a restare entro il massimo consigliato.</li>
      </ul>
      <h3>10. Rata più bassa o meno interessi? Confronto tra durate</h3>
      <p>Una tabella confronta lo stesso importo di mutuo simulato su un ventaglio di durate tipiche (10, 15, 20, 25, 30 anni, più quella che hai scelto): rata mensile, interessi totali, rapporto rata/reddito e sostenibilità per ciascuna. È il modo più diretto per vedere il classico compromesso — <strong>durata più lunga = rata più bassa ma interessi totali più alti</strong> — e scegliere in base al proprio budget, non solo alla rata mensile.</p>
      <h3>11. Prospetto completo</h3>
      <p>In cima al risultato trovi un riepilogo che riunisce tutti i numeri chiave in un unico colpo d'occhio: rata e sostenibilità, costo totale dell'intera operazione (prezzo + interessi + spese accessorie), quanto devi pagare oltre al mutuo, capitale disponibile complessivo, surplus o fabbisogno residuo, e il confronto tra il mutuo che hai simulato e il massimo consigliato. Sotto, una lista numerata raccoglie in ordine tutti i punti di attenzione emersi nella simulazione (LTV elevato, sostenibilità al limite, stress test, mutuo sopra il massimo consigliato, fondo pensione non ancora idoneo, vendita insufficiente, tempistica di vendita stretta) — così non devi cercarli card per card.</p>
      <h3>12. Detrazioni fiscali in dichiarazione dei redditi</h3>
      <p>Se il tipo di acquisto è <strong>prima casa</strong>, FINAI stima due detrazioni IRPEF previste dalla legge (art. 15 TUIR):</p>
      <ul>
        <li><strong>Interessi passivi del mutuo</strong>: detrazione del <strong>19%</strong> degli interessi pagati, fino a un massimo di <strong>4.000 € di interessi annui</strong> (detrazione massima teorica ~760 €/anno). La stima usa gli interessi del primo anno dal piano di ammortamento: negli anni successivi la quota interessi si riduce, quindi anche la detrazione scende progressivamente.</li>
        <li><strong>Spese di intermediazione immobiliare (agenzia)</strong>: detrazione del <strong>19%</strong> della spesa, fino a un massimo di <strong>1.000 €</strong> — <em>una tantum</em>, solo nell'anno di acquisto.</li>
      </ul>
      <p>Per la <strong>seconda casa</strong> queste detrazioni non sono ammesse. In entrambi i casi serve <strong>capienza IRPEF</strong> sufficiente (un'imposta lorda dovuta almeno pari alla detrazione); se il mutuo è cointestato, la detrazione va ripartita tra i cointestatari in base alle rispettive quote — spesso conviene indirizzarla verso chi ha più capienza fiscale. Conserva sempre fatture e bonifici: le detrazioni vanno indicate nel modello 730 o Redditi PF (quadro E), idealmente con l'aiuto di un commercialista o CAF.</p>
    </div>
  ),
  finanziamenti: (
    <div>
      <h2>💳 Finanziamenti</h2>
      <p>Per prestiti personali, cessioni del quinto o altri finanziamenti (non legati all'acquisto di una casa): stesso motore di calcolo della rata dei mutui, senza LTV né spese accessorie, che non si applicano a questo tipo di finanziamento.</p>
      <h3>1. Calcolatore finanziamento</h3>
      <p>Inserisci <strong>importo del finanziamento</strong>, <strong>tasso di interesse annuo (TAN)</strong> e <strong>durata in mesi</strong>. FINAI calcola la rata con lo stesso piano di ammortamento <strong>alla francese</strong> usato per i mutui, e mostra il piano di ammortamento anno per anno.</p>
      <h3>2. Rapporto rata/reddito</h3>
      <p>Come per il mutuo, il rapporto è <strong>complessivo</strong>: somma la nuova rata a quella di eventuali altri debiti/finanziamenti già tra le spese fisse. Reddito dichiarabile o stimato dal budget; classificazione <strong>Sostenibile</strong> / <strong>Al limite</strong> / <strong>Rischioso</strong> con le stesse soglie del mutuo (30% / 35%).</p>
    </div>
  ),
  simulazione: (
    <div>
      <h2>🧪 Simulazione (Paper Trading)</h2>
      <p>Un ambiente protetto per esercitarti a comprare e vendere titoli reali senza rischiare denaro vero.</p>
      <h3>Come funziona</h3>
      <ul>
        <li>Parti con un wallet virtuale da <strong>100.000€</strong> di liquidità</li>
        <li>Acquisti e vendite avvengono ai <strong>prezzi live</strong> di Yahoo Finance — proprio come nella tab Analisi/Mercato</li>
        <li>Comprando più volte lo stesso titolo, FINAI tiene traccia del <strong>prezzo medio di carico ponderato</strong> sulla posizione</li>
        <li>Vendendo (in parte o per intero) calcola il <strong>P&amp;L realizzato</strong> di quell'operazione; le posizioni ancora aperte mostrano il P&amp;L non realizzato ai prezzi correnti</li>
        <li>Tutte le operazioni restano nello storico, più recenti prima</li>
        <li>In qualsiasi momento puoi <strong>azzerare</strong> la simulazione e ripartire dal capitale iniziale</li>
      </ul>
      <p>È il modo più sicuro per testare una strategia — ad esempio un piano di accumulo o un'idea di trading — prima di metterci soldi veri.</p>
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
        ['🧪 Simulazione', 'Ambiente di paper trading con moneta virtuale: compra e vendi titoli reali ai prezzi live senza rischio, per studiare l\'andamento di un investimento prima di farlo davvero.'],
        ['💰 Finanza Personale', 'Importa l\'estratto conto (PDF/Excel) per categorizzare automaticamente le spese, inserisci i costi fissi mensili e ottieni il budget previsionale del mese successivo con la quota di risparmio investibile, suggerimenti di risparmio concreti e un consiglio di investimento (con portafoglio esempio, controllo del fondo di emergenza/debiti e suggerimento PAC) basato su un breve questionario.'],
        ['🏠 Mutui', 'Calcola la rata del mutuo per l\'acquisto di una casa: LTV, rapporto rata/reddito, stress test tassi, capitale proprio e spese accessorie (notaio, istruttoria, perizia, agenzia in % o € con IVA automatica, imposte), a quali fonti attingere (liquidità, fondo pensione, vendita di un\'altra casa — anch\'essa con agenzia in % o €), quanto mutuo potresti richiedere in base a reddito e LTV, un confronto rata/interessi tra diverse durate, e le detrazioni fiscali su interessi e agenzia per la prima casa.'],
        ['💳 Finanziamenti', 'Calcola la rata di un finanziamento o prestito personale con piano di ammortamento alla francese e rapporto rata/reddito comprensivo di altri debiti già tracciati.'],
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
