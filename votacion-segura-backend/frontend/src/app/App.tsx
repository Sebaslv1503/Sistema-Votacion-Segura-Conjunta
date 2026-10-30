import { Navigate, Route, Routes } from 'react-router'
import { ProtectedRoute } from './ProtectedRoute'
import { AppShell } from '../layouts/AppShell'
import { LoginPage } from '../modules/auth/LoginPage'
import { DashboardPage } from '../modules/dashboard/DashboardPage'
import { MembersPage } from '../modules/socios/MembersPage'
import { ElectionsPage } from '../modules/elecciones/ElectionsPage'
import { CandidatesPage } from '../modules/candidaturas/CandidatesPage'
import { VotingPage } from '../modules/votacion/VotingPage'
import { AuditPage } from '../modules/auditoria/AuditPage'
import { ResultsPage } from '../modules/escrutinio/ResultsPage'
import { AdminPage } from '../modules/administracion/AdminPage'
import { SupportPage } from '../modules/soporte/SupportPage'
import { AccessDeniedPage, NotFoundPage } from '../modules/shared/UtilityPages'
import type { Role } from '../shared/types'

const roster: Role[] = ['GESTOR_PADRON', 'ADMINISTRADOR_TECNICO']
const candidatesManagers: Role[] = ['CUSTODIO_ELECTORAL', 'ADMINISTRADOR_TECNICO']
const auditors: Role[] = ['AUDITOR_INDEPENDIENTE', 'ADMINISTRADOR_TECNICO']
const resultsReaders: Role[] = ['SOCIO_VOTANTE', 'CUSTODIO_ELECTORAL', 'AUDITOR_INDEPENDIENTE', 'ADMINISTRADOR_TECNICO']

export function App() {
  return <Routes>
    <Route path="/login" element={<LoginPage />} />
    <Route element={<ProtectedRoute />}>
      <Route element={<AppShell />}>
        <Route index element={<DashboardPage />} />
        <Route path="/elecciones" element={<ElectionsPage />} />
        <Route path="/votacion/:electionId" element={<ProtectedRoute roles={['SOCIO_VOTANTE']} />}><Route index element={<VotingPage />} /></Route>
        <Route path="/socios" element={<ProtectedRoute roles={roster} />}><Route index element={<MembersPage />} /></Route>
        <Route path="/elecciones/:electionId/candidaturas" element={<ProtectedRoute roles={candidatesManagers} />}><Route index element={<CandidatesPage />} /></Route>
        <Route path="/auditoria" element={<ProtectedRoute roles={auditors} />}><Route index element={<AuditPage />} /></Route>
        <Route path="/resultados" element={<ProtectedRoute roles={resultsReaders} />}><Route index element={<ResultsPage />} /></Route>
        <Route path="/administracion" element={<ProtectedRoute roles={['ADMINISTRADOR_TECNICO']} />}><Route index element={<AdminPage />} /></Route>
        <Route path="/soporte" element={<ProtectedRoute roles={['SOPORTE']} />}><Route index element={<SupportPage />} /></Route>
        <Route path="/acceso-denegado" element={<AccessDeniedPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Route>
    <Route path="*" element={<Navigate to="/" replace />} />
  </Routes>
}
