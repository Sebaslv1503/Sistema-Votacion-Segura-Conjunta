import { apiClient } from './apiClient'

export const votacionService = {
  async issueCredential(electionId: string, replacePending = false) {
    const query = replacePending ? '?reemplazarPendiente=true' : ''
    return (await apiClient.post<{ credencial: string }>(`/api/v1/votacion/elecciones/${electionId}/credencial${query}`)).data.credencial
  },
  async cast(credencial: string, candidaturaId: string) {
    return (await apiClient.post<{ confirmacion: string }>('/api/v1/votacion/votos', { credencial, candidaturaId })).data
  },
}
