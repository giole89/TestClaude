import { useEffect, useRef } from 'react'
import axios from 'axios'
import { useAlertStore, Alert } from '@/store/useAlertStore'
import { API_BASE } from '@/lib/constants'

function checkAlert(alert: Alert, price: number, dayChangePct: number): boolean {
  switch (alert.type) {
    case 'above': return price > alert.value
    case 'below': return price < alert.value
    case 'change_up': return dayChangePct >= alert.value
    case 'change_down': return dayChangePct <= -alert.value
    default: return false
  }
}

function showNotification(alert: Alert, price: number) {
  if ('Notification' in window && Notification.permission === 'granted') {
    new Notification(`FINAI Alert: ${alert.ticker}`, {
      body: `Prezzo attuale: ${price.toFixed(2)} — Condizione scattata!`,
    })
  }
}

export function useAlerts(active: boolean) {
  const { alerts, fireAlert } = useAlertStore()
  const intervalRef = useRef<ReturnType<typeof setInterval>>()

  useEffect(() => {
    if ('Notification' in window && Notification.permission === 'default') {
      Notification.requestPermission()
    }
  }, [])

  useEffect(() => {
    if (!active || alerts.length === 0) return

    const checkAll = async () => {
      const tickers = [...new Set(alerts.map(a => a.ticker))]
      try {
        const res = await axios.get(`${API_BASE}/api/batch`, {
          params: { tickers: tickers.join(',') },
        })
        const quotes: Array<{ ticker: string; price: number; dayChangePct: number }> = res.data

        for (const alert of alerts) {
          const quote = quotes.find(q => q.ticker === alert.ticker)
          if (!quote) continue
          if (checkAlert(alert, quote.price, quote.dayChangePct)) {
            fireAlert(alert.id, quote.price)
            showNotification(alert, quote.price)
          }
        }
      } catch {
        // silent fail on polling
      }
    }

    checkAll()
    intervalRef.current = setInterval(checkAll, 60_000)
    return () => clearInterval(intervalRef.current)
  }, [active, alerts, fireAlert])
}
