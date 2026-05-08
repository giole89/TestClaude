import { useState, useEffect, useRef } from 'react'
import axios from 'axios'
import { API_BASE } from '@/lib/constants'

export interface SearchResult {
  ticker: string
  name: string
  exchange: string
  type: string
}

export function useSearch() {
  const [query, setQuery] = useState('')
  const [results, setResults] = useState<SearchResult[]>([])
  const [isLoading, setIsLoading] = useState(false)
  const timerRef = useRef<ReturnType<typeof setTimeout>>()
  const abortRef = useRef<AbortController>()

  useEffect(() => {
    if (query.length < 2) {
      setResults([])
      return
    }
    clearTimeout(timerRef.current)
    timerRef.current = setTimeout(async () => {
      abortRef.current?.abort()
      abortRef.current = new AbortController()
      setIsLoading(true)
      try {
        const { data } = await axios.get(`${API_BASE}/api/search`, {
          params: { q: query },
          signal: abortRef.current.signal,
        })
        setResults(data)
      } catch {
        // cancelled or network error — silently ignore
      } finally {
        setIsLoading(false)
      }
    }, 300)

    return () => clearTimeout(timerRef.current)
  }, [query])

  return { query, setQuery, results, isLoading, clear: () => { setQuery(''); setResults([]) } }
}
