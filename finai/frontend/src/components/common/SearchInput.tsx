import { useRef, useState } from 'react'
import { useSearch } from '@/hooks/useSearch'

interface Props {
  value: string
  onSelect: (ticker: string, name: string) => void
  placeholder?: string
  style?: React.CSSProperties
}

const TYPE_LABELS: Record<string, string> = {
  EQUITY: 'Azione',
  ETF: 'ETF',
  MUTUALFUND: 'Fondo',
}

export function SearchInput({ value, onSelect, placeholder = 'Ticker o nome…', style }: Props) {
  const { query, setQuery, results, isLoading, clear } = useSearch()
  const [open, setOpen] = useState(false)
  const wrapRef = useRef<HTMLDivElement>(null)

  const displayValue = open ? query : value

  const handleFocus = () => {
    setOpen(true)
    if (value && !query) setQuery(value)
  }

  const handleBlur = (e: React.FocusEvent) => {
    if (!wrapRef.current?.contains(e.relatedTarget as Node)) {
      setOpen(false)
    }
  }

  const handleSelect = (ticker: string, name: string) => {
    onSelect(ticker, name)
    clear()
    setOpen(false)
  }

  return (
    <div ref={wrapRef} style={{ position: 'relative', ...style }} onBlur={handleBlur}>
      <div style={{ position: 'relative' }}>
        <input
          value={displayValue}
          onChange={e => { setQuery(e.target.value.toUpperCase()); setOpen(true) }}
          onFocus={handleFocus}
          placeholder={placeholder}
          style={{
            width: '100%',
            background: 'var(--s3)',
            border: '1px solid var(--border)',
            borderRadius: 8,
            padding: '10px 36px 10px 14px',
            color: 'var(--text)',
            fontFamily: 'JetBrains Mono',
            fontSize: 14,
            outline: 'none',
          }}
        />
        {isLoading && (
          <span style={{
            position: 'absolute', right: 12, top: '50%', transform: 'translateY(-50%)',
            fontSize: 13, color: 'var(--muted)',
          }}>⟳</span>
        )}
      </div>

      {open && results.length > 0 && (
        <div style={{
          position: 'absolute', top: '100%', left: 0, right: 0, zIndex: 200,
          background: 'var(--s2)', border: '1px solid var(--border2)',
          borderRadius: 8, boxShadow: '0 8px 24px rgba(0,0,0,0.4)',
          marginTop: 4, maxHeight: 280, overflowY: 'auto',
        }}>
          {results.map(r => (
            <button
              key={r.ticker}
              onMouseDown={() => handleSelect(r.ticker, r.name)}
              style={{
                display: 'flex', alignItems: 'center', gap: 10,
                width: '100%', padding: '9px 14px',
                background: 'none', border: 'none',
                borderBottom: '1px solid var(--border)',
                cursor: 'pointer', textAlign: 'left',
              }}
              onMouseEnter={e => (e.currentTarget.style.background = 'var(--s3)')}
              onMouseLeave={e => (e.currentTarget.style.background = 'none')}
            >
              <span style={{ fontFamily: 'JetBrains Mono', fontWeight: 700, fontSize: 13, color: 'var(--acc)', minWidth: 72 }}>
                {r.ticker}
              </span>
              <span style={{ fontFamily: 'Syne', fontSize: 12, color: 'var(--text)', flex: 1, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                {r.name}
              </span>
              <span style={{ fontFamily: 'Syne', fontSize: 10, color: 'var(--muted)', flexShrink: 0 }}>
                {r.exchange}
              </span>
              <span style={{
                fontFamily: 'Syne', fontSize: 10, color: 'var(--muted2)',
                background: 'var(--s3)', padding: '1px 6px', borderRadius: 4, flexShrink: 0,
              }}>
                {TYPE_LABELS[r.type] ?? r.type}
              </span>
            </button>
          ))}
        </div>
      )}
    </div>
  )
}
