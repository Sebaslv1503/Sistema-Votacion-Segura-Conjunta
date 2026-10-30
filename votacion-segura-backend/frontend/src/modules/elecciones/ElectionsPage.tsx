import { useEffect, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { zodResolver } from '@hookform/resolvers/zod'
import { ArrowRight, CalendarClock, ClipboardList, Plus, ShieldCheck, Users } from 'lucide-react'
import { useForm } from 'react-hook-form'
import { Link } from 'react-router'
import { z } from 'zod'
import { Button, Card, EmptyState, ErrorNotice, LoadingBlock, Modal, PageTitle, StatusBadge } from '../../components/ui'
import { useAuth } from '../../providers/AuthProvider'
import { useToast } from '../../providers/ToastProvider'
import { readableApiError } from '../../services/apiClient'
import { candidaturaService } from '../../services/candidaturaService'
import { eleccionService, type CreateElection } from '../../services/eleccionService'
import { formatDate } from '../../shared/format'
import { electionStateLabels, type Election, type Role } from '../../shared/types'

const electionSchema = z.object({
  nombre: z.string().trim().min(3, 'Ingresa un nombre de al menos 3 caracteres.'),
  descripcion: z.string().max(1000, 'La descripción es demasiado larga.').optional(),
  inicio: z.string().min(1, 'Selecciona la fecha de inicio.'),
  fin: z.string().min(1, 'Selecciona la fecha de cierre.'),
}).refine((data) => new Date(data.fin).getTime() > new Date(data.inicio).getTime(), { message: 'El cierre debe ser posterior al inicio.', path: ['fin'] })
type ElectionForm = z.infer<typeof electionSchema>
type PendingAction = { election: Election; action: 'PROGRAMADA' | 'ABIERTA' | 'PAUSADA' | 'solicitar-cierre' | 'aprobar-cierre' }
const canManage = (role: Role | undefined) => role === 'ADMINISTRADOR_TECNICO' || role === 'CUSTODIO_ELECTORAL'

export function ElectionsPage() {
  const { session } = useAuth()
  const queryClient = useQueryClient()
  const toast = useToast()
  const [createOpen, setCreateOpen] = useState(false)
  const [known, setKnown] = useState<Election[]>([])
  const [pending, setPending] = useState<PendingAction | null>(null)
  const open = useQuery({ queryKey: ['elections', 'open'], queryFn: eleccionService.listOpen })
  useEffect(() => {
    if (open.data) setKnown((current) => mergeElections(current, open.data || []))
  }, [open.data])

  const create = useMutation({
    mutationFn: (input: CreateElection) => eleccionService.create(input),
    onSuccess: (election) => { setKnown((current) => mergeElections(current, [election])); setCreateOpen(false); toast('Elección creada en estado borrador.', 'success'); void queryClient.invalidateQueries({ queryKey: ['elections'] }) },
  })
  const transition = useMutation({
    mutationFn: (operation: PendingAction) => {
      if (operation.action === 'solicitar-cierre') return eleccionService.requestClose(operation.election.id)
      if (operation.action === 'aprobar-cierre') return eleccionService.approveClose(operation.election.id)
      return eleccionService.transition(operation.election.id, operation.action)
    },
    onSuccess: (updated, operation) => {
      setKnown((current) => mergeElections(current.filter((item) => item.id !== updated.id), [updated]))
      setPending(null)
      toast(operation.action === 'solicitar-cierre' ? 'Solicitud de cierre registrada.' : operation.action === 'aprobar-cierre' ? 'Cierre aprobado por este custodio.' : `Estado actualizado: ${electionStateLabels[updated.estado]}.`, 'success')
      void queryClient.invalidateQueries({ queryKey: ['elections'] })
      if (updated.estado === 'ABIERTA') void queryClient.invalidateQueries({ queryKey: ['candidates', updated.id] })
    },
  })
  const visible = known

  return <>
    <PageTitle eyebrow="GESTIÓN ELECTORAL" title="Elecciones" description="Consulta las jornadas abiertas y administra elecciones creadas durante esta sesión. El backend no dispone de un endpoint para listar elecciones borrador, programadas o cerradas."
      actions={canManage(session?.role) ? <Button onClick={() => setCreateOpen(true)}><Plus size={16} /> Crear elección</Button> : undefined} />
    {canManage(session?.role) && <div className="notice notice-warning" style={{ marginBottom: 16 }}><CalendarClock size={17} /><span>Las elecciones nuevas permanecen visibles en esta pantalla mientras dure la sesión. Tras recargar, el backend solo permite recuperar elecciones abiertas. Conserva el identificador para continuar procesos de cierre o escrutinio.</span></div>}
    {open.isError && <div style={{ marginBottom: 15 }}><ErrorNotice message={readableApiError(open.error)} /></div>}
    {transition.isError && <div style={{ marginBottom: 15 }}><ErrorNotice title="La operación no fue aceptada" message={readableApiError(transition.error)} /></div>}
    {open.isPending && !known.length ? <LoadingBlock label="Consultando elecciones abiertas" /> : visible.length ? <div className="election-card-grid">{visible.map((election) => <ElectionCard key={election.id} election={election} role={session?.role} busy={transition.isPending} onAction={(action) => setPending({ election, action })} />)}</div> : <Card><EmptyState title="No hay elecciones abiertas" detail="Cuando el backend exponga una jornada ABIERTA aparecerá en esta lista." action={canManage(session?.role) ? <Button style={{ marginTop: 10 }} onClick={() => setCreateOpen(true)}><Plus size={14} /> Crear una elección</Button> : null} /></Card>}

    {createOpen && <Modal title="Crear elección" onClose={() => { setCreateOpen(false); create.reset() }}>
      <ElectionForm busy={create.isPending} error={create.isError ? readableApiError(create.error) : ''} onCancel={() => setCreateOpen(false)} onSubmit={(values) => create.mutate({ nombre: values.nombre, descripcion: values.descripcion || undefined, inicio: new Date(values.inicio).toISOString(), fin: new Date(values.fin).toISOString() })} />
    </Modal>}
    {pending && <TransitionConfirm action={pending} busy={transition.isPending} onClose={() => { setPending(null); transition.reset() }} onConfirm={() => transition.mutate(pending)} />}
  </>
}

function ElectionCard({ election, role, busy, onAction }: { election: Election; role?: Role; busy: boolean; onAction: (action: PendingAction['action']) => void }) {
  const manage = canManage(role)
  const candidates = useQuery({ queryKey: ['candidates', election.id], queryFn: () => candidaturaService.list(election.id), enabled: election.estado === 'BORRADOR' || election.estado === 'PROGRAMADA', gcTime: 0 })
  return <Card className="election-card">
    <div className="election-card-top"><div><h2>{election.nombre}</h2><p>{election.descripcion || 'Elección institucional'}</p></div><StatusBadge state={election.estado} /></div>
    <div className="election-meta"><div><span>Inicio</span><strong>{formatDate(election.inicio)}</strong></div><div><span>Cierre previsto</span><strong>{formatDate(election.fin)}</strong></div></div>
    <div className="election-meta"><div><span>Identificador</span><strong className="mono-value">{election.id}</strong></div><div><span>Candidaturas activas</span><strong>{candidates.isSuccess ? candidates.data.length : election.estado === 'ABIERTA' ? '—' : candidates.isPending ? 'Consultando…' : 'No disponible'}</strong></div></div>
    {election.estado === 'ABIERTA' && role === 'SOCIO_VOTANTE' ? <Link className="button button-primary full-button" to={`/votacion/${election.id}`}>Consultar y participar <ArrowRight size={14} /></Link> : <div className="election-actions">
      {(election.estado === 'BORRADOR' || election.estado === 'PROGRAMADA') && manage && <Link className="button button-secondary" to={`/elecciones/${election.id}/candidaturas`}><Users size={14} /> Gestionar candidaturas</Link>}
      {election.estado === 'BORRADOR' && manage && <Button variant="primary" disabled={busy} onClick={() => onAction('PROGRAMADA')}>Programar</Button>}
      {election.estado === 'PROGRAMADA' && manage && <Button disabled={busy || (candidates.isSuccess && candidates.data.length === 0)} onClick={() => onAction('ABIERTA')}><ShieldCheck size={14} /> Revisar y abrir</Button>}
      {election.estado === 'ABIERTA' && manage && <><Button variant="secondary" disabled={busy} onClick={() => onAction('PAUSADA')}>Pausar</Button><Button variant="danger" disabled={busy} onClick={() => onAction('solicitar-cierre')}>Solicitar cierre</Button></>}
      {election.estado === 'PAUSADA' && manage && <><Button variant="secondary" disabled={busy} onClick={() => onAction('ABIERTA')}>Reanudar</Button><Button variant="danger" disabled={busy} onClick={() => onAction('solicitar-cierre')}>Solicitar cierre</Button></>}
      {election.estado === 'CIERRE_SOLICITADO' && role === 'CUSTODIO_ELECTORAL' && <Button disabled={busy} onClick={() => onAction('aprobar-cierre')}>Aprobar cierre</Button>}
      {election.estado === 'CERRADA' && role && ['CUSTODIO_ELECTORAL', 'ADMINISTRADOR_TECNICO'].includes(role) && <Link className="button button-secondary" to={`/resultados?eleccion=${encodeURIComponent(election.id)}`}>Escrutinio <ArrowRight size={13} /></Link>}
    </div>}
    {(election.estado === 'PROGRAMADA' || election.estado === 'BORRADOR') && manage && candidates.isSuccess && candidates.data.length === 0 && <small className="field-hint">Registra al menos una candidatura antes de abrir. El backend rechazará la apertura si no hay candidaturas activas.</small>}
  </Card>
}

function ElectionForm({ busy, error, onCancel, onSubmit }: { busy: boolean; error: string; onCancel: () => void; onSubmit: (data: ElectionForm) => void }) {
  const { register, handleSubmit, formState: { errors } } = useForm<ElectionForm>({ resolver: zodResolver(electionSchema) })
  return <form className="form-stack" noValidate onSubmit={handleSubmit(onSubmit)}>
    <div className="form-grid">
      <div className="field form-span-2"><label htmlFor="election-name">Nombre</label><input id="election-name" className="text-input" {...register('nombre')} />{errors.nombre && <small className="field-error">{errors.nombre.message}</small>}</div>
      <div className="field form-span-2"><label htmlFor="election-description">Descripción (opcional)</label><textarea id="election-description" className="textarea-input" {...register('descripcion')} /></div>
      <div className="field"><label htmlFor="election-start">Inicio</label><input id="election-start" className="text-input" type="datetime-local" {...register('inicio')} />{errors.inicio && <small className="field-error">{errors.inicio.message}</small>}</div>
      <div className="field"><label htmlFor="election-end">Fin</label><input id="election-end" className="text-input" type="datetime-local" {...register('fin')} />{errors.fin && <small className="field-error">{errors.fin.message}</small>}</div>
    </div>
    <div className="notice notice-info"><ClipboardList size={16} /><span>La elección se crea como BORRADOR. Crea y revisa las candidaturas antes de programar y abrirla.</span></div>
    {error && <ErrorNotice message={error} />}
    <div className="form-actions"><Button type="button" variant="ghost" onClick={onCancel}>Cancelar</Button><Button type="submit" disabled={busy}>{busy ? 'Creando…' : 'Crear borrador'}</Button></div>
  </form>
}

function TransitionConfirm({ action, busy, onClose, onConfirm }: { action: PendingAction; busy: boolean; onClose: () => void; onConfirm: () => void }) {
  const isOpen = action.action === 'ABIERTA'
  const isClose = action.action === 'aprobar-cierre' || action.action === 'solicitar-cierre'
  const candidates = useQuery({ queryKey: ['candidates', 'review', action.election.id], queryFn: () => candidaturaService.list(action.election.id), enabled: isOpen })
  return <Modal title={isOpen ? 'Revisión previa a apertura' : isClose ? 'Confirmar cierre controlado' : 'Confirmar cambio de estado'} onClose={onClose}>
    <div className="form-stack">
      <div className="preflight-summary"><div><span>Elección</span><strong>{action.election.nombre}</strong></div><div><span>Fechas</span><strong>{formatDate(action.election.inicio)} — {formatDate(action.election.fin)}</strong></div><div><span>Estado actual</span><strong>{electionStateLabels[action.election.estado]}</strong></div></div>
      {isOpen && <>
        {candidates.isPending ? <LoadingBlock label="Revisando candidaturas" /> : candidates.isError ? <ErrorNotice message={readableApiError(candidates.error)} /> : <div className={candidates.data.length ? 'notice notice-success' : 'notice notice-danger'}><Users size={16} /><span>{candidates.data.length} candidaturas activas visibles. {candidates.data.length ? 'Verifica fechas y contenido antes de abrir.' : 'No hay candidaturas activas; el backend rechazará la apertura.'}</span></div>}
        <div className="notice notice-warning"><ShieldCheck size={16} /><span>El servicio de padrón no ofrece al custodio un conteo de socios habilitados. La verificación final de elegibilidad ocurre al votar.</span></div>
      </>}
      {action.action === 'solicitar-cierre' && <div className="notice notice-warning"><ShieldCheck size={16} /><span>Esta solicitud requiere que un segundo custodio distinto apruebe el cierre.</span></div>}
      {action.action === 'aprobar-cierre' && <div className="notice notice-warning"><ShieldCheck size={16} /><span>El backend verificará que tu identidad sea distinta a la del custodio solicitante. Si eres la misma persona, la solicitud será rechazada.</span></div>}
      {isClose && <p className="muted-copy">La votación dejará de aceptar votos cuando el backend confirme el cierre.</p>}
      <div className="form-actions"><Button variant="ghost" onClick={onClose}>Cancelar</Button><Button variant={isClose ? 'danger' : 'primary'} disabled={busy || (isOpen && (!candidates.isSuccess || candidates.data.length === 0))} onClick={onConfirm}>{busy ? 'Procesando…' : action.action === 'solicitar-cierre' ? 'Solicitar cierre' : action.action === 'aprobar-cierre' ? 'Aprobar cierre' : action.action === 'PROGRAMADA' ? 'Programar elección' : action.action === 'ABIERTA' ? 'Abrir elección' : action.action === 'PAUSADA' ? 'Pausar elección' : 'Confirmar'}</Button></div>
    </div>
  </Modal>
}

function mergeElections(current: Election[], updates: Election[]) {
  const map = new Map(current.map((item) => [item.id, item]))
  updates.forEach((item) => map.set(item.id, item))
  return [...map.values()].sort((a, b) => a.inicio.localeCompare(b.inicio))
}
