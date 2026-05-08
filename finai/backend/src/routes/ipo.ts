import { Router, Request, Response, NextFunction } from 'express'
import { z } from 'zod'
import { fetchUpcomingIPOs, fetchRecentIPOs } from '../services/yahooFinance'
import { ipoWatchlistDb } from '../services/database'
import { cache } from '../services/cache'

const router = Router()

const AddWatchlistSchema = z.object({
  id: z.string().min(1),
  ticker: z.string().max(20).optional(),
  companyName: z.string().min(1),
  expectedDate: z.string().optional(),
  exchange: z.string().optional(),
  sector: z.string().optional(),
  lockupDays: z.number().int().positive().default(180),
  ipoPrice: z.number().positive().optional(),
  notes: z.string().optional(),
})

router.get('/upcoming', async (_req: Request, res: Response, next: NextFunction) => {
  try {
    const key = 'ipo:upcoming'
    const cached = cache.getBatch(key)
    if (cached) return res.json(cached)
    const data = await fetchUpcomingIPOs()
    cache.setBatch(key, data)
    res.json(data)
  } catch (err) {
    next(err)
  }
})

router.get('/recent', async (_req: Request, res: Response, next: NextFunction) => {
  try {
    const key = 'ipo:recent'
    const cached = cache.getBatch(key)
    if (cached) return res.json(cached)
    const data = await fetchRecentIPOs()
    cache.setBatch(key, data)
    res.json(data)
  } catch (err) {
    next(err)
  }
})

router.get('/watchlist', (_req: Request, res: Response) => {
  res.json(ipoWatchlistDb.getAll())
})

router.post('/watchlist', (req: Request, res: Response) => {
  const parsed = AddWatchlistSchema.safeParse(req.body)
  if (!parsed.success) return res.status(400).json({ error: parsed.error.flatten() })
  const item = ipoWatchlistDb.add(parsed.data as Parameters<typeof ipoWatchlistDb.add>[0])
  res.status(201).json(item)
})

router.patch('/watchlist/:id', (req: Request, res: Response) => {
  ipoWatchlistDb.update(req.params.id, req.body)
  res.json({ ok: true })
})

router.delete('/watchlist/:id', (req: Request, res: Response) => {
  ipoWatchlistDb.remove(req.params.id)
  res.json({ ok: true })
})

export default router
