import { useRef, useState } from 'react'
import { useFinance } from '@/hooks/useFinance'

function errorMessage(e: any): string {
  return e?.response?.data?.error || e?.message || 'Import non riuscito'
}

export function StatementUpload() {
  const { uploadStatement, isUploading } = useFinance()
  const [error, setError] = useState<string | null>(null)
  const [result, setResult] = useState<{ imported: number; skipped: number; duplicates: number } | null>(null)
  const [dragOver, setDragOver] = useState(false)
  const inputRef = useRef<HTMLInputElement>(null)

  const handleFile = async (file: File | undefined | null) => {
    if (!file) return
    setError(null)
    setResult(null)
    try {
      const res = await uploadStatement(file)
      setResult({ imported: res.imported, skipped: res.skipped, duplicates: res.duplicates })
    } catch (e: any) {
      setError(errorMessage(e))
    }
  }

  return (
    <div style={{ background: 'var(--s2)', border: '1px solid var(--border)', borderRadius: 12, padding: '16px 20px' }}>
      <div style={{ fontFamily: 'Syne', fontWeight: 700, fontSize: 14, color: 'var(--text)', marginBottom: 4 }}>
        Importa estratto conto
      </div>
      <div style={{ fontFamily: 'Syne', fontSize: 12, color: 'var(--muted)', marginBottom: 12 }}>
        Carica un file PDF o Excel (.xlsx/.xls): i movimenti vengono riconosciuti e categorizzati automaticamente come entrate o spese variabili.
      </div>

      <div
        onClick={() => inputRef.current?.click()}
        onDragOver={e => { e.preventDefault(); setDragOver(true) }}
        onDragLeave={() => setDragOver(false)}
        onDrop={e => { e.preventDefault(); setDragOver(false); handleFile(e.dataTransfer.files?.[0]) }}
        style={{
          border: `2px dashed ${dragOver ? 'var(--acc)' : 'var(--border)'}`,
          borderRadius: 10, padding: '24px 16px', textAlign: 'center', cursor: 'pointer',
          background: dragOver ? 'rgba(110,231,183,0.05)' : 'transparent', transition: 'all 0.15s ease',
        }}
      >
        <input
          ref={inputRef}
          type="file"
          accept=".pdf,.xlsx,.xls"
          style={{ display: 'none' }}
          onChange={e => handleFile(e.target.files?.[0])}
        />
        <div style={{ fontFamily: 'Syne', fontSize: 13, color: 'var(--muted2)' }}>
          {isUploading ? '⟳ Lettura ed elaborazione in corso…' : '📄 Trascina qui il file o clicca per selezionarlo'}
        </div>
      </div>

      {error && (
        <div style={{
          marginTop: 12, background: 'rgba(239,68,68,0.1)', border: '1px solid var(--red)', borderRadius: 8,
          padding: '8px 14px', color: 'var(--red)', fontFamily: 'Syne', fontSize: 12,
        }}>
          {error}
        </div>
      )}

      {result && (
        <div style={{
          marginTop: 12, background: 'rgba(110,231,183,0.08)', border: '1px solid var(--acc)', borderRadius: 8,
          padding: '8px 14px', color: 'var(--acc)', fontFamily: 'Syne', fontSize: 12,
        }}>
          ✓ Importati {result.imported} movimenti{result.skipped > 0 ? ` (${result.skipped} righe non riconosciute scartate)` : ''}.
        </div>
      )}

      {result && result.duplicates > 0 && (
        <div style={{
          marginTop: 8, background: 'rgba(234,179,8,0.1)', border: '1px solid #eab308', borderRadius: 8,
          padding: '8px 14px', color: '#eab308', fontFamily: 'Syne', fontSize: 12,
        }}>
          ⚠ {result.duplicates} movimenti risultavano già presenti (stessa data, descrizione e importo) e non sono stati importati di nuovo: probabilmente questo estratto conto, o una sua parte, era già stato caricato.
        </div>
      )}
    </div>
  )
}
