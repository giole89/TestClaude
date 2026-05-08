import { ChatMessage as Msg } from '@/store/useChatStore'

function renderMarkdown(text: string): string {
  return text
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/\*(.+?)\*/g, '<em>$1</em>')
    .replace(/^#{1,3}\s+(.+)$/gm, '<strong style="font-size:1.05em">$1</strong>')
    .replace(/^- (.+)$/gm, '<li>$1</li>')
    .replace(/(<li>.*<\/li>)/gs, '<ul style="padding-left:20px;margin:6px 0">$1</ul>')
    .replace(/\n\n/g, '<br/><br/>')
    .replace(/\n/g, '<br/>')
}

interface Props {
  msg: Msg
}

export function ChatMessageItem({ msg }: Props) {
  const isUser = msg.role === 'user'

  return (
    <div style={{
      display: 'flex',
      justifyContent: isUser ? 'flex-end' : 'flex-start',
      marginBottom: 12,
    }}>
      <div style={{
        maxWidth: '85%',
        padding: '10px 14px',
        borderRadius: isUser ? '16px 16px 4px 16px' : '16px 16px 16px 4px',
        background: isUser ? 'var(--acc)' : 'var(--s3)',
        color: isUser ? '#07080a' : 'var(--text)',
        fontSize: 13,
        lineHeight: 1.6,
        fontFamily: 'Syne',
        position: 'relative',
      }}>
        {isUser ? (
          <span>{msg.content}</span>
        ) : (
          <span dangerouslySetInnerHTML={{ __html: renderMarkdown(msg.content) }} />
        )}
        {msg.streaming && (
          <span style={{
            display: 'inline-block',
            width: 8, height: 14,
            background: 'var(--acc2)',
            marginLeft: 3,
            animation: 'blink 1s infinite',
            verticalAlign: 'middle',
          }} />
        )}
      </div>
    </div>
  )
}
