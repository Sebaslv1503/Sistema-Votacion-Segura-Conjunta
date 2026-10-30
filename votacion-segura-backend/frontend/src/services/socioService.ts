import { apiClient } from './apiClient'
import type { Member, Page } from '../shared/types'

export type CreateMember = { numeroSocio: string; nombres: string; apellidos: string; correoElectronico?: string }
export type UpdateMember = { nombres: string; apellidos: string; correoElectronico?: string }

export const socioService = {
  async list(params: { page: number; size: number; q?: string }) {
    const { data } = await apiClient.get<Page<Member>>('/api/v1/socios', { params: { ...params, sort: 'apellidos,asc' } })
    return data
  },
  async create(payload: CreateMember) { return (await apiClient.post<Member>('/api/v1/socios', payload)).data },
  async update(id: string, payload: UpdateMember) { return (await apiClient.put<Member>(`/api/v1/socios/${id}`, payload)).data },
  async setStatus(id: string, estado: Member['estadoHabilitacion'], activo: boolean) {
    return (await apiClient.patch<Member>(`/api/v1/socios/${id}/habilitacion`, { estado, activo })).data
  },
  async importCsv(file: File) {
    const data = new FormData(); data.append('archivo', file)
    return (await apiClient.post<{ sociosImportados: number }>('/api/v1/socios/importar', data)).data
  },
}
