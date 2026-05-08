import 'dotenv/config'
import express from 'express'
import cors from 'cors'
import helmet from 'helmet'

import { apiLimiter } from './middleware/rateLimit'
import { errorHandler } from './middleware/errorHandler'
import quoteRouter from './routes/quote'
import batchRouter from './routes/batch'
import historyRouter from './routes/history'
import indicesRouter from './routes/indices'
import aiRouter from './routes/ai'
import portfolioRouter from './routes/portfolio'
import alertsRouter from './routes/alerts'
import searchRouter from './routes/search'
import ipoRouter from './routes/ipo'

// Initialise DB on startup (creates tables if not exist)
import './services/database'

const app = express()
const PORT = parseInt(process.env.PORT || '3001')

app.use(helmet())
app.use(cors({ origin: '*' }))
app.use(express.json({ limit: '1mb' }))
app.use(apiLimiter)

app.use('/api/quote', quoteRouter)
app.use('/api/batch', batchRouter)
app.use('/api/history', historyRouter)
app.use('/api/indices', indicesRouter)
app.use('/api/ai', aiRouter)
app.use('/api/portfolio', portfolioRouter)
app.use('/api/alerts', alertsRouter)
app.use('/api/search', searchRouter)
app.use('/api/ipo', ipoRouter)

app.get('/health', (_req, res) => res.json({ status: 'ok', timestamp: Date.now() }))

app.use(errorHandler)

app.listen(PORT, () => {
  console.log(`FINAI backend avviato su http://localhost:${PORT}`)
})

export default app
