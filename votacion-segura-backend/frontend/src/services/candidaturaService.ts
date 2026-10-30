import { apiClient } from './apiClient'
import type { Candidate } from '../shared/types'

export type CandidateInput = { nombre: string; descripcion?: string; propuesta?: string }
export const candidaturaService = {
  async list(electionId: string) { return (await apiClient.get<Candidate[]>(`/api/v1/candidaturas/elecciones/${electionId}`)).data },
  async create(electionId: string, payload: CandidateInput) {
    return (await apiClient.post<Candidate>('/api/v1/candidaturas', { eleccionId: electionId, ...payload })).data
  },
  async update(id: string, payload: CandidateInput & { activo: boolean }) { return (await apiClient.put<Candidate>(`/api/v1/candidaturas/${id}`, payload)).data },
}
