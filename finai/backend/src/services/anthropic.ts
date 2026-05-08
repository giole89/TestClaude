import Anthropic from '@anthropic-ai/sdk'
import { Response } from 'express'

const client = new Anthropic({ apiKey: process.env.ANTHROPIC_API_KEY })

const MODEL = process.env.AI_MODEL || 'claude-haiku-4-5-20251001'

export interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
}

export interface AIContext {
  tab: string
  ticker?: string
  tickerData?: Record<string, unknown>
  compareData?: Record<string, unknown>
  portfolio?: unknown[]
  profile?: string
  marketData?: Record<string, unknown>
}

function buildSystemPrompt(context: AIContext): string {
  const base = `Sei FINAI, un advisor finanziario personale per investitori italiani.
Rispondi SEMPRE in italiano. Sii preciso, utile e diretto.
Non usare tabelle markdown raw nel testo. Usa elenchi puntati quando appropriato.
Non chiedere informazioni che già hai nel contesto.
Fornisci sempre una valutazione chiara e un consiglio concreto.`

  const contextData = JSON.stringify(context, null, 2)

  const tabPrompts: Record<string, string> = {
    analyze: `Sei specializzato nell'analisi di singoli titoli azionari ed ETF.
Hai accesso ai seguenti dati del titolo analizzato:\n${contextData}
Analizza RSI, medie mobili, momentum e volatilità per dare un giudizio tecnico completo.`,

    compare: `Sei specializzato nel confronto tra due strumenti finanziari.
Hai accesso ai seguenti dati comparativi:\n${contextData}
Confronta obiettivamente i due strumenti e indica quale è più adatto e perché.`,

    longterm: `Sei specializzato in investimenti a lungo termine e strategia DCA.
Hai accesso ai seguenti dati:\n${contextData}
Valuta l'adeguatezza per investimento a lungo termine, considera DCA e obiettivi temporali.`,

    portfolio: `Sei specializzato nell'analisi di portafogli personali.
Hai accesso al seguente portafoglio:\n${contextData}
Analizza diversificazione, performance, rischi concentrazione e suggerisci ottimizzazioni.`,

    suggested: `Sei specializzato in portafogli modello e asset allocation.
Hai accesso al seguente profilo e template:\n${contextData}
Suggerisci allocazioni personalizzate basandoti sul profilo di rischio e sui dati di mercato attuali.`,

    market: `Sei specializzato nell'analisi del mercato in tempo reale.
Hai accesso ai seguenti dati di mercato:\n${contextData}
Fornisci una lettura del sentiment e identifica le opportunità più interessanti.`,
  }

  return `${base}\n\n${tabPrompts[context.tab] ?? tabPrompts['market']}`
}

export async function streamChatResponse(
  messages: ChatMessage[],
  context: AIContext,
  res: Response
): Promise<void> {
  const systemPrompt = buildSystemPrompt(context)

  res.setHeader('Content-Type', 'text/event-stream')
  res.setHeader('Cache-Control', 'no-cache')
  res.setHeader('Connection', 'keep-alive')
  res.setHeader('Access-Control-Allow-Origin', '*')

  const stream = await client.messages.stream({
    model: MODEL,
    max_tokens: 4096,
    system: systemPrompt,
    messages: messages.map(m => ({ role: m.role, content: m.content })),
  })

  for await (const chunk of stream) {
    if (chunk.type === 'content_block_delta' && chunk.delta.type === 'text_delta') {
      res.write(`data: ${JSON.stringify({ text: chunk.delta.text })}\n\n`)
    }
  }

  res.write('data: [DONE]\n\n')
  res.end()
}
