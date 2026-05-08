const itLocale = 'it-IT'

export function formatPrice(value: number, currency = 'USD'): string {
  return new Intl.NumberFormat(itLocale, {
    style: 'currency',
    currency,
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(value)
}

export function formatNumber(value: number, decimals = 2): string {
  return new Intl.NumberFormat(itLocale, {
    minimumFractionDigits: decimals,
    maximumFractionDigits: decimals,
  }).format(value)
}

export function formatPct(value: number, decimals = 2): string {
  const sign = value >= 0 ? '+' : ''
  return `${sign}${formatNumber(value, decimals)}%`
}

export function formatLargeNumber(value: number): string {
  if (value >= 1e12) return `${formatNumber(value / 1e12, 2)}T`
  if (value >= 1e9) return `${formatNumber(value / 1e9, 2)}B`
  if (value >= 1e6) return `${formatNumber(value / 1e6, 2)}M`
  return formatNumber(value, 0)
}

export function formatDate(dateStr: string): string {
  return new Intl.DateTimeFormat(itLocale, { day: '2-digit', month: 'short', year: 'numeric' }).format(new Date(dateStr))
}

export function formatDateShort(dateStr: string): string {
  return new Intl.DateTimeFormat(itLocale, { day: '2-digit', month: 'short' }).format(new Date(dateStr))
}

export function formatDateTime(ts: number): string {
  return new Intl.DateTimeFormat(itLocale, {
    day: '2-digit', month: '2-digit', year: 'numeric',
    hour: '2-digit', minute: '2-digit',
  }).format(new Date(ts))
}

export function colorForChange(value: number): string {
  if (value > 0) return 'var(--acc)'
  if (value < 0) return 'var(--red)'
  return 'var(--muted2)'
}

export function rsiLabel(rsi: number): string {
  if (rsi >= 70) return 'Ipercomprato'
  if (rsi <= 30) return 'Ipervenduto'
  if (rsi >= 55) return 'Rialzista'
  if (rsi <= 45) return 'Ribassista'
  return 'Neutro'
}

export function rsiColor(rsi: number): string {
  if (rsi >= 70) return 'var(--acc3)'
  if (rsi <= 30) return 'var(--red)'
  if (rsi >= 55) return 'var(--acc)'
  return 'var(--muted2)'
}
