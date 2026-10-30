import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { authService } from '../services/authService'
import { clearMemoryTokens, getMemoryTokens, roleFromAccessToken, setMemoryTokens } from '../services/sessionMemory'
import type { UserSession } from '../shared/types'

type AuthContextValue = {
  session: UserSession | null
  loading: boolean
  login: (username: string, password: string) => Promise<void>
  logout: () => Promise<void>
}
const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<UserSession | null>(() => {
    const current = getMemoryTokens()
    return current ? { username: current.username, role: current.role } : null
  })
  const [loading, setLoading] = useState(false)

  const clear = useCallback(() => {
    clearMemoryTokens()
    setSession(null)
  }, [])

  useEffect(() => {
    window.addEventListener('votacion:session-expired', clear)
    return () => window.removeEventListener('votacion:session-expired', clear)
  }, [clear])

  const login = useCallback(async (username: string, password: string) => {
    setLoading(true)
    try {
      const response = await authService.login(username.trim(), password)
      const role = roleFromAccessToken(response.accessToken)
      if (!role) throw new Error('El backend no devolvió un rol válido en el access token.')
      setMemoryTokens({ ...response, username: username.trim(), role })
      setSession({ username: username.trim(), role })
    } finally {
      setLoading(false)
    }
  }, [])

  const logout = useCallback(async () => {
    const tokens = getMemoryTokens()
    try {
      if (tokens?.refreshToken) await authService.logout(tokens.refreshToken)
    } finally {
      clear()
    }
  }, [clear])

  const value = useMemo(() => ({ session, loading, login, logout }), [session, loading, login, logout])
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export const useAuth = () => {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth debe utilizarse dentro de AuthProvider')
  return context
}
