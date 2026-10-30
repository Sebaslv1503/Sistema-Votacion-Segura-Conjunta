import { apiClient } from './apiClient'
import type { Participation } from '../shared/types'

export const participacionService = {
  async myState(electionId: string) { return (await apiClient.get<Participation>(`/api/v1/participaciones/elecciones/${electionId}/mi-estado`)).data },
}
