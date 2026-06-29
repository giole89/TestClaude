import { useState, useRef, useEffect } from 'react'
import { useChat } from '@/hooks/useChat'
import { ChatMessageItem } from './ChatMessage'

interface Props {
  tabKey: string
  context: Record<string, unknown>
  quickActions?: string[]
  placeholder?: string
}

export function ChatPanel({ tabKey, context, quickActions = [], placeholder = 'Scrivi un messaggio…' }: Props) {
  const { messages, sendMessage } = useChat(tabKey)
  const [input, setInput] = useState('')
  const [sending, setSending] = useState(false)
  const bottomRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages])

  const handleSend = async () => {
    const text = input.trim()
    if (!text || sending) return
    setInput('')
    setSending(true)
    await sendMessage(text, context)
    setSending(false)
  }

  const handleKey = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      handleSend()
    }
  }

  return (
    <div style={{
      display: 'flex', flexDirection: 'column',
      height: '100%', background: 'var(--s2)',
      border: '1px solid var(--border)', borderRadius: 12,
      overflow: 'hidden',
    }}>
      <div style={{
        padding: '12px 16px', borderBottom: '1px solid var(--border)',
        display: 'flex', alignItems: 'center', gap: 8,
      }}>
        <span style={{ fontSize: 16 }}>🤖</span>
        <span style={{ fontFamily: 'Syne', fontWeight: 700, fontSize: 14, color: 'var(--acc2)' }}>
          FINAI AI
        </span>
        <span style={{ fontSize: 11, color: 'var(--muted)', marginLeft: 4 }}>risponde in italiano</span>
      </div>

      <div style={{ flex: 1, overflowY: 'auto', padding: 16 }}>
        {messages.length === 0 && (
          <div style={{ textAlign: 'center', color: 'var(--muted)', fontSize: 13, marginTop: 24 }}>
            <p>Ciao! Sono FINAI, il tuo advisor finanziario.</p>
            <p style={{ marginTop: 8 }}>Chiedimi qualsiasi cosa sul mercato.</p>
          </div>
        )}
        {messages.map(msg => (
          <ChatMessageItem key={msg.id} msg={msg} />
        ))}
        <div ref={bottomRef} />
      </div>

      {quickActions.length > 0 && messages.length === 0 && (
        <div style={{ padding: '0 16px 8px', display: 'flex', flexWrap: 'wrap', gap: 6 }}>
          {quickActions.map(action => (
            <button
              key={action}
              onClick={() => { setInput(action) }}
              style={{
                padding: '4px 10px', borderRadius: 20,
                border: '1px solid var(--border2)',
                background: 'var(--s3)', color: 'var(--muted2)',
                fontSize: 11, cursor: 'pointer', fontFamily: 'Syne',
              }}
            >
              {action}
            </button>
          ))}
        </div>
      )}

      <div style={{
        padding: '12px 16px', borderTop: '1px solid var(--border)',
        display: 'flex', gap: 8,
      }}>
        <textarea
          value={input}
          onChange={e => setInput(e.target.value)}
          onKeyDown={handleKey}
          placeholder={placeholder}
          rows={2}
          style={{
            flex: 1, background: 'var(--s3)',
            border: '1px solid var(--border)', borderRadius: 8,
            padding: '8px 12px', color: 'var(--text)',
            fontFamily: 'Syne', fontSize: 13, resize: 'none',
            outline: 'none',
          }}
        />
        <button
          onClick={handleSend}
          disabled={sending || !input.trim()}
          aria-label="Invia messaggio"
          style={{
            padding: '8px 16px', borderRadius: 8,
            background: sending || !input.trim() ? 'var(--s3)' : 'var(--acc)',
            border: 'none', cursor: sending || !input.trim() ? 'default' : 'pointer',
            color: sending || !input.trim() ? 'var(--muted)' : '#07080a',
            fontFamily: 'Syne', fontWeight: 700, fontSize: 13,
            transition: 'all 0.15s',
          }}
        >
          {sending ? '…' : '↑'}
        </button>
      </div>
    </div>
  )
}
