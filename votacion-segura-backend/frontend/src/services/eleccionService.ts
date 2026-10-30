import { apiClient } from './apiClient'
import type { Election, ElectionState } from '../shared/types'

export type CreateElection = { nombre: string; descripcion?: string; inicio: string; fin: string }
export const eleccionService = {
  async listOpen() { return (await apiClient.get<Election[]>('/api/v1/elecciones')).data },
  async create(payload: CreateElection) { return (await apiClient.post<Election>('/api/v1/elecciones', payload)).data },
  async transition(id: string, estado: ElectionState) { return (await apiClient.patch<Election>(`/api/v1/elecciones/${id}/estado`, { estado })).data },
  async requestClose(id: string) { return (await apiClient.post<Election>(`/api/v1/elecciones/${id}/solicitar-cierre`)).data },
  async approveClose(id: string) { return (await apiClient.post<Election>(`/api/v1/elecciones/${id}/aprobar-cierre`)).data },
}
