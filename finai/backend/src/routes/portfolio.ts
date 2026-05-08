import { Router, Request, Response } from 'express'
import { z } from 'zod'
import { portfolioDb } from '../services/database'
import { fetchBatch } from '../services/yahooFinance'
import { cache } from '../services/cache'

const router = Router()

const AddItemSchema = z.object({
  id: z.string().min(1),
  ticker: z.string().min(1).max(20).transform(s => s.toUpperCase()),
  name: z.string().min(1),
  qty: z.number().positive(),
  loadPrice: z.number().positive(),
  currentPrice: z.number().positive().optional(),
  currency: z.string().default('USD'),
})

router.get('/', (_req: Request, res: Response) => {
  res.json(portfolioDb.getAll())
})

router.post('/', (req: Request, res: Response) => {
  const parsed = AddItemSchema.safeParse(req.body)
  if (!parsed.success) return res.status(400).json({ error: parsed.error.flatten() })
  const item = portfolioDb.add(parsed.data as Parameters<typeof portfolioDb.add>[0])
  res.status(201).json(item)
})

router.delete('/:id', (req: Request, res: Response) => {
  portfolioDb.remove(req.params.id)
  res.json({ ok: true })
})

router.post('/refresh', async (_req: Request, res: Response) => {
  const items = portfolioDb.getAll()
  if (items.length === 0) return res.json([])

  const tickers = [...new Set(items.map(i => i.ticker))]
  const cacheKey = `portfolio_refresh:${tickers.sort().join(',')}`

  let quotes = cache.getBatch(cacheKey) as Array<{ ticker: string; price: number; name: string; currency: string }> | undefined
  if (!quotes) {
    quotes = await fetchBatch(tickers)
    cache.setBatch(cacheKey, quotes)
  }

  for (const q of quotes) {
    portfolioDb.updatePrice(q.ticker, q.price, q.currency)
    portfolioDb.updateName(q.ticker, q.name)
  }

  res.json(portfolioDb.getAll())
})

export default router
