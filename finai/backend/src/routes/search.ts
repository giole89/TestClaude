import { Router, Request, Response, NextFunction } from 'express'
import { searchTickers } from '../services/yahooFinance'
import { cache } from '../services/cache'

const router = Router()

router.get('/', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const q = (req.query.q as string ?? '').trim()
    if (q.length < 2) return res.json([])

    const key = `search:${q.toLowerCase()}`
    const cached = cache.getQuote(key)
    if (cached) return res.json(cached)

    const results = await searchTickers(q)
    cache.setQuote(key, results)
    res.json(results)
  } catch (err) {
    next(err)
  }
})

export default router
