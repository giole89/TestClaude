import { useCallback } from 'react'
import { useChatStore } from '@/store/useChatStore'
import { API_BASE } from '@/lib/constants'

export function useChat(tabKey: string) {
  const { getMessages, addMessage, appendToMessage, finalizeMessage, resetHistory } = useChatStore()
  const messages = getMessages(tabKey)

  const sendMessage = useCallback(
    async (userText: string, context: Record<string, unknown>) => {
      addMessage(tabKey, { role: 'user', content: userText })

      const assistantId = addMessage(tabKey, { role: 'assistant', content: '', streaming: true })

      const history = getMessages(tabKey)
        .filter(m => !m.streaming)
        .map(m => ({ role: m.role, content: m.content }))

      try {
        const res = await fetch(`${API_BASE}/api/ai/chat`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            messages: [...history, { role: 'user', content: userText }],
            context,
          }),
        })

        if (!res.body) throw new Error('No response body')

        const reader = res.body.getReader()
        const decoder = new TextDecoder()
        let buffer = ''

        while (true) {
          const { done, value } = await reader.read()
          if (done) break

          buffer += decoder.decode(value, { stream: true })
          const lines = buffer.split('\n')
          buffer = lines.pop() ?? ''

          for (const line of lines) {
            if (!line.startsWith('data: ')) continue
            const data = line.slice(6).trim()
            if (data === '[DONE]') break
            try {
              const parsed = JSON.parse(data)
              if (parsed.text) appendToMessage(tabKey, assistantId, parsed.text)
            } catch {
              // skip malformed chunks
            }
          }
        }
      } catch (err) {
        appendToMessage(tabKey, assistantId, `\n\n⚠️ Errore: ${(err as Error).message}`)
      } finally {
        finalizeMessage(tabKey, assistantId)
      }
    },
    [tabKey, addMessage, appendToMessage, finalizeMessage, getMessages]
  )

  return { messages, sendMessage, resetHistory: () => resetHistory(tabKey) }
}
