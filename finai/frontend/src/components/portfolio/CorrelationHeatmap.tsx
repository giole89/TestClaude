import { useCorrelation } from '@/hooks/useCorrelation'
import { formatNumber } from '@/lib/formatters'

function corrColor(c: number): string {
  if (c >= 0.7) return 'rgba(239, 68, 68, 0.7)'   // rosso: alta correlazione
  if (c >= 0.4) return 'rgba(245, 158, 11, 0.6)'  // giallo: media
  if (c >= 0)   return 'rgba(0, 217, 126, 0.4)'   // verde: bassa
  if (c >= -0.4) return 'rgba(59, 130, 246, 0.4)' // blu: bassa inversa
  return 'rgba(139, 92, 246, 0.6)'                  // viola: alta inversa
}

function corrLabel(c: number): string {
  if (c >= 0.7) return 'Alta'
  if (c >= 0.4) return 'Media'
  if (c >= 0)   return 'Bassa'
  if (c >= -0.4) return 'Bassa inv.'
  return 'Alta inv.'
}

export function CorrelationHeatmap() {
  const { data, isLoading, error } = useCorrelation()

  if (isLoading) {
    return (
      <div style={{ background: 'var(--s2)', border: '1px solid var(--border)', borderRadius: 12, padding: '16px 20px' }}>
        <div style={{ fontFamily: 'Syne', fontWeight: 700, fontSize: 13, color: 'var(--text)', marginBottom: 12 }}>Correlazione portafoglio</div>
        <div style={{ color: 'var(--muted)', fontFamily: 'Syne', fontSize: 13 }}>Calcolo correlazione in corso…</div>
      </div>
    )
  }

  if (error || !data || data.labels.length < 2) {
    return (
      <div style={{ background: 'var(--s2)', border: '1px solid var(--border)', borderRadius: 12, padding: '16px 20px' }}>
        <div style={{ fontFamily: 'Syne', fontWeight: 700, fontSize: 13, color: 'var(--text)', marginBottom: 12 }}>Correlazione portafoglio</div>
        <div style={{ color: 'var(--muted)', fontFamily: 'Syne', fontSize: 13 }}>
          {data?.labels.length === 0
            ? 'Portafoglio vuoto.'
            : data?.labels.length === 1
            ? 'Servono almeno 2 ticker per calcolare la correlazione.'
            : 'Dati insufficienti per calcolare la correlazione.'}
        </div>
      </div>
    )
  }

  const { labels, matrix, interpretations } = data
  const n = labels.length
  const cellSize = Math.max(48, Math.min(72, 400 / n))

  return (
    <div style={{ background: 'var(--s2)', border: '1px solid var(--border)', borderRadius: 12, padding: '16px 20px' }}>
      <div style={{ fontFamily: 'Syne', fontWeight: 700, fontSize: 13, color: 'var(--text)', marginBottom: 16 }}>
        Correlazione portafoglio
      </div>

      {/* Matrice */}
      <div style={{ overflowX: 'auto' }}>
        <table aria-label="Matrice di correlazione tra i ticker del portafoglio, valori da -1 a 1 con codifica a colori" style={{ borderCollapse: 'collapse' }}>
          <thead>
            <tr>
              <th style={{ width: 56 }} />
              {labels.map(l => (
                <th key={l} style={{
                  fontFamily: 'JetBrains Mono', fontSize: 11, color: 'var(--acc)',
                  padding: '4px 2px', textAlign: 'center', width: cellSize,
                }}>
                  {l.replace('.MI', '').replace('.DE', '').replace('.AS', '').replace('.PA', '')}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {labels.map((rowLabel, i) => (
              <tr key={rowLabel}>
                <td style={{
                  fontFamily: 'JetBrains Mono', fontSize: 11, color: 'var(--acc)',
                  padding: '2px 8px 2px 0', textAlign: 'right', whiteSpace: 'nowrap',
                }}>
                  {rowLabel.replace('.MI', '').replace('.DE', '').replace('.AS', '').replace('.PA', '')}
                </td>
                {labels.map((_colLabel, j) => {
                  const val = matrix[i]?.[j] ?? 0
                  const isDiag = i === j
                  return (
                    <td key={j} style={{
                      width: cellSize, height: cellSize, textAlign: 'center',
                      background: isDiag ? 'var(--s3)' : corrColor(val),
                      border: '1px solid var(--border)',
                      borderRadius: 4,
                      padding: 2,
                    }}>
                      <div style={{ fontFamily: 'JetBrains Mono', fontSize: 11, color: isDiag ? 'var(--muted)' : 'var(--text)', fontWeight: isDiag ? 400 : 700 }}>
                        {isDiag ? '—' : formatNumber(val, 2)}
                      </div>
                    </td>
                  )
                })}
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Legenda */}
      <div style={{ display: 'flex', gap: 12, marginTop: 12, flexWrap: 'wrap' }}>
        {[
          { color: 'rgba(239, 68, 68, 0.7)', label: 'Alta (>0.7)' },
          { color: 'rgba(245, 158, 11, 0.6)', label: 'Media (0.4-0.7)' },
          { color: 'rgba(0, 217, 126, 0.4)', label: 'Bassa (<0.4)' },
          { color: 'rgba(59, 130, 246, 0.4)', label: 'Inv. bassa' },
          { color: 'rgba(139, 92, 246, 0.6)', label: 'Inv. alta' },
        ].map(l => (
          <div key={l.label} style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
            <div aria-hidden="true" style={{ width: 12, height: 12, background: l.color, borderRadius: 2 }} />
            <span style={{ fontFamily: 'Syne', fontSize: 10, color: 'var(--muted)' }}>{l.label}</span>
          </div>
        ))}
      </div>

      {/* Interpretazioni */}
      {interpretations.length > 0 && (
        <div style={{ marginTop: 12 }}>
          <div style={{ fontFamily: 'Syne', fontSize: 11, color: 'var(--muted)', marginBottom: 6 }}>Interpretazioni:</div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
            {interpretations.slice(0, 6).map((interp, i) => (
              <div key={i} style={{ fontFamily: 'Syne', fontSize: 11, color: 'var(--muted2)' }}>
                • {interp}
              </div>
            ))}
          </div>
        </div>
      )}

      <div style={{ marginTop: 8, fontFamily: 'Syne', fontSize: 10, color: 'var(--muted)', fontStyle: 'italic' }}>
        Alta correlazione = minore diversificazione. Basata su ritorni giornalieri 1 anno.
      </div>
    </div>
  )
}
