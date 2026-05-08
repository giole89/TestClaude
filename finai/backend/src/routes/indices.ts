import { Router, Request, Response, NextFunction } from 'express'
import { fetchIndices } from '../services/yahooFinance'
import { cache } from '../services/cache'

const router = Router()

const INDEX_NAMES: Record<string, string> = {
  '^GSPC': 'S&P 500',
  '^NDX': 'Nasdaq 100',
  '^DJI': 'Dow Jones',
  '^STOXX50E': 'Euro Stoxx 50',
  'FTSEMIB.MI': 'FTSE MIB',
  '^VIX': 'VIX',
  'EURUSD=X': 'EUR/USD',
  'GC=F': 'Oro',
  'CL=F': 'WTI Oil',
}

router.get('/', async (_req: Request, res: Response, next: NextFunction) => {
  try {
    const key = 'indices:all'
    const cached = cache.getBatch(key)
    if (cached) return res.json(cached)

    const data = await fetchIndices()
    const enriched = data.map(d => ({ ...d, displayName: INDEX_NAMES[d.ticker] ?? d.name }))
    cache.setBatch(key, enriched)
    res.json(enriched)
  } catch (err) {
    next(err)
  }
})

export default router
