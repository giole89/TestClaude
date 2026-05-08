import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import axios from 'axios'
import { API_BASE } from '@/lib/constants'
import { AlertType } from '@/store/useAlertStore'

export interface BackendAlert {
  id: string
  ticker: string
  type: AlertType
  value: number
  createdAt: number
  fired: boolean
  firedAt?: number
  firedPrice?: number
}

interface AlertsResponse {
  active: BackendAlert[]
  history: BackendAlert[]
}

const KEY = ['alerts']

export function useAlertsBackend() {
  const qc = useQueryClient()

  const query = useQuery<AlertsResponse>({
    queryKey: KEY,
    queryFn: () => axios.get(`${API_BASE}/api/alerts`).then(r => r.data),
    staleTime: 30_000,
    refetchInterval: 60_000,
    retry: 2,
  })

  const add = useMutation({
    mutationFn: (alert: { id: string; ticker: string; type: AlertType; value: number }) =>
      axios.post(`${API_BASE}/api/alerts`, alert).then(r => r.data),
    onSuccess: () => qc.invalidateQueries({ queryKey: KEY }),
  })

  const remove = useMutation({
    mutationFn: (id: string) => axios.delete(`${API_BASE}/api/alerts/${id}`),
    onSuccess: () => qc.invalidateQueries({ queryKey: KEY }),
  })

  const fire = useMutation({
    mutationFn: ({ id, price }: { id: string; price: number }) =>
      axios.post(`${API_BASE}/api/alerts/${id}/fire`, { price }),
    onSuccess: () => qc.invalidateQueries({ queryKey: KEY }),
  })

  return {
    alerts: query.data?.active ?? [],
    history: query.data?.history ?? [],
    isLoading: query.isLoading,
    add: add.mutateAsync,
    remove: remove.mutateAsync,
    fire: fire.mutateAsync,
  }
}
