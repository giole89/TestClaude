import { Router, Request, Response, NextFunction } from 'express'
import { fetchHistory } from '../services/yahooFinance'
import { cache } from '../services/cache'

const router = Router()

router.get('/:ticker', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const { ticker } = req.params
    const range = (req.query.range as string) || '1y'
    const key = `history:${ticker.toUpperCase()}:${range}`

    const cached = cache.getHistory(key)
    if (cached) return res.json(cached)

    const data = await fetchHistory(ticker, range)
    cache.setHistory(key, data)
    res.json(data)
  } catch (err) {
    next(err)
  }
})

export default router
