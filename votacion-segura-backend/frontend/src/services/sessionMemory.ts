import type { Role, TokenResponse } from '../shared/types'

export type MemoryTokens = TokenResponse & { username: string; role: Role }
let tokens: MemoryTokens | null = null

export const getMemoryTokens = () => tokens
export const setMemoryTokens = (value: MemoryTokens) => { tokens = value }
export const clearMemoryTokens = () => { tokens = null }

export const roleFromAccessToken = (accessToken: string): Role | null => {
  try {
    const payload = accessToken.split('.')[1]
    if (!payload) return null
    const normalized = payload.replace(/-/g, '+').replace(/_/g, '/')
    const claims = JSON.parse(atob(normalized.padEnd(Math.ceil(normalized.length / 4) * 4, '='))) as { role?: unknown }
    const candidate = claims.role
    return typeof candidate === 'string' && [
      'SOCIO_VOTANTE', 'GESTOR_PADRON', 'ADMINISTRADOR_TECNICO', 'CUSTODIO_ELECTORAL', 'AUDITOR_INDEPENDIENTE', 'SOPORTE',
    ].includes(candidate) ? candidate as Role : null
  } catch {
    return null
  }
}
