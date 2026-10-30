import { apiClient } from './apiClient'

export type Health = { status: string; components?: Record<string, { status: string }> }
export const systemService = {
  async health() { return (await apiClient.get<Health>('/actuator/health')).data },
}
