export type Role =
  | 'SOCIO_VOTANTE'
  | 'GESTOR_PADRON'
  | 'ADMINISTRADOR_TECNICO'
  | 'CUSTODIO_ELECTORAL'
  | 'AUDITOR_INDEPENDIENTE'
  | 'SOPORTE'

export const roleLabels: Record<Role, string> = {
  SOCIO_VOTANTE: 'Socio votante',
  GESTOR_PADRON: 'Gestor del padrón',
  ADMINISTRADOR_TECNICO: 'Administrador técnico',
  CUSTODIO_ELECTORAL: 'Custodio electoral',
  AUDITOR_INDEPENDIENTE: 'Auditor independiente',
  SOPORTE: 'Soporte',
}

export type ElectionState =
  | 'BORRADOR'
  | 'PROGRAMADA'
  | 'ABIERTA'
  | 'PAUSADA'
  | 'CIERRE_SOLICITADO'
  | 'CERRADA'
  | 'FINALIZADA'

export const electionStateLabels: Record<ElectionState, string> = {
  BORRADOR: 'Borrador',
  PROGRAMADA: 'Programada',
  ABIERTA: 'Abierta',
  PAUSADA: 'Pausada',
  CIERRE_SOLICITADO: 'Cierre solicitado',
  CERRADA: 'Cerrada',
  FINALIZADA: 'Finalizada',
}

export type UserSession = { username: string; role: Role }
export type TokenResponse = { accessToken: string; refreshToken: string; tokenType: 'Bearer'; expiresInMs: number }
export type Election = { id: string; nombre: string; descripcion: string | null; inicio: string; fin: string; estado: ElectionState }
export type Candidate = { id: string; eleccionId: string; nombre: string; descripcion: string | null; propuesta: string | null; activo: boolean }
export type MemberState = 'HABILITADO' | 'INHABILITADO' | 'PENDIENTE_VERIFICACION' | 'BAJA'
export type Member = { id: string; numeroSocio: string; nombres: string; apellidos: string; correoElectronico: string | null; estadoHabilitacion: MemberState; activo: boolean }
export type Participation = { electionId: string; eligible: boolean; participationState: 'SIN_CREDENCIAL' | 'PENDIENTE' | 'VOTADO' }
export type AuditEvent = { id: string; actorId: string | null; rolActor: string | null; tipo: string; accion: string; resultado: string; fecha: string; correlationId: string | null; detalles: string | null; hash: string | null }
export type OfficialResult = { eleccionId: string; totalVotos: number; fechaCierre: string; conteosJson: string; hash: string; estado: string }
export type Page<T> = { content: T[]; totalElements: number; totalPages: number; size: number; number: number; first: boolean; last: boolean }
export type ApiErrorBody = { timestamp?: string; status?: number; error?: string; message?: string; path?: string }

export const isRole = (value: unknown): value is Role =>
  typeof value === 'string' && value in roleLabels
