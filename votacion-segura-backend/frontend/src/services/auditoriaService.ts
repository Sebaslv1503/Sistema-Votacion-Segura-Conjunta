import { apiClient } from './apiClient'
import type { AuditEvent, Page } from '../shared/types'

export const auditoriaService = {
  async list(params: { page: number; size: number; actorId?: string }) {
    return (await apiClient.get<Page<AuditEvent>>('/api/v1/auditoria', { params })).data
  },
}
