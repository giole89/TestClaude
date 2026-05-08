import { Router, Request, Response, NextFunction } from 'express'
import { fetchQuote, fetchHistory } from '../services/yahooFinance'
import { cache } from '../services/cache'
import { calcRSI, calcSMA, calcVolatility, calcMomentum, calcBullScore } from '../services/indicators'

const router = Router()

router.get('/:ticker', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const { ticker } = req.params
    const key = `quote:${ticker.toUpperCase()}`

    const cached = cache.getQuote(key)
    if (cached) return res.json(cached)

    const data = await fetchQuote(ticker)
    cache.setQuote(key, data)
    res.json(data)
  } catch (err) {
    next(err)
  }
})

router.get('/:ticker/full', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const { ticker } = req.params
    const key = `full:${ticker.toUpperCase()}`

    const cached = cache.getQuote(key)
    if (cached) return res.json(cached)

    const [quote, history] = await Promise.all([
      fetchQuote(ticker),
      fetchHistory(ticker, '1y'),
    ])

    const closes = history.map(h => h.close)
    const rsi = calcRSI(closes)
    const sma20 = calcSMA(closes, 20)
    const sma50 = calcSMA(closes, 50)
    const sma200 = calcSMA(closes, 200)
    const volatility = calcVolatility(closes)
    const momentum30 = calcMomentum(closes, 30)
    const bullScore = calcBullScore({
      price: quote.price,
      sma20,
      sma50,
      sma200,
      rsi,
      momentum30,
      volatility,
      rangePosition: quote.rangePosition,
    })

    const full = { ...quote, rsi, sma20, sma50, sma200, volatility, momentum30, bullScore, history }
    cache.setQuote(key, full)
    res.json(full)
  } catch (err) {
    next(err)
  }
})

export default router
