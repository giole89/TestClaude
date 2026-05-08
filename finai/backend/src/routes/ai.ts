import { Router, Request, Response, NextFunction } from 'express'
import { streamChatResponse, ChatMessage, AIContext } from '../services/anthropic'
import { aiLimiter } from '../middleware/rateLimit'

const router = Router()

router.post('/chat', aiLimiter, async (req: Request, res: Response, next: NextFunction) => {
  try {
    const { messages, context } = req.body as { messages: ChatMessage[]; context: AIContext }

    if (!messages || !Array.isArray(messages) || messages.length === 0) {
      return res.status(400).json({ error: 'messages è richiesto' })
    }

    await streamChatResponse(messages, context || { tab: 'market' }, res)
  } catch (err) {
    if (!res.headersSent) next(err)
    else {
      res.write(`data: ${JSON.stringify({ error: (err as Error).message })}\n\n`)
      res.end()
    }
  }
})

export default router
