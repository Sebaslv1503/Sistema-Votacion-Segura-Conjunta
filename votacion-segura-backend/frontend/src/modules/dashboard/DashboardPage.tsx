import { useQuery } from '@tanstack/react-query'
import { Activity, ArrowRight, CalendarClock, ClipboardCheck, ShieldCheck, Users, Vote } from 'lucide-react'
import { Link } from 'react-router'
import { Card, EmptyState, ErrorNotice, LoadingBlock, PageTitle, StatusBadge } from '../../components/ui'
import { auditoriaService } from '../../services/auditoriaService'
import { eleccionService } from '../../services/eleccionService'
import { readableApiError } from '../../services/apiClient'
import { socioService } from '../../services/socioService'
import { systemService } from '../../services/systemService'
import { useAuth } from '../../providers/AuthProvider'
import { formatDate, formatNumber } from '../../shared/format'
import { roleLabels } from '../../shared/types'

export function DashboardPage() {
  const { session } = useAuth()
  const role = session?.role
  const elections = useQuery({ queryKey: ['elections', 'open'], queryFn: eleccionService.listOpen, enabled: Boolean(role) })
  const members = useQuery({ queryKey: ['members', 'count'], queryFn: () => socioService.list({ page: 0, size: 1 }), enabled: role === 'GESTOR_PADRON' || role === 'ADMINISTRADOR_TECNICO' })
  const health = useQuery({ queryKey: ['system', 'health'], queryFn: systemService.health, enabled: role === 'ADMINISTRADOR_TECNICO' })
  const audit = useQuery({ queryKey: ['audit', 'recent'], queryFn: () => auditoriaService.list({ page: 0, size: 5 }), enabled: role === 'AUDITOR_INDEPENDIENTE' || role === 'ADMINISTRADOR_TECNICO' })

  return <>
    <PageTitle eyebrow="VISTA GENERAL" title={`Buenos días, ${session?.username}`} description={`Espacio de trabajo · ${role ? roleLabels[role] : ''}`} />
    <div className="notice notice-info role-callout" style={{ marginBottom: 17 }}><ShieldCheck size={18} /><span>El acceso y las acciones disponibles se determinan por tu rol. La autorización final siempre se verifica en el servidor.</span></div>

    {elections.isPending ? <LoadingBlock label="Consultando elecciones abiertas" /> : elections.isError ? <ErrorNotice message={readableApiError(elections.error)} /> : <div className="dashboard-grid">
      <Metric label="Elecciones abiertas" value={formatNumber(elections.data.length)} caption="Disponibles según el servicio electoral" icon={<Vote size={18} />} />
      {(role === 'GESTOR_PADRON' || role === 'ADMINISTRADOR_TECNICO') && <Metric label="Registros en el padrón" value={members.isSuccess ? formatNumber(members.data.totalElements) : members.isPending ? '…' : '—'} caption="Conteo entregado por el padrón autorizado" icon={<Users size={18} />} />}
      {role === 'ADMINISTRADOR_TECNICO' && <Metric label="Estado operativo" value={health.data?.status === 'UP' ? 'Operativo' : health.isPending ? '…' : 'Revisar'} caption="Actuator · /actuator/health" icon={<Activity size={18} />} tone={health.data?.status === 'UP' ? 'green' : 'amber'} />}
      {role === 'AUDITOR_INDEPENDIENTE' && <Metric label="Eventos recientes" value={audit.isSuccess ? formatNumber(audit.data.totalElements) : audit.isPending ? '…' : '—'} caption="Página consultada desde el registro de auditoría" icon={<ClipboardCheck size={18} />} />}
      {role === 'CUSTODIO_ELECTORAL' && <Metric label="En jornada" value={formatNumber(elections.data.length)} caption="El endpoint expone elecciones ABIERTAS" icon={<CalendarClock size={18} />} />}
      {role === 'SOCIO_VOTANTE' && <Metric label="Jornada electoral" value={elections.data.length ? 'Activa' : 'Sin jornada'} caption="Consulta las elecciones habilitadas para ti" icon={<Vote size={18} />} tone={elections.data.length ? 'green' : 'neutral'} />}

      {role === 'ADMINISTRADOR_TECNICO' && !health.isPending && health.isError && <div className="span-12"><ErrorNotice title="Estado de servicio no disponible" message={readableApiError(health.error, 'El endpoint operativo no respondió.')} /></div>}
      {(role === 'GESTOR_PADRON' || role === 'ADMINISTRADOR_TECNICO') && members.isError && <div className="span-12"><ErrorNotice title="Padrón no disponible" message={readableApiError(members.error)} /></div>}

      <Card className="span-8">
        <div className="card-heading"><div><h2>Elecciones en jornada</h2><p>El servicio disponible devuelve únicamente elecciones ABIERTAS.</p></div><Link to="/elecciones" className="table-action">Ver elecciones <ArrowRight size={13} /></Link></div>
        {elections.data.length === 0 ? <EmptyState title="No hay elecciones abiertas" detail="Cuando una elección esté en periodo de votación aparecerá en esta lista." /> : <div className="data-table-wrap"><table><thead><tr><th>Elección</th><th>Periodo</th><th>Estado</th><th /></tr></thead><tbody>{elections.data.slice(0, 5).map((election) => <tr key={election.id}><td><strong>{election.nombre}</strong><span className="table-secondary">{election.descripcion || 'Sin descripción'}</span></td><td>{formatDate(election.inicio)}<span className="table-secondary">Hasta {formatDate(election.fin)}</span></td><td><StatusBadge state={election.estado} /></td><td><Link className="table-action" to={role === 'SOCIO_VOTANTE' ? `/votacion/${election.id}` : '/elecciones'}>{role === 'SOCIO_VOTANTE' ? 'Participar' : 'Abrir'} <ArrowRight size={12} /></Link></td></tr>)}</tbody></table></div>}
      </Card>

      <Card className="span-4"><div className="card-heading"><div><h2>Herramientas de tu rol</h2><p>Accesos a operaciones disponibles</p></div></div><div className="quick-links">
        {(role === 'GESTOR_PADRON' || role === 'ADMINISTRADOR_TECNICO') && <QuickLink to="/socios" title="Gestionar padrón" detail="Buscar, actualizar e importar socios" icon={<Users size={17} />} />}
        {(role === 'CUSTODIO_ELECTORAL' || role === 'ADMINISTRADOR_TECNICO') && <QuickLink to="/elecciones" title="Control de elecciones" detail="Configurar, abrir y solicitar cierres" icon={<Vote size={17} />} />}
        {(role === 'AUDITOR_INDEPENDIENTE' || role === 'ADMINISTRADOR_TECNICO') && <QuickLink to="/auditoria" title="Registro de auditoría" detail="Consultar eventos de solo lectura" icon={<ClipboardCheck size={17} />} />}
        {(role === 'SOCIO_VOTANTE' || role === 'CUSTODIO_ELECTORAL' || role === 'AUDITOR_INDEPENDIENTE' || role === 'ADMINISTRADOR_TECNICO') && <QuickLink to="/resultados" title="Resultados oficiales" detail="Solo actas publicadas por el backend" icon={<ShieldCheck size={17} />} />}
        {role === 'ADMINISTRADOR_TECNICO' && <QuickLink to="/administracion" title="Estado del sistema" detail="Consultar disponibilidad básica" icon={<Activity size={17} />} />}
        {role === 'SOPORTE' && <p className="muted-copy">El backend todavía no ofrece rutas de gestión de casos de soporte.</p>}
      </div></Card>

      {(role === 'AUDITOR_INDEPENDIENTE' || role === 'ADMINISTRADOR_TECNICO') && <Card className="span-12"><div className="card-heading"><div><h2>Actividad reciente</h2><p>Eventos disponibles para tu rol</p></div><Link className="table-action" to="/auditoria">Abrir auditoría <ArrowRight size={13} /></Link></div>{audit.isPending ? <LoadingBlock label="Consultando auditoría" /> : audit.isError ? <ErrorNotice message={readableApiError(audit.error)} /> : audit.data.content.length ? <div className="data-table-wrap"><table><thead><tr><th>Operación</th><th>Actor</th><th>Fecha</th><th>Resultado</th></tr></thead><tbody>{audit.data.content.map((event) => <tr key={event.id}><td><strong>{event.accion}</strong><span className="table-secondary">{event.tipo.replaceAll('_', ' ')}</span></td><td>{event.rolActor?.replaceAll('_', ' ') || 'Sistema'}</td><td>{formatDate(event.fecha)}</td><td><StatusBadge state={event.resultado} tone={event.resultado === 'SUCCESS' ? 'success' : event.resultado === 'FAILURE' ? 'danger' : 'warning'} /></td></tr>)}</tbody></table></div> : <EmptyState title="Sin eventos recientes" detail="El servicio no devolvió eventos para mostrar." />}</Card>}
    </div>}
  </>
}

function Metric({ label, value, caption, icon, tone = 'blue' }: { label: string; value: string; caption: string; icon: React.ReactNode; tone?: 'blue' | 'green' | 'amber' | 'neutral' }) {
  return <Card className={`span-4 metric-card metric-${tone}`}><div><div className="metric-label">{label}</div><div className="metric-value">{value}</div><div className="metric-caption">{caption}</div></div><div className="metric-icon">{icon}</div></Card>
}
function QuickLink({ to, title, detail, icon }: { to: string; title: string; detail: string; icon: React.ReactNode }) {
  return <Link className="quick-link" to={to}><span className="quick-link-icon">{icon}</span><span><strong>{title}</strong><small>{detail}</small></span><ArrowRight size={14} /></Link>
}
