import { useState } from 'react'
import { useFinance, InvestmentGoal, InvestmentHorizon, PortfolioLine } from '@/hooks/useFinance'
import { formatNumber, colorForChange } from '@/lib/formatters'

const ASSET_CLASS_COLOR: Record<string, string> = {
  'Azionario': 'var(--acc)',
  'Obbligazionario': 'var(--acc3)',
  'Liquidità': 'var(--muted2)',
}

function PortfolioTable({ lines }: { lines: PortfolioLine[] }) {
  if (lines.length === 0) return null
  return (
    <div style={{ marginTop: 12 }}>
      <div style={{ fontFamily: 'Syne', fontWeight: 700, fontSize: 11, color: 'var(--muted2)', marginBottom: 6 }}>
        Portafoglio verosimile di oggi
      </div>
      <div style={{ background: 'var(--s3)', border: '1px solid var(--border)', borderRadius: 10, overflow: 'hidden' }}>
        {lines.map(l => (
          <div key={l.ticker} style={{
            display: 'grid', gridTemplateColumns: '10px 70px 1fr 50px 90px', gap: 8, alignItems: 'center',
            padding: '8px 12px', borderBottom: '1px solid var(--border)', fontFamily: 'JetBrains Mono', fontSize: 11,
          }}>
            <span style={{ width: 8, height: 8, borderRadius: '50%', background: ASSET_CLASS_COLOR[l.assetClass] ?? 'var(--muted)' }} />
            <span style={{ color: 'var(--text)', fontWeight: 700 }}>{l.ticker}</span>
            <span style={{ color: 'var(--muted2)', fontFamily: 'Syne', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }} title={l.rationale}>
              {l.name}
            </span>
            <span style={{ textAlign: 'right', color: 'var(--text)' }}>{formatNumber(l.weightPct, 1)}%</span>
            <span style={{ textAlign: 'right', color: l.dayChangePct != null ? colorForChange(l.dayChangePct) : 'var(--muted)' }}>
              {l.price != null ? `${formatNumber(l.price, 2)} ${l.currency ?? ''}` : 'n/d'}
              {l.dayChangePct != null && ` (${l.dayChangePct > 0 ? '+' : ''}${formatNumber(l.dayChangePct, 2)}%)`}
            </span>
          </div>
        ))}
      </div>
    </div>
  )
}

const GOAL_OPTIONS: Array<{ value: InvestmentGoal; label: string; desc: string }> = [
  { value: 'EMERGENCY', label: '🛟 Fondo di emergenza', desc: 'Liquidità da usare in caso di imprevisti' },
  { value: 'MAJOR_PURCHASE', label: '🏠 Grande acquisto', desc: 'Casa, auto, o altra spesa importante futura' },
  { value: 'RETIREMENT', label: '🌅 Pensione / lungo termine', desc: 'Integrare la pensione o costruire capitale nel tempo' },
  { value: 'GROWTH', label: '📈 Crescita del capitale', desc: 'Far crescere i risparmi senza un obiettivo specifico' },
  { value: 'OTHER', label: '✍️ Altro', desc: 'Un obiettivo personale diverso da questi' },
]

const HORIZON_OPTIONS: Array<{ value: InvestmentHorizon; label: string }> = [
  { value: 'UNDER_1Y', label: 'Meno di 1 anno' },
  { value: 'Y1_3', label: '1 - 3 anni' },
  { value: 'Y3_5', label: '3 - 5 anni' },
  { value: 'Y5_10', label: '5 - 10 anni' },
  { value: 'OVER_10Y', label: 'Oltre 10 anni' },
]

function OptionCard({ selected, label, desc, onClick }: { selected: boolean; label: string; desc?: string; onClick: () => void }) {
  return (
    <button
      onClick={onClick}
      style={{
        textAlign: 'left', padding: '12px 14px', borderRadius: 10,
        border: `1px solid ${selected ? 'var(--acc)' : 'var(--border)'}`,
        background: selected ? 'rgba(110,231,183,0.08)' : 'var(--s3)',
        cursor: 'pointer', display: 'flex', flexDirection: 'column', gap: 2,
      }}
    >
      <span style={{ fontFamily: 'Syne', fontWeight: 700, fontSize: 13, color: 'var(--text)' }}>{label}</span>
      {desc && <span style={{ fontFamily: 'Syne', fontSize: 11, color: 'var(--muted)' }}>{desc}</span>}
    </button>
  )
}

function RecommendationCard() {
  const { recommendation } = useFinance()
  if (!recommendation) return null
  const { allocation } = recommendation

  return (
    <div style={{ background: 'var(--s2)', border: '1px solid var(--acc)', borderRadius: 12, padding: '16px 20px', marginTop: 16 }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 10 }}>
        <div style={{
          padding: '6px 14px', borderRadius: 8, background: 'var(--acc)', color: '#07080a',
          fontFamily: 'Syne', fontWeight: 800, fontSize: 13,
        }}>
          {recommendation.profileLabel}
        </div>
      </div>

      <div style={{ display: 'flex', gap: 8, marginBottom: 12, height: 18, borderRadius: 6, overflow: 'hidden' }}>
        <div style={{ width: `${allocation.equityPct}%`, background: 'var(--acc)' }} title={`Azionario ${allocation.equityPct}%`} />
        <div style={{ width: `${allocation.bondPct}%`, background: 'var(--acc3)' }} title={`Obbligazionario ${allocation.bondPct}%`} />
        <div style={{ width: `${allocation.liquidityPct}%`, background: 'var(--muted2)' }} title={`Liquidità ${allocation.liquidityPct}%`} />
      </div>
      <div style={{ display: 'flex', gap: 16, marginBottom: 14, fontFamily: 'Syne', fontSize: 11, color: 'var(--muted2)' }}>
        <span>● Azionario {formatNumber(allocation.equityPct, 0)}%</span>
        <span style={{ color: 'var(--acc3)' }}>● Obbligazionario {formatNumber(allocation.bondPct, 0)}%</span>
        <span>● Liquidità {formatNumber(allocation.liquidityPct, 0)}%</span>
      </div>

      <p style={{ fontFamily: 'Syne', fontSize: 13, color: 'var(--muted2)', margin: '0 0 12px 0', lineHeight: 1.5 }}>
        {recommendation.summary}
      </p>

      <div style={{ fontFamily: 'Syne', fontWeight: 700, fontSize: 11, color: 'var(--muted2)', marginBottom: 6 }}>
        Strumenti suggeriti
      </div>
      <ul style={{ margin: 0, paddingLeft: 18, fontFamily: 'Syne', fontSize: 12, color: 'var(--text)' }}>
        {recommendation.suggestedInstruments.map(i => <li key={i} style={{ marginBottom: 4 }}>{i}</li>)}
      </ul>

      {recommendation.highInterestDebtWarning && (
        <div style={{
          marginTop: 14, padding: '10px 12px', borderRadius: 8, background: 'rgba(239,68,68,0.1)',
          border: '1px solid var(--red)', fontFamily: 'Syne', fontSize: 12, color: 'var(--red)', lineHeight: 1.5,
        }}>
          <span style={{ fontWeight: 700 }}>⚠ Prima estingui i debiti ad alto interesse: </span>
          {recommendation.highInterestDebtWarning}
        </div>
      )}

      {recommendation.emergencyFundWarning && (
        <div style={{
          marginTop: 14, padding: '10px 12px', borderRadius: 8, background: 'rgba(245,158,11,0.1)',
          border: '1px solid var(--acc3)', fontFamily: 'Syne', fontSize: 12, color: 'var(--acc3)', lineHeight: 1.5,
        }}>
          <span style={{ fontWeight: 700 }}>🛟 Completa prima il fondo di emergenza: </span>
          {recommendation.emergencyFundWarning}
        </div>
      )}

      {recommendation.pacNote && (
        <div style={{
          marginTop: 14, padding: '10px 12px', borderRadius: 8, background: 'var(--s3)',
          fontFamily: 'Syne', fontSize: 12, color: 'var(--muted2)', lineHeight: 1.5,
        }}>
          <span style={{ fontWeight: 700, color: 'var(--text)' }}>Come investirla: </span>
          {recommendation.pacNote}
        </div>
      )}

      {recommendation.marketSnapshot && (
        <div style={{
          marginTop: 14, padding: '10px 12px', borderRadius: 8, background: 'var(--s3)',
          fontFamily: 'Syne', fontSize: 12, color: 'var(--muted2)', lineHeight: 1.5,
        }}>
          <span style={{ fontWeight: 700, color: 'var(--text)' }}>Mercato oggi: </span>
          {recommendation.marketSnapshot.note}
        </div>
      )}

      <PortfolioTable lines={recommendation.samplePortfolio} />
    </div>
  )
}

export function QuestionnaireWizard() {
  const { profile, submitQuestionnaire, isSubmittingQuestionnaire } = useFinance()
  const [goal, setGoal] = useState<InvestmentGoal | null>(profile?.goal ?? null)
  const [horizon, setHorizon] = useState<InvestmentHorizon | null>(profile?.horizon ?? null)
  const [liquidSavings, setLiquidSavings] = useState(profile?.liquidSavings != null ? String(profile.liquidSavings) : '')
  const [error, setError] = useState<string | null>(null)

  const handleSubmit = async () => {
    if (!goal || !horizon) return
    setError(null)
    try {
      const parsed = parseFloat(liquidSavings)
      await submitQuestionnaire({ goal, horizon, liquidSavings: liquidSavings.trim() === '' || isNaN(parsed) ? null : parsed })
    } catch (e: any) {
      setError(e?.response?.data?.error || 'Impossibile calcolare il consiglio')
    }
  }

  return (
    <div style={{ background: 'var(--s2)', border: '1px solid var(--border)', borderRadius: 12, padding: '16px 20px' }}>
      <div style={{ fontFamily: 'Syne', fontWeight: 700, fontSize: 14, color: 'var(--text)', marginBottom: 4 }}>
        Questionario: a cosa serve il tuo investimento?
      </div>
      <div style={{ fontFamily: 'Syne', fontSize: 12, color: 'var(--muted)', marginBottom: 14 }}>
        Rispondi per ricevere un suggerimento sul tipo di investimento più adatto alla quota di risparmio investibile.
      </div>

      <div style={{ fontFamily: 'Syne', fontWeight: 700, fontSize: 12, color: 'var(--muted2)', marginBottom: 8 }}>
        1. A cosa serviranno i soldi che investi?
      </div>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: 8, marginBottom: 16 }}>
        {GOAL_OPTIONS.map(o => (
          <OptionCard key={o.value} selected={goal === o.value} label={o.label} desc={o.desc} onClick={() => setGoal(o.value)} />
        ))}
      </div>

      <div style={{ fontFamily: 'Syne', fontWeight: 700, fontSize: 12, color: 'var(--muted2)', marginBottom: 8 }}>
        2. Tra quanto tempo prevedi di averne bisogno?
      </div>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(120px, 1fr))', gap: 8, marginBottom: 16 }}>
        {HORIZON_OPTIONS.map(o => (
          <OptionCard key={o.value} selected={horizon === o.value} label={o.label} onClick={() => setHorizon(o.value)} />
        ))}
      </div>

      <div style={{ fontFamily: 'Syne', fontWeight: 700, fontSize: 12, color: 'var(--muted2)', marginBottom: 8 }}>
        3. Quanta liquidità hai già accantonata? <span style={{ fontWeight: 400, color: 'var(--muted)' }}>(opzionale)</span>
      </div>
      <div style={{ marginBottom: 16 }}>
        <input
          value={liquidSavings}
          onChange={e => setLiquidSavings(e.target.value)}
          placeholder="€ in conto deposito/corrente"
          type="number"
          style={{
            width: 220, background: 'var(--s3)', border: '1px solid var(--border)',
            borderRadius: 8, padding: '10px 12px', color: 'var(--text)', fontFamily: 'JetBrains Mono', fontSize: 13, outline: 'none',
          }}
        />
        <div style={{ fontFamily: 'Syne', fontSize: 11, color: 'var(--muted)', marginTop: 6 }}>
          Serve per verificare se il fondo di emergenza (3 mesi di spese) è già adeguato prima di consigliarti di investire.
        </div>
      </div>

      {error && (
        <div style={{ color: 'var(--red)', fontFamily: 'Syne', fontSize: 12, marginBottom: 10 }}>{error}</div>
      )}

      <button
        onClick={handleSubmit}
        disabled={!goal || !horizon || isSubmittingQuestionnaire}
        style={{
          padding: '10px 20px', borderRadius: 8, background: 'var(--acc)', border: 'none',
          color: '#07080a', fontFamily: 'Syne', fontWeight: 700, fontSize: 13,
          cursor: (!goal || !horizon) ? 'default' : 'pointer', opacity: (!goal || !horizon) ? 0.5 : 1,
        }}
      >
        {isSubmittingQuestionnaire ? 'Calcolo…' : 'Ottieni il consiglio'}
      </button>

      <RecommendationCard />
    </div>
  )
}
