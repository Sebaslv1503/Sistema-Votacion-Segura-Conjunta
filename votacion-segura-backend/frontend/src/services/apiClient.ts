import axios, { type InternalAxiosRequestConfig } from 'axios'
import type { TokenResponse } from '../shared/types'
import { clearMemoryTokens, getMemoryTokens, setMemoryTokens } from './sessionMemory'

const baseURL = (import.meta.env.VITE_API_URL || '').replace(/\/$/, '')

export const apiClient = axios.create({ baseURL, timeout: 20_000, headers: { Accept: 'application/json' } })
const refreshClient = axios.create({ baseURL, timeout: 20_000, headers: { Accept: 'application/json' } })

type RetryConfig = InternalAxiosRequestConfig & { _retriedAfterRefresh?: boolean }
let refreshInFlight: Promise<TokenResponse | null> | null = null

apiClient.interceptors.request.use((config) => {
  const accessToken = getMemoryTokens()?.accessToken
  if (accessToken) config.headers.Authorization = `Bearer ${accessToken}`
  return config
})

apiClient.interceptors.response.use((response) => response, async (error: unknown) => {
  if (!axios.isAxiosError(error) || error.response?.status !== 401 || !error.config) throw error
  const config = error.config as RetryConfig
  const isAuthRoute = config.url?.startsWith('/api/v1/auth/')
  const current = getMemoryTokens()
  if (isAuthRoute || config._retriedAfterRefresh || !current?.refreshToken) {
    if (!isAuthRoute) expireSession()
    throw error
  }

  config._retriedAfterRefresh = true
  if (!refreshInFlight) {
    refreshInFlight = refreshClient.post<TokenResponse>('/api/v1/auth/refresh', { refreshToken: current.refreshToken })
      .then(({ data }) => {
        setMemoryTokens({ ...data, username: current.username, role: current.role })
        return data
      })
      .catch(() => { expireSession(); return null })
      .finally(() => { refreshInFlight = null })
  }
  const refreshed = await refreshInFlight
  if (!refreshed) throw error
  config.headers.Authorization = `${refreshed.tokenType} ${refreshed.accessToken}`
  return apiClient(config)
})

const expireSession = () => {
  clearMemoryTokens()
  if (typeof window !== 'undefined') window.dispatchEvent(new Event('votacion:session-expired'))
}

export const readableApiError = (error: unknown, fallback = 'No se pudo completar la operación.') => {
  if (axios.isAxiosError<{ message?: string }>(error)) {
    const status = error.response?.status
    if (status === 403) return 'Tu cuenta no tiene permiso para realizar esta acción.'
    if (status === 409) return error.response?.data?.message || 'El estado actual no permite esta operación.'
    if (status === 401) return 'La sesión terminó. Inicia sesión nuevamente.'
    if (status && status >= 500) return 'El servicio no pudo completar la operación. Inténtalo más tarde.'
    return error.response?.data?.message || fallback
  }
  return fallback
}
