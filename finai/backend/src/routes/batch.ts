import { Router, Request, Response, NextFunction } from 'express'
import { fetchBatch } from '../services/yahooFinance'
import { cache } from '../services/cache'

const router = Router()

router.get('/', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const tickersParam = req.query.tickers as string
    if (!tickersParam) return res.status(400).json({ error: 'Parametro tickers mancante' })

    const tickers = tickersParam.split(',').map(t => t.trim()).filter(Boolean)
    const key = `batch:${tickers.sort().join(',')}`

    const cached = cache.getBatch(key)
    if (cached) return res.json(cached)

    const CHUNK_SIZE = 10
    const chunks: string[][] = []
    for (let i = 0; i < tickers.length; i += CHUNK_SIZE) {
      chunks.push(tickers.slice(i, i + CHUNK_SIZE))
    }

    const results = await Promise.allSettled(chunks.map(chunk => fetchBatch(chunk)))
    const data = results
      .filter((r): r is PromiseFulfilledResult<Awaited<ReturnType<typeof fetchBatch>>> => r.status === 'fulfilled')
      .flatMap(r => r.value)

    cache.setBatch(key, data)
    res.json(data)
  } catch (err) {
    next(err)
  }
})

export default router
