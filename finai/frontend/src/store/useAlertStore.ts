import { create } from 'zustand'
import { persist } from 'zustand/middleware'

export type AlertType = 'above' | 'below' | 'change_up' | 'change_down'

export interface Alert {
  id: string
  ticker: string
  type: AlertType
  value: number
  createdAt: number
  fired?: boolean
  firedAt?: number
  firedPrice?: number
}

interface AlertState {
  alerts: Alert[]
  history: Alert[]
  addAlert: (alert: Omit<Alert, 'id' | 'createdAt'>) => void
  removeAlert: (id: string) => void
  fireAlert: (id: string, price: number) => void
}

export const useAlertStore = create<AlertState>()(
  persist(
    (set) => ({
      alerts: [],
      history: [],
      addAlert: (alert) =>
        set((s) => ({
          alerts: [
            ...s.alerts,
            { ...alert, id: `alert-${Date.now()}`, createdAt: Date.now() },
          ],
        })),
      removeAlert: (id) =>
        set((s) => ({ alerts: s.alerts.filter((a) => a.id !== id) })),
      fireAlert: (id, price) =>
        set((s) => {
          const alert = s.alerts.find((a) => a.id === id)
          if (!alert) return {}
          const fired = { ...alert, fired: true, firedAt: Date.now(), firedPrice: price }
          return {
            alerts: s.alerts.filter((a) => a.id !== id),
            history: [fired, ...s.history].slice(0, 10),
          }
        }),
    }),
    { name: 'finai-alerts' }
  )
)
