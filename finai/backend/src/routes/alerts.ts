import { Router, Request, Response } from 'express'
import { z } from 'zod'
import { alertsDb } from '../services/database'

const router = Router()

const AddAlertSchema = z.object({
  id: z.string().min(1),
  ticker: z.string().min(1).max(20).transform(s => s.toUpperCase()),
  type: z.enum(['above', 'below', 'change_up', 'change_down']),
  value: z.number().positive(),
})

const FireAlertSchema = z.object({
  price: z.number().positive(),
})

router.get('/', (_req: Request, res: Response) => {
  res.json({
    active: alertsDb.getActive(),
    history: alertsDb.getHistory(),
  })
})

router.post('/', (req: Request, res: Response) => {
  const parsed = AddAlertSchema.safeParse(req.body)
  if (!parsed.success) return res.status(400).json({ error: parsed.error.flatten() })
  const alert = alertsDb.add(parsed.data as Parameters<typeof alertsDb.add>[0])
  res.status(201).json(alert)
})

router.delete('/:id', (req: Request, res: Response) => {
  alertsDb.remove(req.params.id)
  res.json({ ok: true })
})

router.post('/:id/fire', (req: Request, res: Response) => {
  const parsed = FireAlertSchema.safeParse(req.body)
  if (!parsed.success) return res.status(400).json({ error: parsed.error.flatten() })
  alertsDb.fire(req.params.id, parsed.data.price)
  res.json({ ok: true })
})

export default router
