import { useState, useCallback } from 'react'
import { useAlertsBackend } from '@/hooks/useAlertsBackend'
import { useAlerts } from '@/hooks/useAlerts'
import { AlertType } from '@/store/useAlertStore'
import { formatDateTime } from '@/lib/formatters'

const ALERT_LABELS: Record<AlertType, string> = {
  above: 'Prezzo >',
  below: 'Prezzo <',
  change_up: 'Variazione% ≥',
  change_down: 'Variazione% ≤ -',
}

export function AlertsPage() {
  const { alerts, history, isLoading, add, remove, fire } = useAlertsBackend()
  const [ticker, setTicker] = useState('')
  const [type, setType] = useState<AlertType>('above')
  const [value, setValue] = useState('')

  const handleFire = useCallback(async (id: string, price: number) => {
    await fire({ id, price })
  }, [fire])

  useAlerts(true, alerts, handleFire)

  const handleAdd = async () => {
    const t = ticker.trim().toUpperCase()
    const v = parseFloat(value)
    if (!t || isNaN(v)) return
    await add({ id: `alert-${Date.now()}`, ticker: t, type, value: v })
    setTicker(''); setValue('')
  }

  return (
    <div style={{ padding: 24, maxWidth: 900, margin: '0 auto' }}>
      <h2 style={{ fontFamily: 'Syne', fontWeight: 800, fontSize: 20, color: 'var(--text)', marginBottom: 20 }}>
        🔔 Alert Prezzi
      </h2>

      <div style={{ background: 'var(--s2)', border: '1px solid var(--border)', borderRadius: 12, padding: '20px', marginBottom: 24 }}>
        <div style={{ fontFamily: 'Syne', fontWeight: 700, fontSize: 14, color: 'var(--text)', marginBottom: 12 }}>Nuovo Alert</div>
        <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
          <input value={ticker} onChange={e => setTicker(e.target.value.toUpperCase())} placeholder="Ticker (es. AAPL)" style={{ background: 'var(--s3)', border: '1px solid var(--border)', borderRadius: 8, padding: '10px 14px', color: 'var(--text)', fontFamily: 'JetBrains Mono', fontSize: 13, outline: 'none', width: 140 }} />
          <select value={type} onChange={e => setType(e.target.value as AlertType)} style={{ background: 'var(--s3)', border: '1px solid var(--border)', borderRadius: 8, padding: '10px 14px', color: 'var(--text)', fontFamily: 'Syne', fontSize: 13, outline: 'none', cursor: 'pointer' }}>
            <option value="above">Prezzo sopra</option>
            <option value="below">Prezzo sotto</option>
            <option value="change_up">Variazione% ≥</option>
            <option value="change_down">Variazione% ≤ −</option>
          </select>
          <input value={value} onChange={e => setValue(e.target.value)} placeholder="Valore" type="number" style={{ background: 'var(--s3)', border: '1px solid var(--border)', borderRadius: 8, padding: '10px 14px', color: 'var(--text)', fontFamily: 'JetBrains Mono', fontSize: 13, outline: 'none', width: 120 }} />
          <button onClick={handleAdd} style={{ padding: '10px 20px', borderRadius: 8, background: 'var(--acc)', border: 'none', color: '#07080a', fontFamily: 'Syne', fontWeight: 700, fontSize: 13, cursor: 'pointer' }}>
            + Aggiungi
          </button>
        </div>
      </div>

      <div style={{ marginBottom: 24 }}>
        <div style={{ fontFamily: 'Syne', fontWeight: 700, fontSize: 14, color: 'var(--text)', marginBottom: 12 }}>Alert Attivi ({alerts.length})</div>
        {isLoading && <div style={{ color: 'var(--muted)', fontFamily: 'Syne', fontSize: 13 }}>Caricamento…</div>}
        {!isLoading && alerts.length === 0 && <div style={{ color: 'var(--muted)', fontFamily: 'Syne', fontSize: 13, padding: '20px 0' }}>Nessun alert attivo.</div>}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
          {alerts.map(alert => (
            <div key={alert.id} style={{ background: 'var(--s2)', border: '1px solid var(--border)', borderRadius: 10, padding: '12px 16px', display: 'flex', alignItems: 'center', gap: 12 }}>
              <span style={{ fontFamily: 'JetBrains Mono', fontWeight: 700, fontSize: 14, color: 'var(--acc)', minWidth: 80 }}>{alert.ticker}</span>
              <span style={{ fontFamily: 'Syne', fontSize: 13, color: 'var(--muted2)', flex: 1 }}>
                {ALERT_LABELS[alert.type]} {alert.value}{alert.type === 'change_up' || alert.type === 'change_down' ? '%' : ''}
              </span>
              <span style={{ fontFamily: 'JetBrains Mono', fontSize: 11, color: 'var(--muted)' }}>{formatDateTime(alert.createdAt)}</span>
              <button onClick={() => remove(alert.id)} style={{ background: 'none', border: '1px solid var(--red)', borderRadius: 6, padding: '3px 10px', color: 'var(--red)', cursor: 'pointer', fontFamily: 'Syne', fontSize: 12 }}>Elimina</button>
            </div>
          ))}
        </div>
      </div>

      {history.length > 0 && (
        <div>
          <div style={{ fontFamily: 'Syne', fontWeight: 700, fontSize: 14, color: 'var(--text)', marginBottom: 12 }}>Storico Alert Scattati</div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
            {history.map(alert => (
              <div key={alert.id} style={{ background: 'var(--s2)', border: '1px solid var(--border)', borderRadius: 10, padding: '12px 16px', display: 'flex', alignItems: 'center', gap: 12, opacity: 0.7 }}>
                <span style={{ fontFamily: 'JetBrains Mono', fontWeight: 700, fontSize: 14, color: 'var(--acc3)', minWidth: 80 }}>{alert.ticker}</span>
                <span style={{ fontFamily: 'Syne', fontSize: 13, color: 'var(--muted2)', flex: 1 }}>
                  {ALERT_LABELS[alert.type]} {alert.value} — Prezzo: {alert.firedPrice?.toFixed(2)}
                </span>
                <span style={{ fontFamily: 'JetBrains Mono', fontSize: 11, color: 'var(--muted)' }}>{alert.firedAt ? formatDateTime(alert.firedAt) : ''}</span>
                <span style={{ padding: '2px 8px', borderRadius: 6, background: 'var(--acc3)', color: '#07080a', fontFamily: 'Syne', fontSize: 11, fontWeight: 700 }}>SCATTATO</span>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}
