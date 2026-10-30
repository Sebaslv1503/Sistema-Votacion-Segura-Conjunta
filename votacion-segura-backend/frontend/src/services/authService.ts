import { apiClient } from './apiClient'
import type { TokenResponse } from '../shared/types'

export const authService = {
  async login(username: string, password: string) {
    const { data } = await apiClient.post<TokenResponse>('/api/v1/auth/login', { username, password })
    return data
  },
  async logout(refreshToken: string) {
    await apiClient.post('/api/v1/auth/logout', { refreshToken })
  },
}
