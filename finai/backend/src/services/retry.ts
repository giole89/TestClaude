export async function withRetry<T>(
  fn: () => Promise<T>,
  maxRetries = 3,
  baseDelayMs = 2000,
): Promise<T> {
  let lastErr: unknown
  for (let attempt = 0; attempt < maxRetries; attempt++) {
    try {
      return await fn()
    } catch (err) {
      lastErr = err
      if (attempt < maxRetries - 1) {
        await new Promise(r => setTimeout(r, baseDelayMs * Math.pow(2, attempt)))
      }
    }
  }
  throw lastErr
}

class CircuitBreaker {
  private failures = 0
  private lastFailureAt = 0
  private readonly threshold: number
  private readonly resetAfterMs: number

  constructor(threshold = 5, resetAfterMs = 60_000) {
    this.threshold = threshold
    this.resetAfterMs = resetAfterMs
  }

  isOpen(): boolean {
    if (this.failures < this.threshold) return false
    if (Date.now() - this.lastFailureAt > this.resetAfterMs) {
      this.failures = 0
      return false
    }
    return true
  }

  recordFailure(): void {
    this.failures++
    this.lastFailureAt = Date.now()
  }

  recordSuccess(): void {
    this.failures = 0
  }
}

export const yahooCircuitBreaker = new CircuitBreaker(5, 60_000)

export async function withCircuitBreaker<T>(
  fn: () => Promise<T>,
  fallback?: () => T | undefined,
): Promise<T | undefined> {
  if (yahooCircuitBreaker.isOpen()) {
    console.warn('[CircuitBreaker] Yahoo Finance circuit open — serving from cache/fallback')
    return fallback ? fallback() : undefined
  }
  try {
    const result = await fn()
    yahooCircuitBreaker.recordSuccess()
    return result
  } catch (err) {
    yahooCircuitBreaker.recordFailure()
    throw err
  }
}
