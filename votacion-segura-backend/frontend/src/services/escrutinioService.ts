import { apiClient } from './apiClient'
import type { OfficialResult } from '../shared/types'

export const escrutinioService = {
  async generate(electionId: string) { return (await apiClient.post<OfficialResult>(`/api/v1/resultados/elecciones/${electionId}/escrutinio`)).data },
  async approve(electionId: string) { return (await apiClient.post<OfficialResult>(`/api/v1/resultados/elecciones/${electionId}/aprobacion`)).data },
  async publish(electionId: string) { return (await apiClient.post<OfficialResult>(`/api/v1/resultados/elecciones/${electionId}/publicar`)).data },
  async getPublished(electionId: string) { return (await apiClient.get<OfficialResult>(`/api/v1/resultados/elecciones/${electionId}`)).data },
}
