import NodeCache from 'node-cache'

const quotesCache = new NodeCache({ stdTTL: parseInt(process.env.CACHE_TTL_QUOTES || '60') })
const historyCache = new NodeCache({ stdTTL: parseInt(process.env.CACHE_TTL_HISTORY || '14400') })
const aiCache = new NodeCache({ stdTTL: 1800 })
const batchCache = new NodeCache({ stdTTL: 120 })

export const cache = {
  getQuote: (key: string) => quotesCache.get<unknown>(key),
  setQuote: (key: string, value: unknown) => quotesCache.set(key, value),

  getHistory: (key: string) => historyCache.get<unknown>(key),
  setHistory: (key: string, value: unknown) => historyCache.set(key, value),

  getAI: (key: string) => aiCache.get<string>(key),
  setAI: (key: string, value: string) => aiCache.set(key, value),

  getBatch: (key: string) => batchCache.get<unknown>(key),
  setBatch: (key: string, value: unknown) => batchCache.set(key, value),
}
